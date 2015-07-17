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

import java.util.Date;

/**
 * Stores the MAC ID to IP Address mapping details.
 */

// TODO Convert this into an immutable class using the builder pattern.
public class IPAssignment {

    private Ip4Address ipAddress;

    private Date timestamp;

    private long leasePeriod;

    private AssignmentStatus assignmentStatus;

    public enum AssignmentStatus {
        Option_Requested,
        Option_Assigned,
        Option_Expired;
    }

    /**
     * Default constructor for IPAssignment, where the timestamp is set to the current time.
     */
    IPAssignment() {
        timestamp = new Date();
    }

    /**
     * Constructor for IPAssignment, where the ipAddress, the lease period
     * and assignment status is supplied. The timestamp is set to the current time.
     *
     * @param ipAddress
     * @param leasePeriod
     * @param assignmentStatus
     */
    IPAssignment(Ip4Address ipAddress, long leasePeriod, AssignmentStatus assignmentStatus) {
        this.ipAddress = ipAddress;
        this.leasePeriod = leasePeriod;
        this.assignmentStatus = assignmentStatus;

        this.timestamp = new Date();
    }

    /**
     * Sets the IP address for the IP assignment.
     *
     * @param ipaddress the assigned IP address
     */
    public void setIpAddress(Ip4Address ipaddress) {
        this.ipAddress = ipaddress;
    }

    /**
     * Sets the Timestamp for the IP assignment when the assignment was made.
     *
     * @param timestamp timestamp when the assignment was made
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the assignment status for the IP assignment.
     *
     * @param assignmentStatus the assignment status
     */
    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    /**
     * Sets the Lease period value when the argument supplied is in seconds.
     *
     * @param leasePeriod lease time in seconds
     */
    public void setLeasePeriodinSeconds(long leasePeriod) {
        this.leasePeriod = leasePeriod * 1000;
    }

    /**
     * Sets the Lease period value when the argument supplied is in milliseconds.
     *
     * @param leasePeriod lease time in milliseconds
     */
    public void setLeasePeriodinMilliseconds(long leasePeriod) {
        this.leasePeriod = leasePeriod;
    }

    /**
     * Returns the IP Address of the IP assignment.
     *
     * @return the IP address
     */
    public Ip4Address getIpAddress() {
        return this.ipAddress;
    }

    /**
     * Returns the timestamp of the IP assignment.
     *
     * @return the timestamp
     */
    public Date getTimestamp() {
        return this.timestamp;
    }

    /**
     * Returns the assignment status of the IP assignment.
     *
     * @return the assignment status
     */
    public AssignmentStatus getAssignmentStatus() {
        return this.assignmentStatus;
    }

    /**
     * Returns the lease period of the IP assignment.
     *
     * @return the lease period
     */
    public long getLeasePeriod() {
        return this.leasePeriod;
    }
}
