/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.dhcpserver.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPOption;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcpserver.DHCPService;
import org.onosproject.dhcpserver.DHCPStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.onlab.packet.MacAddress.valueOf;

/**
 * Skeletal ONOS DHCP Server application.
 */
@Component(immediate = true)
@Service
public class DHCPManager implements DHCPService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private DHCPPacketProcessor processor = new DHCPPacketProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DHCPStore dhcpStore;

    private ApplicationId appId;

    // TODO Make the hardcoded values configurable.

    private static final String MY_IP = "10.0.0.2";

    private static final MacAddress MY_MAC = valueOf("4f:4f:4f:4f:4f:4f");

    /**
     * leaseTime - 10 mins or 600s.
     * renewalTime - 5 mins or 300s.
     * rebindingTime - 6 mins or 360s.
     */

    private static int leaseTime = 600;

    private static int renewalTime = 300;

    private static int rebindingTime = 360;

    private static byte packetTTL = (byte) 127;

    private static String subnetMask = "255.0.0.0";

    private static String broadcastAddress = "10.255.255.255";

    @Activate
    protected void activate() {
        // start the dhcp server
        appId = coreService.registerApplication("org.onosproject.dhcpserver");

        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 10);
        requestPackets();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);

        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(UDP.DHCP_SERVER_PORT)
                .matchUdpSrc(UDP.DHCP_CLIENT_PORT);

        packetService.cancelPackets(selectorServer.build(), PacketPriority.CONTROL, appId);
        log.info("Stopped");
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackets() {

        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(UDP.DHCP_SERVER_PORT)
                .matchUdpSrc(UDP.DHCP_CLIENT_PORT);

        packetService.requestPackets(selectorServer.build(),
                PacketPriority.CONTROL, appId);
    }

    @Override
    public Map<MacAddress, Ip4Address> listMapping() {

        return dhcpStore.listMapping();
    }

    @Override
    public int getLeaseTime() {
        return leaseTime;
    }

    @Override
    public int getRenewalTime() {
        return renewalTime;
    }

    @Override
    public int getRebindingTime() {
        return rebindingTime;
    }

    @Override
    public boolean setStaticMapping(MacAddress macID, Ip4Address ipAddress) {
        return dhcpStore.assignStaticIP(macID, ipAddress);
    }

    @Override
    public boolean removeStaticMapping(MacAddress macID) {
        return dhcpStore.removeStaticIP(macID);
    }

    @Override
    public Iterable<Ip4Address> getAvailableIPs() {
        return dhcpStore.getAvailableIPs();
    }

    private class DHCPPacketProcessor implements PacketProcessor {

        /**
         * Builds the DHCP Reply packet.
         *
         * @param packet the incoming Ethernet frame
         * @param ipOffered the IP offered by the DHCP Server
         * @param outgoingMessageType the message type of the outgoing packet
         * @return the Ethernet reply frame
         */
        private Ethernet buildReply(Ethernet packet, String ipOffered, byte outgoingMessageType) {
            Ip4Address myIPAddress = Ip4Address.valueOf(MY_IP);
            Ip4Address ipAddress;

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(MY_MAC);
            ethReply.setDestinationMACAddress(packet.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_IPV4);
            ethReply.setVlanID(packet.getVlanID());

            // IP Packet
            IPv4 ipv4Packet = (IPv4) packet.getPayload();
            IPv4 ipv4Reply = new IPv4();
            ipv4Reply.setSourceAddress(myIPAddress.toInt());
            ipAddress = Ip4Address.valueOf(ipOffered);
            ipv4Reply.setDestinationAddress(ipAddress.toInt());
            ipv4Reply.setTtl(packetTTL);

            // UDP Datagram.
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            UDP udpReply = new UDP();
            udpReply.setSourcePort((byte) UDP.DHCP_SERVER_PORT);
            udpReply.setDestinationPort((byte) UDP.DHCP_CLIENT_PORT);

            // DHCP Payload.
            DHCP dhcpPacket = (DHCP) udpPacket.getPayload();
            DHCP dhcpReply = new DHCP();
            dhcpReply.setOpCode(DHCP.OPCODE_REPLY);

            ipAddress = Ip4Address.valueOf(ipOffered);
            dhcpReply.setYourIPAddress(ipAddress.toInt());
            dhcpReply.setServerIPAddress(myIPAddress.toInt());

            dhcpReply.setTransactionId(dhcpPacket.getTransactionId());
            dhcpReply.setClientHardwareAddress(dhcpPacket.getClientHardwareAddress());
            dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcpReply.setHardwareAddressLength((byte) 6);

            // DHCP Options.
            DHCPOption option = new DHCPOption();
            List<DHCPOption> optionList = new ArrayList<>();

            // DHCP Message Type.
            option.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
            option.setLength((byte) 1);
            byte[] optionData = {outgoingMessageType};
            option.setData(optionData);
            optionList.add(option);

            // DHCP Server Identifier.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue());
            option.setLength((byte) 4);
            option.setData(myIPAddress.toOctets());
            optionList.add(option);

            // IP Address Lease Time.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getValue());
            option.setLength((byte) 4);
            option.setData(ByteBuffer.allocate(4).putInt(leaseTime).array());
            optionList.add(option);

            // IP Address Renewal Time.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_RenewalTime.getValue());
            option.setLength((byte) 4);
            option.setData(ByteBuffer.allocate(4).putInt(renewalTime).array());
            optionList.add(option);

            // IP Address Rebinding Time.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OPtionCode_RebindingTime.getValue());
            option.setLength((byte) 4);
            option.setData(ByteBuffer.allocate(4).putInt(rebindingTime).array());
            optionList.add(option);

            // Subnet Mask.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getValue());
            option.setLength((byte) 4);
            ipAddress = Ip4Address.valueOf(subnetMask);
            option.setData(ipAddress.toOctets());
            optionList.add(option);

            // Broadcast Address.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_BroadcastAddress.getValue());
            option.setLength((byte) 4);
            ipAddress = Ip4Address.valueOf(broadcastAddress);
            option.setData(ipAddress.toOctets());
            optionList.add(option);

            // Router Address.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_RouterAddress.getValue());
            option.setLength((byte) 4);
            option.setData(myIPAddress.toOctets());
            optionList.add(option);

            // DNS Server Address.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_DomainServer.getValue());
            option.setLength((byte) 4);
            option.setData(myIPAddress.toOctets());
            optionList.add(option);

            // End Option.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());
            option.setLength((byte) 1);
            optionList.add(option);

            dhcpReply.setOptions(optionList);

            udpReply.setPayload(dhcpReply);
            ipv4Reply.setPayload(udpReply);
            ethReply.setPayload(ipv4Reply);

            return ethReply;
        }

        /**
         * Sends the Ethernet reply frame via the Packet Service.
         *
         * @param context the context of the incoming frame
         * @param reply the Ethernet reply frame
         */
        private void sendReply(PacketContext context, Ethernet reply) {
            if (reply != null) {
                TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                ConnectPoint sourcePoint = context.inPacket().receivedFrom();
                builder.setOutput(sourcePoint.port());

                packetService.emit(new DefaultOutboundPacket(sourcePoint.deviceId(),
                                    builder.build(), ByteBuffer.wrap(reply.serialize())));
            }
        }

        /**
         * Processes the DHCP Payload and initiates a reply to the client.
         *
         * @param context context of the incoming message
         * @param dhcpPayload the extracted DHCP payload
         */
        private void processDHCPPacket(PacketContext context, DHCP dhcpPayload) {

            Ethernet packet = context.inPacket().parsed();
            boolean flagIfRequestedIP = false;
            boolean flagIfServerIP = false;
            Ip4Address requestedIP = Ip4Address.valueOf("0.0.0.0");
            Ip4Address serverIP = Ip4Address.valueOf("0.0.0.0");

            if (dhcpPayload != null) {

                // TODO Convert this to enum value.
                byte incomingPacketType = 0;
                for (DHCPOption option : dhcpPayload.getOptions()) {
                    if (option.getCode() == DHCP.DHCPOptionCode.OptionCode_MessageType.getValue()) {
                        byte[] data = option.getData();
                        incomingPacketType = data[0];
                    }
                    if (option.getCode() == DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue()) {
                        byte[] data = option.getData();
                        requestedIP = Ip4Address.valueOf(data);
                        flagIfRequestedIP = true;
                    }
                    if (option.getCode() == DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue()) {
                        byte[] data = option.getData();
                        serverIP = Ip4Address.valueOf(data);
                        flagIfServerIP = true;
                    }
                }

                String ipOffered = "";
                DHCP.DHCPMessageType outgoingPacketType;
                MacAddress clientMAC = new MacAddress(dhcpPayload.getClientHardwareAddress());

                if (incomingPacketType == DHCP.DHCPMessageType.MessageType_Discover.getValue()) {

                    outgoingPacketType = DHCP.DHCPMessageType.MessageType_Offer;
                    ipOffered = dhcpStore.suggestIP(clientMAC, requestedIP).toString();

                    Ethernet ethReply = buildReply(packet, ipOffered, outgoingPacketType.getValue());
                    sendReply(context, ethReply);

                } else if (incomingPacketType == DHCP.DHCPMessageType.MessageType_Request.getValue()) {

                    outgoingPacketType = DHCP.DHCPMessageType.MessageType_ACK;

                    if (flagIfServerIP && flagIfRequestedIP) {
                        // SELECTING state
                        if (MY_IP.equals(serverIP.toString()) &&
                                dhcpStore.assignIP(clientMAC, requestedIP, leaseTime)) {

                            Ethernet ethReply = buildReply(packet, requestedIP.toString(),
                                    outgoingPacketType.getValue());
                            sendReply(context, ethReply);
                        }
                    } else if (flagIfRequestedIP) {
                        // INIT-REBOOT state
                        if (dhcpStore.assignIP(clientMAC, requestedIP, leaseTime)) {
                            Ethernet ethReply = buildReply(packet, requestedIP.toString(),
                                    outgoingPacketType.getValue());
                            sendReply(context, ethReply);
                        }
                    } else {
                        // RENEWING and REBINDING state
                        int ciaadr = dhcpPayload.getClientIPAddress();
                        if (ciaadr != 0) {
                            Ip4Address clientIaddr = Ip4Address.valueOf(ciaadr);
                            if (dhcpStore.assignIP(clientMAC, clientIaddr, leaseTime)) {
                                Ethernet ethReply = buildReply(packet, clientIaddr.toString(),
                                        outgoingPacketType.getValue());
                                sendReply(context, ethReply);
                            }
                        }
                    }
                } else if (incomingPacketType == DHCP.DHCPMessageType.MessageType_Release.getValue()) {

                    dhcpStore.releaseIP(clientMAC);
                }
            }
        }

        @Override
        public void process(PacketContext context) {

            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            Ethernet packet = context.inPacket().parsed();
            if (packet == null) {
                return;
            }

            if (packet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();

                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();

                    if (udpPacket.getDestinationPort() == UDP.DHCP_SERVER_PORT &&
                            udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT) {
                        // This is meant for the dhcp server so process the packet here.

                        DHCP dhcpPayload = (DHCP) udpPacket.getPayload();
                        processDHCPPacket(context, dhcpPayload);
                    }
                }
            }
        }
    }
}