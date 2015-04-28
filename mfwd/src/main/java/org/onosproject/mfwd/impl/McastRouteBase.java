/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.mfwd.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;
import java.util.HashSet;

/**
 * McastRouteBase base class for McastRouteGroup and McastRouteSource.
 */
public class McastRouteBase implements McastRoute {
    protected final IpPrefix gaddr;
    protected final IpPrefix saddr;

    protected ConnectPoint ingressPoint;
    protected Set<ConnectPoint> egressPoints;

    protected boolean isGroup = false;

    /**
     * Create a multicast route. This is the parent class for both the Group
     * and the source.
     *
     * @param saddr source address
     * @param gaddr multicast group address
     */
    public McastRouteBase(String saddr, String gaddr) {
        this.gaddr = IpPrefix.valueOf(checkNotNull(gaddr));
        if (saddr == null || saddr.equals("*")) {
            this.saddr = IpPrefix.valueOf(0, 0);
        } else {
            this.saddr = IpPrefix.valueOf(checkNotNull(gaddr));
        }

        this.init();
    }

    /**
     * Create a multicast group table entry.
     * @param gaddr multicast group address
     */
    public McastRouteBase(String gaddr) {
        this("*", gaddr);
    }

    /**
     * Set the source and group address value of a (*, G) group.
     * @param gpfx the group prefix address
     */
    public McastRouteBase(IpPrefix gpfx) {
        this(IpPrefix.valueOf(0, 0), gpfx);
    }

    /**
     * Create a multicast route constructor.
     * @param saddr source address
     * @param gaddr group address
     */
    public McastRouteBase(IpPrefix saddr, IpPrefix gaddr) {
        this.saddr = checkNotNull(saddr);
        this.gaddr = checkNotNull(gaddr);

        this.init();
    }

    private void init() {
        this.isGroup = (this.saddr.prefixLength() == 0);
        this.ingressPoint = null;
        this.egressPoints = new HashSet();
    }

    /**
     * Get the multicast group address.
     * @return the multicast group address
     */
    public IpPrefix getGaddr() {
        return gaddr;
    }

    /**
     * Get the multicast source address.
     * @return the multicast source address
     */
    public IpPrefix getSaddr() {
        return saddr;
    }

    /**
     * Is this an IPv4 multicast route.
     * @return true if it is an IPv4 route
     */
    public boolean isIp4() {
        return gaddr.isIp4();
    }

    /**
     * Is this an IPv6 multicast route.
     * @return true if it is an IPv6 route
     */
    public boolean isIp6() {
        return gaddr.isIp6();
    }

    /**
     * Is this a multicast group route?
     * @return true if it is a multicast group route.
     */
    public boolean isGroup() {
        return isGroup;
    }

    /**
     * @return true if this is (S, G) false if it (*, G).
     */
    public boolean isSource() {
        return (!isGroup);
    }

    /**
     * Add an ingress point to this route.
     *
     * @param ingress incoming connect point
     */
    public void addIngressPoint(ConnectPoint ingress) {
        ingressPoint = checkNotNull(ingress);
    }

    /**
     * Add or modify the ingress connect point.
     * @param deviceId the switch device Id
     * @param portNum the ingress port number
     */
    public void addIngressPoint(String deviceId, long portNum) {
        ingressPoint = new ConnectPoint(
                DeviceId.deviceId(deviceId),
                PortNumber.portNumber(portNum));
    }

    /**
     * Get the ingress ConnectPoint.
     * @return the ingress ConnectPoint
     */
    public ConnectPoint getIngressPoint() {
        return this.ingressPoint;
    }

    /**
     * Add an egress ConnectPoint.
     *
     * @param member member egress connect point
     */
    public void addEgressPoint(ConnectPoint member) {
        egressPoints.add(checkNotNull(member));
    }

    /**
     * Add an egress ConnectPoint.
     *
     * @param deviceId deviceId of the connect point
     * @param portNum portNum of the connect point
     */
    public void addEgressPoint(String deviceId, long portNum) {
        ConnectPoint cp = new ConnectPoint(DeviceId.deviceId(deviceId), PortNumber.portNumber(portNum));
        this.egressPoints.add(cp);
    }

    /**
     * Get egress connect points for the route.
     * @return Set of egress connect points
     */
    public Set<ConnectPoint> getEgressPoints() {
        return egressPoints;
    }

    /**
     * Pretty Print this Multicast Route.  Works for McastRouteSource and McastRouteGroup.
     * @return pretty string of the multicast route
     */
    public String toString() {
        String out = String.format("(%s, %s) ingress: %s ",
                saddr.toString(), gaddr.toString(),
                (ingressPoint == null) ? "NULL" : ingressPoint.toString());
        out += "egress: {";
        if (egressPoints != null && !egressPoints.isEmpty()) {
            for (ConnectPoint eg : egressPoints) {
                out += " " + eg.toString();
            }
        }
        out += ("}\n");
        return out;
    }
}