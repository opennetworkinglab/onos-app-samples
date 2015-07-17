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
package org.onosproject.dhcpserver;

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
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
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

    private static long timeoutForPendingAssignments = 60;

    private static final Ip4Address START_IP = Ip4Address.valueOf("10.1.0.100");

    private static final Ip4Address END_IP = Ip4Address.valueOf("10.1.0.200");

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
            return assignmentInfo.getIpAddress();
        } else {
            Ip4Address nextIPAddr = fetchNextIP();

            assignmentInfo = new IPAssignment(nextIPAddr,
                                            timeoutForPendingAssignments,
                                            IPAssignment.AssignmentStatus.Option_Requested);

            allocationMap.put(macID, assignmentInfo);
            return nextIPAddr;
        }
    }

    @Override
    public boolean assignIP(MacAddress macID, Ip4Address ipAddr, long leaseTime) {

        IPAssignment assignmentInfo;
        if (allocationMap.containsKey(macID)) {
            assignmentInfo = allocationMap.get(macID).value();
            if (assignmentInfo.getIpAddress().toInt() == ipAddr.toInt()) {
                assignmentInfo.setAssignmentStatus(IPAssignment.AssignmentStatus.Option_Assigned);
                assignmentInfo.setTimestamp(new Date());
                assignmentInfo.setLeasePeriodinSeconds(leaseTime);
                allocationMap.put(macID, assignmentInfo);
                return true;
            }
        } else if (freeIPPool.contains(ipAddr)) {
            assignmentInfo = new IPAssignment(ipAddr, leaseTime, IPAssignment.AssignmentStatus.Option_Assigned);
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
            Ip4Address freeIP = allocationMap.get(macID).value().getIpAddress();
            allocationMap.remove(macID);
            freeIPPool.add(freeIP);
        }
    }

    @Override
    public void setDefaultTimeoutForPurge(long timeInSeconds) {
        timeoutForPendingAssignments = timeInSeconds;
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
                long timeLapsed = dateNow.getTime() - ipAssignment.getTimestamp().getTime();
                if (timeLapsed > (ipAssignment.getLeasePeriod())) {
                    Ip4Address freeIP = ipAssignment.getIpAddress();
                    allocationMap.remove(entry.getKey());
                    freeIPPool.add(freeIP);
                }
            }
            timeout = Timer.getTimer().newTimeout(new PurgeListTask(), INITIAL_DELAY, TimeUnit.MINUTES);
        }

    }

}
