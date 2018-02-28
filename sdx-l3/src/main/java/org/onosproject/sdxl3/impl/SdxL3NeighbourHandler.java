/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.sdxl3.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.host.HostService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.sdxl3.SdxL3;
import org.onosproject.sdxl3.SdxL3PeerService;
import org.onosproject.sdxl3.config.SdxParticipantsConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages neighbour message handlers for the use cases of (a) internal BGP
 * speakers connected to the network at some point that are exchanging neighbour
 * resolution messages with external routers that are connected behind interfaces
 * and (b) external border routers that exchange neighbour resolution messages
 * between each-other.
 * <p>
 * For each internal speaker port we use a handler that proxies packets from
 * that port to the appropriate external-facing interface port.
 * For each port that can potentially interface an external router, we use a
 * handler that forwards messages to the appropriate edge ports for peer-to-peer
 * messages. For peer-to-speaker messages, it responds based on the interface
 * configuration and proxies replies back the the internal BGP speaker.
 * </p>
 */
@Component(immediate = true, enabled = false)
public class SdxL3NeighbourHandler {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NeighbourResolutionService neighbourService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SdxL3PeerService sdxL3PeerService;

    private ApplicationId sdxL3AppId;
    private ApplicationId routerAppId;

    private Set<ConnectPoint> speakerConnectPoints = new HashSet<>();
    private Set<ConnectPoint> peerConnectPoints = new HashSet<>();

    private InternalNetworkConfigListener configListener = new InternalNetworkConfigListener();

    private InternalSpeakerNeighbourHandler internalHandler = new InternalSpeakerNeighbourHandler();
    private ExternalPeerNeighbourHandler externalHandler = new ExternalPeerNeighbourHandler();

    @Activate
    protected void activate() {
        sdxL3AppId = coreService.registerApplication(SdxL3.SDX_L3_APP);
        routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        configService.addListener(configListener);

        configurePeerHandlers();
        configureSpeakerHandlers();
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        neighbourService.unregisterNeighbourHandlers(sdxL3AppId);
    }

    private void configurePeerHandlers() {
        SdxParticipantsConfig peersConfig = configService.getConfig(sdxL3AppId, SdxParticipantsConfig.class);

        if (peersConfig == null) {
            return;
        }

        peerConnectPoints.forEach(
                cp -> neighbourService.unregisterNeighbourHandler(cp, externalHandler, sdxL3AppId));
        peerConnectPoints.clear();

        peersConfig.bgpPeers().forEach(peer -> {
                    ConnectPoint cp = sdxL3PeerService.getInterfaceForPeer(peer.ip()).connectPoint();
                    neighbourService.registerNeighbourHandler(cp, externalHandler, sdxL3AppId);
                    peerConnectPoints.add(cp);
        });
    }

    private void configureSpeakerHandlers() {
        BgpConfig config = configService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        if (config == null) {
            return;
        }

        speakerConnectPoints.forEach(
                cp -> neighbourService.unregisterNeighbourHandler(cp, internalHandler, sdxL3AppId));
        speakerConnectPoints.clear();

        config.bgpSpeakers().forEach(speaker -> {
            neighbourService.registerNeighbourHandler(speaker.connectPoint(), internalHandler, sdxL3AppId);
            speakerConnectPoints.add(speaker.connectPoint());
        });
    }

    /**
     * Neighbour message handler for ports connected to the internal BGP speakers.
     */
    private class InternalSpeakerNeighbourHandler implements
            NeighbourMessageHandler {
        @Override
        public void handleMessage(NeighbourMessageContext context, HostService hostService) {
            // For messages coming from a BGP speaker, look at the sender address
            // to find the interface to proxy to
            interfaceService.getInterfacesByIp(context.sender())
                    .forEach(context::forward);
        }
    }

    /**
     * Neighbour message handler for ports connected to the external BGP peers.
     */
    public class ExternalPeerNeighbourHandler implements
            NeighbourMessageHandler {

        @Override
        public void handleMessage(NeighbourMessageContext context, HostService hostService) {
            if (sentFromPeerToPeer(context)) {
                forwardMessageToPeer(context);
            } else {
                handleMessageForSpeaker(context, hostService);
            }
        }

        private void handleMessageForSpeaker(NeighbourMessageContext context, HostService hostService) {
            switch (context.type()) {
                case REQUEST:
                    // Reply to requests that target our configured interface IP
                    // address on this port. Drop all other requests.
                    interfaceService.getInterfacesByPort(context.inPort())
                            .stream()
                            .filter(intf -> intf.ipAddressesList()
                                    .stream()
                                    .anyMatch(ia -> ia.ipAddress().equals(context.target()) &&
                                            ia.subnetAddress().contains(context.sender())))
                            .forEach(intf -> context.reply(intf.mac()));
                    break;
                case REPLY:
                    // Proxy replies over to our internal BGP speaker if the host
                    // is known to us
                    Host h = hostService.getHostsByMac(context.dstMac()).stream()
                            .findFirst()
                            .get();
                    if (h == null) {
                        context.drop();
                    } else {
                        VlanId bgpSpeakerVlanId = h.vlan();
                        if (!bgpSpeakerVlanId.equals(VlanId.NONE)) {
                            context.packet().setVlanID(bgpSpeakerVlanId.toShort());
                        } else {
                            context.packet().setVlanID(Ethernet.VLAN_UNTAGGED);
                        }
                        context.forward(h.location());
                    }
                    break;
                default:
                    break;
            }
        }

        private boolean sentFromPeerToPeer(NeighbourMessageContext context) {
            return isPeerAddress(context.sender()) && isPeerAddress(context.target());
        }

        private boolean isPeerAddress(IpAddress ip) {
            BgpConfig config = configService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

            if (config == null) {
                return false;
            }

            return config.bgpSpeakers()
                    .stream()
                    .flatMap(speaker -> speaker.peers().stream())
                    .anyMatch(peerAddress -> peerAddress.equals(ip));
        }

        private void forwardMessageToPeer(NeighbourMessageContext context) {
            Interface interfaceForPeer =
                    sdxL3PeerService.getInterfaceForPeer(context.target());
            if (interfaceForPeer != null) {
                context.forward(interfaceForPeer);
            }
        }

    }

    private class InternalNetworkConfigListener implements
            NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
            case CONFIG_REGISTERED:
                break;
            case CONFIG_UNREGISTERED:
                break;
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
            case CONFIG_REMOVED:
                if (event.configClass() == RoutingService.CONFIG_CLASS) {
                    configurePeerHandlers();
                    configureSpeakerHandlers();
                } else if (event.configClass() == SdxL3PeerService.CONFIG_CLASS) {
                    configurePeerHandlers();
                }
                break;
            default:
                break;
            }
        }
    }
}
