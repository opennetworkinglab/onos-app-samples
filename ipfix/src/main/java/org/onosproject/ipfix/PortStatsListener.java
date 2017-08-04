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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onosproject.ipfix.packet.DataRecord;
import org.onosproject.ipfix.packet.TemplateRecord;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.openflow.controller.Dpid;

import com.google.common.primitives.Longs;

/**
 * Internal PortStats Listener.
 * Exports IPFIX statistics when PortStats are updated.
 */
public class PortStatsListener implements DeviceListener {

    public Map<ConnectPoint, List<Long>> prevStatsMap = new HashMap<ConnectPoint, List<Long>>();

    private IpfixManager ipfixManager;

    /**
     * Internal PortStats Listener.
     * Exports IPFIX statistics when PortStats are updated.
     *
     * @param ipfixManager ipfix manager instance
     */
    public PortStatsListener(IpfixManager ipfixManager) {
        this.ipfixManager = ipfixManager;
    }

    @Override
    public void event(DeviceEvent event) {

        Device device = event.subject();

        switch (event.type()) {
            case PORT_STATS_UPDATED:
                if (ipfixManager.deviceService.getRole(device.id()) == MastershipRole.MASTER) {
                    ipfixManager.log.trace("PortStats Updated: I am MASTER for deviceId={}", device.id().toString());

                    List<DataRecord> recordsInList = new ArrayList<DataRecord>();
                    List<DataRecord> recordsOutList = new ArrayList<DataRecord>();

                    IpAddress exporterIpv4 =
                            IpAddress.valueOf(device.annotations().toString().split("=")[2].split(":")[0]);
                    long dpid = Dpid.dpid(device.id().uri()).value();
                    byte[] byteExporterIpv6 = new byte[16];
                    System.arraycopy(Longs.toByteArray(0), 0, byteExporterIpv6, 0, 8);
                    System.arraycopy(Longs.toByteArray(dpid), 0, byteExporterIpv6, 8, 8);
                    Ip6Address exporterIpv6 = Ip6Address.valueOf(byteExporterIpv6);

                    for (PortStatistics stat : ipfixManager.deviceService.getPortDeltaStatistics(device.id())) {

                        final String format = "PortStatsListener Delta Stats: port={}, pktRx={}, pktTx={},"
                                + "bytesRx={}, bytesTx={}, pktRxDrp={}, pktTxDrp={}, Dur={}.{}";
                        ipfixManager.log.trace(format, stat.port(), stat.packetsReceived(), stat.packetsSent(),
                                stat.bytesReceived(), stat.bytesSent(),
                                stat.packetsRxDropped(), stat.packetsTxDropped(),
                                stat.durationSec(), (stat.durationNano() / 1000000));

                        long inBytes = stat.bytesReceived();
                        long inPackets = stat.packetsReceived();
                        long outBytes = stat.bytesSent();
                        long outPackets = stat.packetsSent();
                        long endTime = System.currentTimeMillis();
                        long startTime = endTime - (stat.durationSec() * TimeUnit.SECONDS.toMillis(1) +
                                stat.durationNano() / TimeUnit.MILLISECONDS.toNanos(1));

                        DataRecordPortStatsIn recordIn = new DataRecordPortStatsIn(exporterIpv4, exporterIpv6,
                                stat.port(), inBytes, inPackets, startTime, endTime);
                        DataRecordPortStatsOut recordOut = new DataRecordPortStatsOut(exporterIpv4, exporterIpv6,
                                stat.port(), outBytes, outPackets, startTime, endTime);

                        recordsInList.add(recordIn);
                        recordsOutList.add(recordOut);
                    }

                    if (recordsInList.isEmpty() && recordsOutList.isEmpty()) {
                        ipfixManager.log.trace("PortStats: Previous PortStats for device={} where zero,"
                                + "not sending IPFIX flow", device.id());
                    } else {
                        TemplateRecord trIn = DataRecordPortStatsIn.portStatsInTemplateRecord();
                        TemplateRecord trOut = DataRecordPortStatsOut.portStatsOutTemplateRecord();
                        ipfixManager.ipfixSender.sendRecords(trIn, recordsInList, dpid,
                                IpfixManager.collectorIp, IpfixManager.collectorPort);
                        ipfixManager.ipfixSender.sendRecords(trOut, recordsOutList, dpid,
                                IpfixManager.collectorIp, IpfixManager.collectorPort);
                    }
                }
                break;

            default:
                break;
        }
    }
}
