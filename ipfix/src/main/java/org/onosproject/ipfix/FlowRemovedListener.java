/*
 * Copyright 2015 Open Networking Foundation
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
package org.onosproject.ipfix;

import java.util.ArrayList;
import java.util.List;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.ipfix.packet.DataRecord;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.openflow.controller.Dpid;

import com.google.common.primitives.Longs;

/**
 * Flow Rule Listener for detecting flow removal or flow statistics update by ONOS.
 */
public class FlowRemovedListener implements FlowRuleListener {

    private IpfixManager ipfixManager;

    /**
     * Flow Event listerner for Flow removed events of Reactive Forwarding application.
     *
     * @param ipfixManager ipfix manager instance
     */
    public FlowRemovedListener(IpfixManager ipfixManager) {
        this.ipfixManager = ipfixManager;
    }

    @Override
    public void event(FlowRuleEvent event) {
        switch (event.type()) {
            case RULE_REMOVED:
                FlowRule rule = event.subject();
                FlowEntry entry = (FlowEntry) rule;
                if (entry.appId() == ipfixManager.coreService.getAppId("org.onosproject.fwd").id()) {
                    flowRemovedRfwd(entry);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Handle ONOS Reactive forwarding application flow removal.
     * When flow is removed, generate and send IPFIX record.
     *
     * @param entry flow entry removed from ONOS
     */
    private void flowRemovedRfwd(FlowEntry entry) {

        //Log
        ipfixManager.log.trace("Flow Removed from Reactive Forwarding, id={}, device={}, selector={}, treatment={}",
                entry.id(), entry.deviceId(), entry.selector(), entry.treatment());

        // Exporters
        IpAddress exporterIpv4 = IpAddress.valueOf(ipfixManager.deviceService.getDevice(
                entry.deviceId()).annotations().toString().split("=")[2].split(":")[0]);

        long dpid = Dpid.dpid(entry.deviceId().uri()).value();
        byte[] byteExporterIpv6 = new byte[16];
        System.arraycopy(Longs.toByteArray(0), 0, byteExporterIpv6, 0, 8);
        System.arraycopy(Longs.toByteArray(dpid), 0, byteExporterIpv6, 8, 8);
        Ip6Address exporterIpv6 = Ip6Address.valueOf(byteExporterIpv6);

        // Timestamps, octets, packets
        long start = System.currentTimeMillis() - (1000 * entry.life());
        long end = System.currentTimeMillis();
        long octets = entry.bytes();
        long packets = entry.packets();

        // Input and Output ports
        PortCriterion portCrit = (PortCriterion) entry.selector().getCriterion(Type.IN_PORT);
        int intfIn = (portCrit == null) ? 0 : (int) portCrit.port().toLong();
        List<Instruction> instructions = entry.treatment().allInstructions();
        int intfOut = 0;
        for (Instruction instruction : instructions) {
            if (instruction.type() == Instruction.Type.OUTPUT) {
                OutputInstruction outputInstruction = (OutputInstruction) instruction;
                intfOut = (outputInstruction == null) ? 0 : (int) outputInstruction.port().toLong();
            }
        }

        // Ethernet MACs, Ethertype and VLAN
        EthCriterion ethCrit;
        ethCrit = (EthCriterion) entry.selector().getCriterion(Type.ETH_SRC);
        MacAddress srcMac = (ethCrit == null) ? MacAddress.valueOf("00:00:00:00:00:00") : ethCrit.mac();
        ethCrit = (EthCriterion) entry.selector().getCriterion(Type.ETH_DST);
        MacAddress dstMac = (ethCrit == null) ? MacAddress.valueOf("00:00:00:00:00:00") : ethCrit.mac();

        EthTypeCriterion ethTypeCrit = (EthTypeCriterion) entry.selector().getCriterion(Type.ETH_TYPE);
        Short ethType = (ethTypeCrit == null) ? 0x0000 : ethTypeCrit.ethType().toShort();

        VlanIdCriterion vlanCrit = (VlanIdCriterion) entry.selector().getCriterion(Type.VLAN_VID);
        Short vlan = (vlanCrit == null) ? 0x0000 : vlanCrit.vlanId().toShort();

        // IP Criterion check
        IPCriterion srcIpCrit = (IPCriterion) entry.selector().getCriterion(Type.IPV4_SRC);
        IPCriterion dstIpCrit = (IPCriterion) entry.selector().getCriterion(Type.IPV4_DST);
        IPCriterion srcIp6Crit = (IPCriterion) entry.selector().getCriterion(Type.IPV6_SRC);
        IPCriterion dstIp6Crit = (IPCriterion) entry.selector().getCriterion(Type.IPV6_DST);

        // If IP criterions are null send MAC Data Record, else send IPv4 or IPv6 Data Record
        if (srcIpCrit == null && dstIpCrit == null && srcIp6Crit == null && dstIp6Crit == null) {
            DataRecordRfwdMac record = new DataRecordRfwdMac(
                    exporterIpv4, exporterIpv6,
                    start, end,
                    octets, packets,
                    intfIn, intfOut,
                    srcMac, dstMac,
                    ethType, vlan);
            List<DataRecord> recordList = new ArrayList<DataRecord>();
            recordList.add(record);
            ipfixManager.ipfixSender.sendRecords(DataRecordRfwdMac.getTemplateRecord(),
                    recordList, dpid, IpfixManager.collectorIp, IpfixManager.collectorPort);
        } else {
            // Checking IPv4 and IPv6 criterions
            IPProtocolCriterion protocolCrit = (IPProtocolCriterion) entry.selector().getCriterion(Type.IP_PROTO);
            byte ipProtocol = (protocolCrit == null) ? (byte) 0xff : (byte) protocolCrit.protocol();

            IPDscpCriterion dscpCrit = (IPDscpCriterion) entry.selector().getCriterion(Type.IP_DSCP);
            byte dscp = (dscpCrit == null) ? 0x00 : dscpCrit.ipDscp();
            IPEcnCriterion ecnCrit = (IPEcnCriterion) entry.selector().getCriterion(Type.IP_ECN);
            byte ecn = (ecnCrit == null) ? 0x00 : ecnCrit.ipEcn();
            byte tos = (byte) ((byte) (dscp << 2) | ecn);

            IPv6FlowLabelCriterion flowLabelCrit =
                    (IPv6FlowLabelCriterion) entry.selector().getCriterion(Type.IPV6_FLABEL);
            int flowLabelIpv6 = (flowLabelCrit == null) ? 0 : flowLabelCrit.flowLabel();

            int srcPort = 0;
            int dstPort = 0;
            if (ipProtocol == IPv4.PROTOCOL_TCP) {
                TcpPortCriterion tcpCrit;
                tcpCrit = (TcpPortCriterion) entry.selector().getCriterion(Type.TCP_SRC);
                srcPort = (tcpCrit == null) ? 0 : tcpCrit.tcpPort().toInt();
                tcpCrit = (TcpPortCriterion) entry.selector().getCriterion(Type.TCP_DST);
                dstPort = (tcpCrit == null) ? 0 : tcpCrit.tcpPort().toInt();
            } else if (ipProtocol == IPv4.PROTOCOL_UDP) {
                UdpPortCriterion udpCrit;
                udpCrit = (UdpPortCriterion) entry.selector().getCriterion(Type.UDP_SRC);
                srcPort = (udpCrit == null) ? 0 : udpCrit.udpPort().toInt();
                udpCrit = (UdpPortCriterion) entry.selector().getCriterion(Type.UDP_DST);
                dstPort = (udpCrit == null) ? 0 : udpCrit.udpPort().toInt();
            } else if (ipProtocol == IPv4.PROTOCOL_ICMP) {
                IcmpTypeCriterion icmpTypeCrit = (IcmpTypeCriterion) entry.selector().getCriterion(Type.ICMPV4_TYPE);
                Short icmpType = (icmpTypeCrit == null) ? 0 : icmpTypeCrit.icmpType();
                IcmpCodeCriterion icmpCodeCrit = (IcmpCodeCriterion) entry.selector().getCriterion(Type.ICMPV4_CODE);
                Short icmpCode = (icmpCodeCrit == null) ? 0 : icmpCodeCrit.icmpCode();
                dstPort = 256 * icmpType + icmpCode;
            } else if (ipProtocol == IPv6.PROTOCOL_ICMP6) {
                Icmpv6TypeCriterion icmpv6TypeCrit =
                        (Icmpv6TypeCriterion) entry.selector().getCriterion(Type.ICMPV6_TYPE);
                Short icmpType = (icmpv6TypeCrit == null) ? 0 : icmpv6TypeCrit.icmpv6Type();
                Icmpv6CodeCriterion icmpv6CodeCrit =
                        (Icmpv6CodeCriterion) entry.selector().getCriterion(Type.ICMPV6_CODE);
                Short icmpCode = (icmpv6CodeCrit == null) ? 0 : icmpv6CodeCrit.icmpv6Code();
                dstPort = 256 * icmpType + icmpCode;
            }
            // If IPv4 than send IPv4 Data record
            if ((srcIpCrit != null || dstIpCrit != null) && ethType == Ethernet.TYPE_IPV4) {
                IpAddress srcIp = (srcIpCrit == null) ? IpAddress.valueOf(0) : srcIpCrit.ip().address();
                IpAddress dstIp = (dstIpCrit == null) ? IpAddress.valueOf(0) : dstIpCrit.ip().address();
                DataRecordRfwdIpv4 record = new DataRecordRfwdIpv4(
                        exporterIpv4, exporterIpv6,
                        start, end,
                        octets, packets,
                        intfIn, intfOut,
                        srcMac, dstMac,
                        ethType, vlan,
                        srcIp, dstIp,
                        ipProtocol, tos,
                        (short) srcPort, (short) dstPort);
                List<DataRecord> recordList = new ArrayList<DataRecord>();
                recordList.add(record);
                ipfixManager.ipfixSender.sendRecords(DataRecordRfwdIpv4.getTemplateRecord(),
                        recordList, dpid, IpfixManager.collectorIp, IpfixManager.collectorPort);
            }
            // If IPv6 than send IPv6 Data record
            if ((srcIp6Crit != null || dstIp6Crit != null) && ethType == Ethernet.TYPE_IPV6) {
                Ip6Address srcIp6 = (srcIp6Crit == null) ?
                        Ip6Address.valueOf("0:0:0:0:0:0:0:0") : srcIp6Crit.ip().address().getIp6Address();
                Ip6Address dstIp6 = (dstIp6Crit == null) ?
                        Ip6Address.valueOf("0:0:0:0:0:0:0:0") : dstIp6Crit.ip().address().getIp6Address();
                DataRecordRfwdIpv6 record = new DataRecordRfwdIpv6(
                    exporterIpv4, exporterIpv6,
                    start, end,
                    octets, packets,
                    intfIn, intfOut,
                    srcMac, dstMac,
                    ethType, vlan,
                    srcIp6, dstIp6,
                    flowLabelIpv6,
                    ipProtocol, tos,
                    (short) srcPort, (short) dstPort);
                List<DataRecord> recordList = new ArrayList<DataRecord>();
                recordList.add(record);
                ipfixManager.ipfixSender.sendRecords(DataRecordRfwdIpv6.getTemplateRecord(),
                        recordList, dpid, IpfixManager.collectorIp, IpfixManager.collectorPort);
            }
        }
    }
}