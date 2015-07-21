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

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Timer;
import org.onosproject.dhcpserver.DHCPStore;
import org.onosproject.dhcpserver.IPAssignment;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the pool of available IP Addresses in the network and
 * Remembers the mapping between MAC ID and IP Addresses assigned.
 */

@Component(immediate = true)
@Service
public class DistributedDHCPStore implements DHCPStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<MacAddress, IPAssignment> allocationMap;

    private DistributedSet<Ip4Address> freeIPPool;

    private Timeout timeout;

    // TODO Make the hardcoded values configurable.

    private static final int INITIAL_DELAY = 2;

    private static int timeoutForPendingAssignments = 60;

    private static final Ip4Address START_IP = Ip4Address.valueOf("10.1.0.140");

    private static final Ip4Address END_IP = Ip4Address.valueOf("10.1.0.160");

    @Activate
    protected void activate() {
        allocationMap = storageService.<MacAddress, IPAssignment>consistentMapBuilder()
                .withName("onos-dhcp-assignedIP")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(IPAssignment.class,
                                        IPAssignment.AssignmentStatus.class,
                                        Date.class,
                                        long.class,
                                        Ip4Address.class)
                                .build()))
                .build();

        freeIPPool = storageService.<Ip4Address>setBuilder()
                .withName("onos-dhcp-freeIP")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

        populateIPPoolfromRange(START_IP, END_IP);
        timeout = Timer.getTimer().newTimeout(new PurgeListTask(), INITIAL_DELAY, TimeUnit.MINUTES);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        timeout.cancel();
        log.info("Stopped");
    }

    @Override
    public Ip4Address suggestIP(MacAddress macID) {

        IPAssignment assignmentInfo;
        if (allocationMap.containsKey(macID)) {
            assignmentInfo = allocationMap.get(macID).value();
            return assignmentInfo.ipAddress();
        } else {
            Ip4Address nextIPAddr = fetchNextIP();


            assignmentInfo = IPAssignment.builder()
                                        .ipAddress(nextIPAddr)
                                        .timestamp(new Date())
                                        .leasePeriod(timeoutForPendingAssignments)
                                        .assignmentStatus(IPAssignment.AssignmentStatus.Option_Requested)
                                        .build();

            allocationMap.put(macID, assignmentInfo);
            return nextIPAddr;
        }
    }

    @Override
    public boolean assignIP(MacAddress macID, Ip4Address ipAddr, int leaseTime) {

        IPAssignment assignmentInfo;
        if (allocationMap.containsKey(macID)) {
            assignmentInfo = allocationMap.get(macID).value();
            if (assignmentInfo.ipAddress().toInt() == ipAddr.toInt()) {

                assignmentInfo = IPAssignment.builder()
                                    .ipAddress(ipAddr)
                                    .timestamp(new Date())
                                    .leasePeriod(leaseTime)
                                    .assignmentStatus(IPAssignment.AssignmentStatus.Option_Assigned)
                                    .build();
                allocationMap.put(macID, assignmentInfo);
                return true;
            }
        } else if (freeIPPool.contains(ipAddr)) {
            assignmentInfo = IPAssignment.builder()
                                    .ipAddress(ipAddr)
                                    .timestamp(new Date())
                                    .leasePeriod(leaseTime)
                                    .assignmentStatus(IPAssignment.AssignmentStatus.Option_Assigned)
                                    .build();
            if (freeIPPool.remove(ipAddr)) {
                allocationMap.put(macID, assignmentInfo);
                return true;
            }
        }
        return false;
    }

    @Override
    public void releaseIP(MacAddress macID) {
        if (allocationMap.containsKey(macID)) {
            Ip4Address freeIP = allocationMap.get(macID).value().ipAddress();
            allocationMap.remove(macID);
            freeIPPool.add(freeIP);
        }
    }

    @Override
    public void setDefaultTimeoutForPurge(int timeInSeconds) {
        timeoutForPendingAssignments = timeInSeconds;
    }

    @Override
    public Map<MacAddress, Ip4Address> listMapping() {

        Map<MacAddress, Ip4Address> allMapping = new HashMap<>();
        for (Map.Entry<MacAddress, Versioned<IPAssignment>> entry: allocationMap.entrySet()) {
            IPAssignment assignment = entry.getValue().value();
            if (assignment.assignmentStatus() == IPAssignment.AssignmentStatus.Option_Assigned) {
                allMapping.put(entry.getKey(), assignment.ipAddress());
            }
        }

        return allMapping;
    }

    @Override
    public boolean assignStaticIP(MacAddress macID, Ip4Address ipAddr) {
        return assignIP(macID, ipAddr, -1);
    }

    @Override
    public boolean removeStaticIP(MacAddress macID) {
        if (allocationMap.containsKey(macID)) {
            IPAssignment assignment = allocationMap.get(macID).value();
            Ip4Address freeIP = assignment.ipAddress();
            if (assignment.leasePeriod() < 0) {
                allocationMap.remove(macID);
                freeIPPool.add(freeIP);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<Ip4Address> getAvailableIPs() {
        return ImmutableSet.<Ip4Address>copyOf(freeIPPool);
    }

    /**
     * Appends all the IPs in a given range to the free pool of IPs.
     *
     * @param startIP Start IP for the range
     * @param endIP End IP for the range
     */
    public void populateIPPoolfromRange(Ip4Address startIP, Ip4Address endIP) {
        int lastIP = endIP.toInt();
        Ip4Address nextIP;
        for (int loopCounter = startIP.toInt(); loopCounter <= lastIP; loopCounter++) {
            nextIP = Ip4Address.valueOf(loopCounter);
            freeIPPool.add(nextIP);
        }
    }

    /**
     * Fetches the next available IP from the free pool pf IPs.
     *
     * @return the next available IP address
     */
    private Ip4Address fetchNextIP() {
        for (Ip4Address freeIP : freeIPPool) {
            if (freeIPPool.remove(freeIP)) {
                return freeIP;
            }
        }
        return null;
    }

    /**
     * Purges the IP allocation map to remove expired entries and returns the freed IPs to the free pool.
     */
    private class PurgeListTask implements TimerTask {

        @Override
        public void run(Timeout to) {
            IPAssignment ipAssignment;
            Date dateNow = new Date();
            for (Map.Entry<MacAddress, Versioned<IPAssignment>> entry: allocationMap.entrySet()) {
                ipAssignment = entry.getValue().value();
                long timeLapsed = dateNow.getTime() - ipAssignment.timestamp().getTime();
                if ((ipAssignment.leasePeriod() > 0) && (timeLapsed > (ipAssignment.leasePeriod()))) {
                    Ip4Address freeIP = ipAssignment.ipAddress();
                    allocationMap.remove(entry.getKey());
                    freeIPPool.add(freeIP);
                }
            }
            timeout = Timer.getTimer().newTimeout(new PurgeListTask(), INITIAL_DELAY, TimeUnit.MINUTES);
        }

    }

}
