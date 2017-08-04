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

package org.onosproject.sdxl2;


import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PACKET_WRITE;

/**
 * Implementation of ARP and NDP handler based on ProxyArpManager.
 */
public class SdxL2ArpNdpHandler {

    private static final String MSG_NULL = "ARP or NDP message cannot be null.";

    private IntentService intentService;

    private ApplicationId applicationId;

    private PacketService packetService;

    private static String vcType;

    /**
     * Creates an ARP/NDP packet handler.
     *
     * @param intentService IntentService object
     * @param packetService PacketService object
     * @param applicationId ApplicationId object
     */
    public SdxL2ArpNdpHandler(IntentService intentService,
                              PacketService packetService, ApplicationId applicationId) {
        this.intentService = intentService;
        this.packetService = packetService;
        this.applicationId = applicationId;
    }

    /**
     * Handles the ARP/NDP packets.
     *
     * @param context containing the packet to process.
     * @return true if the packet has been handled otherwise false.
     */
    boolean handlePacket(PacketContext context) {

        checkPermission(PACKET_WRITE);

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if (ethPkt == null) {
            return false;
        }

        MessageContext msgContext = createContext(ethPkt, pkt.receivedFrom());

        if (msgContext == null) {
            return false;
        }

        switch (msgContext.type()) {
            case REPLY:
                return relay(msgContext);
            case REQUEST:
                return relay(msgContext);
            default:
        }

        return false;
    }

    /**
     * Relays the packets edge-to-edge
     * applying the egress actions.
     *
     * @param ctx containing the ARP/NDP packet.
     * @return true if the packet has been handled otherwise false.
     */
    private boolean relay(MessageContext ctx) {

        Iterator<Intent> intents = intentService.getIntents().iterator();
        boolean resolved = false;
        Intent intent;
        while (intents.hasNext()) {

            intent = intents.next();
            if (intent.appId().equals(applicationId) &&
                    intentService.getIntentState(intent.key()) == IntentState.INSTALLED) {

                PointToPointIntent ppIntent = (PointToPointIntent) intent;
                TrafficSelector selector = ctx.selector();

                if (ctx.inPort().equals(ppIntent.ingressPoint()) &&
                        partialequals(selector, ppIntent.selector())) {

                    Ethernet packet = ctx.packet();

                    checkNotNull(packet, MSG_NULL);

                    applytreatment(packet, ppIntent.treatment());

                    TrafficTreatment.Builder builder;
                    builder = DefaultTrafficTreatment.builder();
                    builder.setOutput(ppIntent.egressPoint().port());
                    TrafficTreatment treatment = builder.build();


                    ByteBuffer buf = ByteBuffer.wrap(packet.serialize());
                    packetService.emit(new DefaultOutboundPacket(ppIntent.egressPoint().deviceId(),
                            treatment, buf));
                    resolved = true;
                    break;

                }
            }
        }

        return resolved;
    }

    /**
     * Verifies if the TrafficSelector of the packet is equal
     * to the one derived from a SDX-L2 Intent.
     *
     * @param generated the traffic selector of the packet.
     * @param fromIntent the traffic selector of the intent.
     * @return true if all the criteria match.
     */
    private boolean partialequals(TrafficSelector generated, TrafficSelector fromIntent) {

        Set<Criterion> criteria = generated.criteria();
        for (Criterion criterion : criteria) {
            if (!vcType.equals(VirtualCircuitMechanism.MAC) && criterion.type().equals(Type.ETH_SRC)) {
                continue;
            }

            if (!fromIntent.criteria().contains(criterion)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Applies the egress TrafficTreatment we found
     * in the associated SDX-L2 Intent.
     *
     * @param packet the packet to modify.
     * @param treatment the TrafficTreatment to apply.
     */
    private void applytreatment(Ethernet packet, TrafficTreatment treatment) {

        for (Instruction instruction : treatment.allInstructions()) {
            if (instruction.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2instruction = (L2ModificationInstruction) instruction;
                switch (l2instruction.subtype()) {
                    case VLAN_ID:
                        ModVlanIdInstruction modVlanIdInstruction = (ModVlanIdInstruction) l2instruction;
                        packet.setVlanID(modVlanIdInstruction.vlanId().toShort());
                        break;
                    case VLAN_PUSH:
                        packet.setVlanID((short) 1);
                        break;
                    case VLAN_POP:
                        packet.setVlanID(Ethernet.VLAN_UNTAGGED);
                        break;
                    default:
                }
            }
        }

    }

    /**
     * Attempts to create a MessageContext for the given Ethernet frame. If the
     * frame is a valid ARP or NDP request or response, a context will be
     * created.
     *
     * @param eth input Ethernet frame
     * @param inPort in port
     * @return MessageContext if the packet was ARP or NDP, otherwise null
     */
    private MessageContext createContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() == Ethernet.TYPE_ARP) {
            return createArpContext(eth, inPort);
        } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            return createNdpContext(eth, inPort);
        }
        return null;
    }

