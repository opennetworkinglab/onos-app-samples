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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

/**
 * DHCPStore Interface.
 */
public interface DHCPStore {

    /**
     * Returns an IP Address for a Mac ID, in response to a DHCP DISCOVER message.
     *
     * @param macID Mac ID of the client requesting an IP
     * @return IP address assigned to the Mac ID
     */
    Ip4Address suggestIP(MacAddress macID);

    /**
     * Assigns the requested IP to the Mac ID, in response to a DHCP REQUEST message.
     *
     * @param macID Mac Id of the client requesting an IP
     * @param ipAddr IP Address being requested
     * @param leaseTime Lease time offered by the server for this mapping
     * @return returns true if the assignment was successful, false otherwise
     */
    boolean assignIP(MacAddress macID, Ip4Address ipAddr, long leaseTime);

    /**
     * Sets the default time for which suggested IP mappings are valid.
     *
     * @param timeInSeconds default time for IP mappings to be valid
     */
    void setDefaultTimeoutForPurge(long timeInSeconds);

    /**
     * Releases the IP assigned to a Mac ID into the free pool.
     *
     * @param macID the macID for which the mapping needs to be changed
     */
    void releaseIP(MacAddress macID);

}
