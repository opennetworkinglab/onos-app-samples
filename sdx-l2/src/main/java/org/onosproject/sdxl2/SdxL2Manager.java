/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.sdxl2;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.packet.Ethernet.TYPE_IPV6;
import static org.onlab.packet.ICMP6.NEIGHBOR_ADVERTISEMENT;
import static org.onlab.packet.ICMP6.NEIGHBOR_SOLICITATION;
import static org.onlab.packet.IPv6.PROTOCOL_ICMP6;
import static org.onosproject.net.packet.PacketPriority.CONTROL;

/**
 * Implementation of the SdxL2Service.
 */
@Component(immediate = true)
@Service
public class SdxL2Manager implements SdxL2Service {

    private static final String SDXL2_APP = "org.onosproject.sdxl2";
    private static Logger log = LoggerFactory.getLogger(SdxL2Manager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SdxL2Store sdxL2Store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    protected SdxL2Processor processor = new SdxL2Processor();

    protected SdxL2ArpNdpHandler arpndpHandler;

    protected ApplicationId appId;

    protected SdxL2MonitoringService monitoringManager;

    protected String vcType = "MAC";


    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(SDXL2_APP);
        monitoringManager = new SdxL2MonitoringManager(appId, intentService, edgePortService);
        handleArpNdp();
        log.info("Started");
    }



    @Deactivate
    protected void deactivate() {
        this.cleanSdxL2();
        unhandleArpNdp();
        log.info("Stopped");
    }

