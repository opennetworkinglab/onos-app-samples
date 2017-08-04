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

import java.util.List;

import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onosproject.ipfix.packet.DataRecord;
import org.onosproject.ipfix.packet.HeaderException;
import org.onosproject.ipfix.packet.Ie;
import org.onosproject.ipfix.packet.InformationElement;
import org.onosproject.ipfix.packet.TemplateRecord;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * IPFIX Data record for switch port receive statistics.
 */
public class DataRecordPortStatsIn extends DataRecord {
    public static final int TEMPLATE_ID = 341;
    public static final int FIELD_COUNT = 7;
    public static final int LENGTH = 56;

    private IpAddress exporterIPv4Address;
    private Ip6Address exporterIPv6Address;
    private int ingressInterface;
    private long octetDeltaCount;
    private long packetDeltaCount;
    private long flowStartMilliseconds;
    private long flowEndMilliseconds;

    /**
     * IPFIX Data record for switch port receive statistics.
     *
     * @param exporterIpv4 IPv4 address of the IPFIX exporter
     * @param exporterIpv6 IPv6 address of the IPFIX exporter, used for DPID
     * @param intf Ingress/receiving interface number
     * @param octets number of bytes received
     * @param packets number of packets received
     * @param start start timestamp of the flow
     * @param end end timestamp of the flow
     */
    public DataRecordPortStatsIn(IpAddress exporterIpv4, Ip6Address exporterIpv6,
            int intf, long octets, long packets, long start, long end) {

        exporterIPv4Address = exporterIpv4;
        exporterIPv6Address = exporterIpv6;
        ingressInterface = intf;
        octetDeltaCount = octets;
        packetDeltaCount = packets;
        flowStartMilliseconds = start;
        flowEndMilliseconds = end;
    }
    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public byte[] getBytes() throws HeaderException {
        try {
            byte[] data = new byte[LENGTH];

            System.arraycopy(exporterIPv4Address.toOctets(), 0, data, 0, 4);
            System.arraycopy(exporterIPv6Address.toOctets(), 0, data, 4, 16);
            System.arraycopy(Ints.toByteArray(ingressInterface), 0, data, 20, 4);
            System.arraycopy(Longs.toByteArray(octetDeltaCount), 0, data, 24, 8);
            System.arraycopy(Longs.toByteArray(packetDeltaCount), 0, data, 32, 8);
            System.arraycopy(Longs.toByteArray(flowStartMilliseconds), 0, data, 40, 8);
            System.arraycopy(Longs.toByteArray(flowEndMilliseconds), 0, data, 48, 8);

            return data;
        } catch (Exception e) {
            throw new HeaderException("Error while generating the bytes: " + e.getMessage());
        }
    }

    /**
     * IPFIX Template Record for switch port receive statistics.
     *
     * @return TemplateRecord IPFIX Template Record
     */
    public static TemplateRecord portStatsInTemplateRecord() {

        TemplateRecord tr = new TemplateRecord();
        tr.setTemplateID(TEMPLATE_ID);
        tr.setFieldCount(FIELD_COUNT);

        List<InformationElement> ieTemp = tr.getInformationElements();

        ieTemp.add(new InformationElement(Ie.exporterIPv4Address));
        ieTemp.add(new InformationElement(Ie.exporterIPv6Address));
        ieTemp.add(new InformationElement(Ie.ingressInterface));
        ieTemp.add(new InformationElement(Ie.octetDeltaCount));
        ieTemp.add(new InformationElement(Ie.packetDeltaCount));
        ieTemp.add(new InformationElement(Ie.flowStartMilliseconds));
        ieTemp.add(new InformationElement(Ie.flowEndMilliseconds));

        return tr;
    }
}