    /**
     * Extracts context information from ARP packets.
     *
     * @param eth input Ethernet frame that is thought to be ARP
     * @param inPort in port
     * @return MessageContext object if the packet was a valid ARP packet,
     * otherwise null
     */
    private MessageContext createArpContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() != Ethernet.TYPE_ARP) {
            return null;
        }

        ARP arp = (ARP) eth.getPayload();

        IpAddress target = Ip4Address.valueOf(arp.getTargetProtocolAddress());
        IpAddress sender = Ip4Address.valueOf(arp.getSenderProtocolAddress());

        MessageType type;
        if (arp.getOpCode() == ARP.OP_REQUEST) {
            type = MessageType.REQUEST;
        } else if (arp.getOpCode() == ARP.OP_REPLY) {
            type = MessageType.REPLY;
        } else {
            return null;
        }
        return new MessageContext(eth, inPort, Protocol.ARP, type, target, sender);
    }

    /**
     * Extracts context information from NDP packets.
     *
     * @param eth input Ethernet frame that is thought to be NDP
     * @param inPort in port
     * @return MessageContext object if the packet was a valid NDP packet,
     * otherwise null
     */
    private MessageContext createNdpContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() != Ethernet.TYPE_IPV6) {
            return null;
        }
        IPv6 ipv6 = (IPv6) eth.getPayload();

        if (ipv6.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
            return null;
        }
        ICMP6 icmpv6 = (ICMP6) ipv6.getPayload();

        IpAddress sender = Ip6Address.valueOf(ipv6.getSourceAddress());
        IpAddress target = null;

        MessageType type;
        if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_SOLICITATION) {
            type = MessageType.REQUEST;
            NeighborSolicitation nsol = (NeighborSolicitation) icmpv6.getPayload();
            target = Ip6Address.valueOf(nsol.getTargetAddress());
        } else if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
            type = MessageType.REPLY;
        } else {
            return null;
        }
        return new MessageContext(eth, inPort, Protocol.NDP, type, target, sender);
    }

    /**
     * Provides supported protocols.
     */
    private enum Protocol {
        ARP, NDP
    }

    /**
     * Provides supported messages.
     */
    private enum MessageType {
        REQUEST, REPLY
    }

    /**
     * Provides context information for a particular ARP or NDP message, with
     * a unified interface to access data regardless of protocol.
     */
    private class MessageContext {
        private Protocol protocol;
        private MessageType type;

        private IpAddress target;
        private IpAddress sender;

        private Ethernet eth;
        private ConnectPoint inPort;

        MessageContext(Ethernet eth, ConnectPoint inPort,
                       Protocol protocol, MessageType type,
                       IpAddress target, IpAddress sender) {
            this.eth = eth;
            this.inPort = inPort;
            this.protocol = protocol;
            this.type = type;
            this.target = target;
            this.sender = sender;
        }

        public ConnectPoint inPort() {
            return inPort;
        }

        public Ethernet packet() {
            return eth;
        }

        public Protocol protocol() {
            return protocol;
        }

        public MessageType type() {
            return type;
        }

        public VlanId vlan() {
            return VlanId.vlanId(eth.getVlanID());
        }

        public MacAddress srcMac() {
            return MacAddress.valueOf(eth.getSourceMACAddress());
        }

        public IpAddress target() {
            return target;
        }

        public IpAddress sender() {
            return sender;
        }

        /**
         * Builds TrafficSelector using data
         * in MessageContext.
         *
         * @return the TrafficSelector object
         */
        public TrafficSelector selector() {
            switch (vcType) {
                case VirtualCircuitMechanism.MPLS:
                case VirtualCircuitMechanism.VLAN:
                default:
            }
            return buildMacSelector(this.srcMac(), this.vlan());
        }

        /**
         * Builds TrafficSelector for MAC
         * based tunnels.
         *
         * @param srcMac the sender's MAC address.
         * @param ingressTag the VLAN tag at ingress.
         * @return the TrafficSelector object
         */
        private TrafficSelector buildMacSelector(MacAddress srcMac, VlanId ingressTag) {

            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthSrc(srcMac);
            if (ingressTag.toShort() != VlanId.UNTAGGED) {
                selectorBuilder.matchVlanId(ingressTag);
            }
            return selectorBuilder.build();
        }
    }

    /**
     * Retrieves the SDX-L2 VC type.
     *
     * @return VC type
     */
    public static String getVcType() {
        return vcType;
    }

    /**
     * Sets up the SDX-L2 VC type.
     *
     * @param vcType VC type
     */
    static void setVcType(String vcType) {
        SdxL2ArpNdpHandler.vcType = vcType;
    }
}