    /**
     * Creates a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     */
    @Override
    public void createSdxL2(String sdxl2) {

        checkNotNull(sdxl2, "sdxl2 name cannot be null");
        checkState(!sdxl2.contains(","), "sdxl2 names cannot contain commas");
        checkState(!sdxl2.contains("|"), "sdxl2 names cannot contain pipe");
        checkState(!sdxl2.contains("-"), "sdxl2 names cannot contain dash");
        checkState(!sdxl2.contains(":"), "sdxl2 names cannot contain colon");

        try {
            this.sdxL2Store.putSdxL2(sdxl2);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Deletes a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     */
    @Override
    public void deleteSdxL2(String sdxl2) {

        checkNotNull(sdxl2, "sdxl2 name cannot be null");

        try {
            this.sdxL2Store.removeSdxL2(sdxl2);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Returns a set of SDX-L2 names.
     *
     * @return a set of SDX-L2 names
     */
    @Override
    public Set<String> getSdxL2s() {
        return this.sdxL2Store.getSdxL2s();
    }

    /**
     * Adds an SDX-L2 connection point to an SDX-L2.
     *
     * @param sdxl2   SDX-L2 name
     * @param sdxl2cp SDX-L2 connection point object
     */
    @Override
    public void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint sdxl2cp) {

        checkNotNull(sdxl2, "sdxl2 name cannot be null");
        checkNotNull(sdxl2cp, "SdxL2ConnectionPoint cannot be null");

        try {
            this.sdxL2Store.addSdxL2ConnectionPoint(sdxl2, sdxl2cp);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Returns all the SDX-L2 connection points names in a SDX-L2 or all the SDX-L2 connection points names.
     *
     * @param sdxl2 SDX-L2 name
     * @return a set of SDX-L2 connection points names
     */
    @Override
    public Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2) {

        try {
            return this.sdxL2Store.getSdxL2ConnectionPoints(sdxl2);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

        return Collections.emptySet();

    }

    /**
     * Removes an SDX-L2 connection point from an SDX-L2.
     *
     * @param sdxl2cp SDX-L2 connection point name
     */
    @Override
    public void removeSdxL2ConnectionPoint(String sdxl2cp) {

        checkNotNull(sdxl2cp, "SdxL2ConnectionPoint name cannot be null");

        try {
            this.sdxL2Store.removeSdxL2ConnectionPoint(sdxl2cp);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Returns an SDX-L2 connection point in a SDX-L2.
     *
     * @param sdxl2cp SDX-L2 connection point name
     * @return the relative SdxL2ConnectionPoint object
     */
    @Override
    public SdxL2ConnectionPoint getSdxL2ConnectionPoint(String sdxl2cp) {
        checkNotNull(sdxl2cp, "SdxL2ConnectionPoint name cannot be null");
        try {
            return this.sdxL2Store.getSdxL2ConnectionPoint(sdxl2cp);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

        return null;
    }

    /**
     * Returns the state of the Intent that has been provided as input.
     *
     * @param intentKey key of the intent;
     * @return the last state of the intent;
     */
    @Override
    public SdxL2State getIntentState(Key intentKey) {
        checkNotNull(intentKey, "Intent key cannot be null");
        return this.monitoringManager.getIntentState(intentKey);
    }

    /**
     * Returns the state of the EdgePort that has been provided as input.
     *
     * @param edgeport the connect point representing the edge port
     * @return the last state of the edgeport;
     */
    @Override
    public SdxL2State getEdgePortState(ConnectPoint edgeport) {
        checkNotNull(edgeport, "Edge port cannot be null");
        return this.monitoringManager.getEdgePortState(edgeport);
    }

    /**
     * Cleans the state of the Application.
     */
    @Override
    public void cleanSdxL2() {
        this.monitoringManager.cleanup();
    }

    /**
     * It requests ARP and NDP packets to the PacketService
     * and registers the SDX-L2 PacketProcessor.
     */
    private void handleArpNdp() {
        SdxL2ArpNdpHandler.vcType = vcType;
        arpndpHandler = new SdxL2ArpNdpHandler(intentService, packetService, appId);
        packetService.addProcessor(processor, PacketProcessor.director(1));

        // ARP packet
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_ARP);
        packetService.requestPackets(selectorBuilder.build(),
                CONTROL, appId, Optional.<DeviceId>empty());

        // IPv6 Neighbor Solicitation packet.
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_SOLICITATION);
        packetService.requestPackets(selectorBuilder.build(),
                CONTROL, appId, Optional.<DeviceId>empty());

        // IPv6 Neighbor Advertisement packet.
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_ADVERTISEMENT);
        packetService.requestPackets(selectorBuilder.build(),
                CONTROL, appId, Optional.<DeviceId>empty());
    }

    /**
     * Withdraws the requests for ARP/NDP packets and
     * unregisters the SDX-L2 PacketProcessor.
     */
    private void unhandleArpNdp() {
        arpndpHandler = null;
        packetService.removeProcessor(processor);
        processor = null;

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_ARP);
        packetService.cancelPackets(selectorBuilder.build(),
                CONTROL, appId, Optional.<DeviceId>empty());

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_SOLICITATION);
        packetService.cancelPackets(selectorBuilder.build(),
                CONTROL, appId, Optional.<DeviceId>empty());

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_ADVERTISEMENT);
        packetService.cancelPackets(selectorBuilder.build(),
                CONTROL, appId, Optional.<DeviceId>empty());
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class SdxL2Processor implements PacketProcessor {

        /**
         * Processes the inbound packet as specified in the given context.
         *
         * @param context packet processing context
         */
        @Override
        public void process(PacketContext context) {

            /** Stop processing if the packet has been handled, since we
             * can't do any more to it
             */
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }

            boolean handled = false;
            if (ethPkt.getEtherType() == TYPE_ARP) {
                //handle the arp packet.
                handled = arpndpHandler.handlePacket(context);
            } else if (ethPkt.getEtherType() == TYPE_IPV6) {
                IPv6 ipv6Pkt = (IPv6) ethPkt.getPayload();
                if (ipv6Pkt.getNextHeader() == IPv6.PROTOCOL_ICMP6) {
                    ICMP6 icmp6Pkt = (ICMP6) ipv6Pkt.getPayload();
                    if (icmp6Pkt.getIcmpType() == NEIGHBOR_SOLICITATION ||
                            icmp6Pkt.getIcmpType() == NEIGHBOR_ADVERTISEMENT) {
                        // handle ICMPv6 solicitations and advertisements
                        handled = arpndpHandler.handlePacket(context);
                    }
                }
            }

            if (handled) {
                context.block();
            }

        }
    }
}
