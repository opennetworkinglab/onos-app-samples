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

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.routing.config.RoutingConfiguration;
import org.onosproject.sdxl3.SdxL3;
import org.onosproject.sdxl3.SdxL3PeerService;
import org.onosproject.sdxl3.config.SdxParticipantsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the connectivity requirements between peers.
 */
@Service
@Component(immediate = true, enabled = false)
public class SdxL3PeerManager implements SdxL3PeerService {
    private static final int PRIORITY_OFFSET = 1000;

    private static final String SUFFIX_DST = "dst";
    private static final String SUFFIX_SRC = "src";
    private static final String SUFFIX_ICMP = "icmp";

    private static final Logger log = LoggerFactory.getLogger(
            SdxL3PeerManager.class);

    private static final short BGP_PORT = 179;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    private ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY,
                              CONFIG_CLASS, CONFIG_KEY) {
                @Override
                public SdxParticipantsConfig createConfig() {
                    return new SdxParticipantsConfig();
                }
            };

    private ApplicationId sdxAppId;
    private ApplicationId routerAppId;

    private final Map<Key, PointToPointIntent> peerIntents = new HashMap<>();

    private final InternalNetworkConfigListener configListener
            = new InternalNetworkConfigListener();

    private final InternalInterfaceListener interfaceListener
            = new InternalInterfaceListener();

    @Activate
    public void activate() {
        sdxAppId = coreService.registerApplication(SdxL3.SDX_L3_APP);
        routerAppId = coreService.registerApplication(RoutingService.ROUTER_APP_ID);

        RoutingConfiguration.register(registry);

        registry.registerConfigFactory(configFactory);

        configService.addListener(configListener);
        interfaceService.addListener(interfaceListener);

        setUpConnectivity();

        log.info("Connectivity with BGP peers established");
    }

    @Deactivate
    public void deactivate() {
        configService.removeListener(configListener);
        interfaceService.removeListener(interfaceListener);

        log.info("Connectivity with BGP peers stopped");
    }

    /**
     * Adds details for a BGP peer to the SDX-L3 configuration.
     *
     * @param peerName      Peer name
     * @param peerAddress   Peer IP address
     * @param port          Connection point with peer
     * @param interfaceName Name of the interface configured on port
     */
    @Override
    public void addPeerDetails(String peerName,
                               IpAddress peerAddress,
                               ConnectPoint port,
                               String interfaceName) {

        BgpConfig bgpConfig = getBgpConfig();
        if (bgpConfig == null) {
            throw new ItemNotFoundException("BGP configuration not found");
        }

        if (!peerAddressExists(bgpConfig, peerAddress)) {
            throw new ItemNotFoundException("Peer IP not found");
        }

        Interface peerInterface = getInterface(port, interfaceName);
        if (peerInterface == null) {
            throw new ItemNotFoundException("Interface not found");
        }

        if (!interfaceSubnetIncludesIp(peerInterface, peerAddress)) {
            throw new IllegalArgumentException("Interface not configured for IP "
                                                       + peerAddress);
        }

        Interface confInterface = getConfiguredInterfaceForPeer(peerAddress);
        if (confInterface != null) {
            if (confInterface.equals(peerInterface)) {
                // Do nothing since the association exists.
                return;
            } else {
                // The peer is associated with another interface.
                throw new IllegalArgumentException("Peer details already exist");
            }
        }

        SdxParticipantsConfig peersConfig =
                configService.addConfig(sdxAppId, SdxParticipantsConfig.class);
        if (peerName != null && peerNameExists(peersConfig, peerName)) {
            throw new IllegalArgumentException("Peer name in use");
        }

        addPeerToConf(peersConfig, peerName, peerAddress, port, interfaceName);
        configService.
                applyConfig(sdxAppId, SdxParticipantsConfig.class, peersConfig.node());
    }

    /**
     * Removes details for a BGP peer to the SDX-L3 configuration.
     *
     * @param peerAddress Peer IP address
     */
    @Override
    public void removePeerDetails(IpAddress peerAddress) {
        BgpConfig bgpConfig = getBgpConfig();
        if (bgpConfig == null) {
            throw new ItemNotFoundException("BGP configuration not found");
        }

        SdxParticipantsConfig peersConfig =
                configService.addConfig(sdxAppId, SdxParticipantsConfig.class);

        if (peersConfig.getPeerForIp(peerAddress) == null) {
            throw new ItemNotFoundException("Peer details not found");
        }

        removePeerFromConf(peersConfig, peerAddress);
        configService.applyConfig(sdxAppId,
                                  SdxParticipantsConfig.class,
                                  peersConfig.node());
    }

    /**
     * Returns BGP configuration has been specified and any BGP speaker
     * exists in this configuration, else null is returned.
     *
     * @return BGP configuration or null
     */
    private BgpConfig getBgpConfig() {
        BgpConfig bgpConfig = configService.
                getConfig(routerAppId, BgpConfig.class);

        if (bgpConfig == null || bgpConfig.bgpSpeakers().isEmpty()) {
            return null;
        }
        return bgpConfig;
    }

    private Interface getInterface(ConnectPoint port, String interfaceName) {
        Optional<Interface> interfaceMatch = interfaceService
                .getInterfacesByPort(port)
                .stream()
                .filter(intf -> intf.name().equals(interfaceName))
                .findFirst();
        if (!interfaceMatch.isPresent()) {
            // No such interface name configured for given connectPoint
            return null;
        }
        return interfaceMatch.get();
    }

    private boolean interfaceSubnetIncludesIp(Interface peerInterface, IpAddress peerAddress) {
        if (peerInterface.ipAddressesList().stream()
                .anyMatch(intfIp -> intfIp.subnetAddress().
                        contains(peerAddress))) {
            // Interface configured subnet not including peer address
            return true;
        }
        return false;
    }

    private boolean peerNameExists(SdxParticipantsConfig config, String peerName) {
        if (config.getPeerForName(Optional.of(peerName)) == null) {
            return false;
        }
        return true;
    }

    /**
     * Adds the peer to the SdxProvidersConfig .
     *
     * @param peersConfig the BGP peers configuration
     */
    private void addPeerToConf(SdxParticipantsConfig peersConfig, String peerName,
                               IpAddress peerAddress, ConnectPoint port,
                               String interfaceName) {
        log.debug("Adding peer with IP to configuration: {}", peerAddress);
        SdxParticipantsConfig.PeerConfig peer = new SdxParticipantsConfig.
                PeerConfig(Optional.ofNullable(peerName), peerAddress,
                           port, interfaceName);

        peersConfig.addPeer(peer);
    }

    /**
     * Removes the speaker from the BgpConfig service.
     *
     * @param peersConfig the BGP peeers configuration
     */
    private void removePeerFromConf(SdxParticipantsConfig peersConfig,
                                    IpAddress peerAddress) {
        log.debug("Removing peer details from configuration: {}",
                  peerAddress.toString());
        peersConfig.removePeer(peerAddress);
    }

    /**
     * Returns true if a given IP address has been specified as a BGP peer
     * address in the network configuration.
     *
     * @param bgpConfig BGP configuration
     * @param peerAddress IP address of peer
     * @return whether address has been specified for a peer or not
     */
    private Boolean peerAddressExists(BgpConfig bgpConfig,
                                      IpAddress peerAddress) {
        List<IpAddress> peeringAddresses =
                getPeerAddresses(bgpConfig);
        if (!peeringAddresses.contains(peerAddress)) {
            return false;
        }
        return true;
    }

    private Interface getConfiguredInterfaceForPeer(IpAddress peerAddress) {
        if (sdxAppId == null) {
            return null;
        }

        SdxParticipantsConfig config = configService.getConfig(sdxAppId, SdxParticipantsConfig.class);
        if (config == null) {
            return null;
        }

        ConnectPoint port = config.getPortForPeer(peerAddress);
        String intfName = config.getInterfaceNameForPeer(peerAddress);
        if (port != null && intfName != null) {
            Optional<Interface> interfaceMatch = interfaceService
                    .getInterfacesByPort(port)
                    .stream()
                    .filter(intf -> intf.name().equals(intfName))
                    .findFirst();
            if (interfaceMatch.isPresent()) {
                return interfaceMatch.get();
            }
        }
        return null;
    }

    /**
     * Returns the interface used as connection point to peer.
     *
     * @param peerAddress IP address of peer
     * @return interface to the peer
     */
    @Override
    public Interface getInterfaceForPeer(IpAddress peerAddress) {
        Interface peeringInterface = getConfiguredInterfaceForPeer(peerAddress);
        if (peeringInterface == null) {
            peeringInterface = interfaceService.getMatchingInterface(peerAddress);
        }
        return peeringInterface;
    }

    @Override
    public List<IpAddress> getPeerAddresses(BgpConfig bgpConfig) {
        List<IpAddress> peeringAddresses = Lists.newArrayList();

        List<BgpConfig.BgpSpeakerConfig> bgpSpeakers =
                Lists.newArrayList(bgpConfig.bgpSpeakers());
        bgpSpeakers.forEach(
                s -> peeringAddresses.addAll(s.peers()));

        return peeringAddresses;
    }

    /**
     * Sets up paths to establish connectivity between all internal
     * BGP speakers and external BGP peers.
     */
    private void setUpConnectivity() {
        BgpConfig config = getBgpConfig();
        if (config == null) {
            log.warn("No BGP configuration found");
            return;
        }

        Map<Key, PointToPointIntent> existingIntents = new HashMap<>(peerIntents);

        for (BgpConfig.BgpSpeakerConfig bgpSpeaker : config.bgpSpeakers()) {
            log.debug("Start to set up BGP paths for BGP speaker: {}",
                    bgpSpeaker);

            buildSpeakerIntents(bgpSpeaker).forEach(i -> {
                PointToPointIntent intent = existingIntents.remove(i.key());
                if (intent == null || !IntentUtils.intentsAreEqual(i, intent)) {
                    peerIntents.put(i.key(), i);
                    intentSynchronizer.submit(i);
                }
            });
        }

        // Remove any remaining intents that we used to have that we don't need
        // anymore
        existingIntents.values().forEach(i -> {
            peerIntents.remove(i.key());
            intentSynchronizer.withdraw(i);
        });
    }

    private Collection<PointToPointIntent> buildSpeakerIntents(BgpConfig.BgpSpeakerConfig speaker) {
        List<PointToPointIntent> intents = new ArrayList<>();

        // Get the BGP Speaker VLAN Id
        VlanId bgpSpeakerVlanId = speaker.vlan();

        for (IpAddress peerAddress : speaker.peers()) {
            Interface peeringInterface = getInterfaceForPeer(peerAddress);

            if (peeringInterface == null) {
                log.debug("No peering interface found for peer {} on speaker {}",
                        peerAddress, speaker);
                continue;
            }

            IpAddress bgpSpeakerAddress = null;
            for (InterfaceIpAddress address : peeringInterface.ipAddressesList()) {
                if (address.subnetAddress().contains(peerAddress)) {
                    bgpSpeakerAddress = address.ipAddress();
                    break;
                }
            }

            checkNotNull(bgpSpeakerAddress);

            VlanId peerVlanId = peeringInterface.vlan();

            intents.addAll(buildIntents(speaker.connectPoint(), bgpSpeakerVlanId,
                                        bgpSpeakerAddress,
                                        peeringInterface.connectPoint(),
                                        peerVlanId,
                                        peerAddress));
        }

        return intents;
    }

    /**
     * Builds the required intents between a BGP speaker and an external router.
     *
     * @param portOne the BGP speaker connect point
     * @param vlanOne the BGP speaker VLAN
     * @param ipOne the BGP speaker IP address
     * @param portTwo the external BGP peer connect point
     * @param vlanTwo the external BGP peer VLAN
     * @param ipTwo the external BGP peer IP address
     * @return the intents to install
     */
    private Collection<PointToPointIntent> buildIntents(ConnectPoint portOne,
                                                        VlanId vlanOne,
                                                        IpAddress ipOne,
                                                        ConnectPoint portTwo,
                                                        VlanId vlanTwo,
                                                        IpAddress ipTwo) {

        List<PointToPointIntent> intents = new ArrayList<>();

        TrafficTreatment.Builder treatmentToPeer = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder treatmentToSpeaker = DefaultTrafficTreatment.builder();
        TrafficSelector selector;
        Key key;

        byte tcpProtocol;
        byte icmpProtocol;

        if (ipOne.isIp4()) {
            tcpProtocol = IPv4.PROTOCOL_TCP;
            icmpProtocol = IPv4.PROTOCOL_ICMP;
        } else {
            tcpProtocol = IPv6.PROTOCOL_TCP;
            icmpProtocol = IPv6.PROTOCOL_ICMP6;
        }

        // Add VLAN treatment for traffic going from BGP speaker to BGP peer
        treatmentToPeer = applyVlanTreatment(vlanOne, vlanTwo, treatmentToPeer);

        // Path from BGP speaker to BGP peer matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanOne,
                ipOne,
                ipTwo,
                null,
                BGP_PORT);

        key = buildKey(ipOne, ipTwo, SUFFIX_DST);

        intents.add(PointToPointIntent.builder()
                .appId(sdxAppId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToPeer.build())
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP speaker to BGP peer matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanOne,
                ipOne,
                ipTwo,
                BGP_PORT,
                null);

        key = buildKey(ipOne, ipTwo, SUFFIX_SRC);

        intents.add(PointToPointIntent.builder()
                .appId(sdxAppId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToPeer.build())
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // ICMP path from BGP speaker to BGP peer
        selector = buildSelector(icmpProtocol,
                vlanOne,
                ipOne,
                ipTwo,
                null,
                null);

        key = buildKey(ipOne, ipTwo, SUFFIX_ICMP);

        intents.add(PointToPointIntent.builder()
                .appId(sdxAppId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToPeer.build())
                .ingressPoint(portOne)
                .egressPoint(portTwo)
                .priority(PRIORITY_OFFSET)
                .build());

        // Add VLAN treatment for traffic going from BGP peer to BGP speaker
        treatmentToSpeaker = applyVlanTreatment(vlanTwo, vlanOne, treatmentToSpeaker);

        // Path from BGP peer to BGP speaker matching destination TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanTwo,
                ipTwo,
                ipOne,
                null,
                BGP_PORT);

        key = buildKey(ipTwo, ipOne, SUFFIX_DST);

        intents.add(PointToPointIntent.builder()
                .appId(sdxAppId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToSpeaker.build())
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        // Path from BGP peer to BGP speaker matching source TCP port 179
        selector = buildSelector(tcpProtocol,
                vlanTwo,
                ipTwo,
                ipOne,
                BGP_PORT,
                null);

        key = buildKey(ipTwo, ipOne, SUFFIX_SRC);

        intents.add(PointToPointIntent.builder()
                .appId(sdxAppId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToSpeaker.build())
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        // ICMP path from BGP peer to BGP speaker
        selector = buildSelector(icmpProtocol,
                vlanTwo,
                ipTwo,
                ipOne,
                null,
                null);

        key = buildKey(ipTwo, ipOne, SUFFIX_ICMP);

        intents.add(PointToPointIntent.builder()
                .appId(sdxAppId)
                .key(key)
                .selector(selector)
                .treatment(treatmentToSpeaker.build())
                .ingressPoint(portTwo)
                .egressPoint(portOne)
                .priority(PRIORITY_OFFSET)
                .build());

        return intents;
    }

    /**
     * Builds a traffic selector based on the set of input parameters.
     *
     * @param ipProto IP protocol
     * @param ingressVlanId VLAN Id configured on the ingress interface
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @param srcTcpPort source TCP port, or null if shouldn't be set
     * @param dstTcpPort destination TCP port, or null if shouldn't be set
     * @return the new traffic selector
     */
    private TrafficSelector buildSelector(byte ipProto,
                                          VlanId ingressVlanId,
                                          IpAddress srcIp,
                                          IpAddress dstIp, Short srcTcpPort,
                                          Short dstTcpPort) {
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder().matchIPProtocol(ipProto);
        // Match on any VLAN Id if a VLAN Id configured on the ingress interface
        if (!ingressVlanId.equals(VlanId.NONE)) {
            builder.matchVlanId(VlanId.ANY);
        }

        if (dstIp.isIp4()) {
            builder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                    .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));
        } else {
            builder.matchEthType(Ethernet.TYPE_IPV6)
                    .matchIPv6Src(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET6_MASK_LENGTH))
                    .matchIPv6Dst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET6_MASK_LENGTH));
        }

        if (srcTcpPort != null) {
            builder.matchTcpSrc(TpPort.tpPort(srcTcpPort));
        }

        if (dstTcpPort != null) {
            builder.matchTcpDst(TpPort.tpPort(dstTcpPort));
        }

        return builder.build();
    }

    /**
     * Builds an intent Key for a point-to-point intent based off the source
     * and destination IP address, as well as a suffix String to distinguish
     * between different types of intents between the same source and
     * destination.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @param suffix suffix string
     * @return intent key
     */
    private Key buildKey(IpAddress srcIp, IpAddress dstIp, String suffix) {
        String keyString = new StringBuilder()
                .append(srcIp.toString())
                .append("-")
                .append(dstIp.toString())
                .append("-")
                .append(suffix)
                .toString();

        return Key.of(keyString, sdxAppId);
    }

    /**
     * Adds the VLAN Id treatment before building the intents, depending on how
     * the VLAN Ids of the BGP speakers and the BGP peers are configured.
     */
    private TrafficTreatment.Builder applyVlanTreatment(VlanId vlanOne,
                                                        VlanId vlanTwo,
                                                        TrafficTreatment.Builder treatment) {
        if (!vlanOne.equals(vlanTwo)) {
            // VLANs are different. Do some VLAN treatment
            if (vlanTwo.equals(VlanId.NONE)) {
                // VLAN two is none. VLAN one is set. Do a pop
                treatment.popVlan();
            } else {
                // Either both VLANs are set or vlanOne is not
                if (vlanOne.equals(VlanId.NONE)) {
                    // VLAN one is none. VLAN two is set. Push the VLAN header
                    treatment.pushVlan();
                }
                // Set the VLAN Id to the egress VLAN Id
                treatment.setVlanId(vlanTwo);
            }
        }
        return treatment;
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {

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
                if (event.configClass() == RoutingService.CONFIG_CLASS ||
                        event.configClass() == CONFIG_CLASS) {
                    setUpConnectivity();
                }
                break;
            default:
                break;
            }
        }
    }

    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            switch (event.type()) {
                case INTERFACE_ADDED:
                case INTERFACE_UPDATED:
                case INTERFACE_REMOVED:
                    setUpConnectivity();
                    break;
                default:
                    break;
            }
        }
    }

}
