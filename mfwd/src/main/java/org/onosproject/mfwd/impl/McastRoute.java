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

import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

/**
 * This McastRouteBase interface is implemented by the McastRouteBase class which
 * in turn acts as the base class for both the McastRouteGroup and McastRouteSource.
 */
interface McastRoute {

    /**
     * get the group addresses.
     * @return group address
     */
    public IpPrefix getGaddr();

    /**
     * get the source address.
     * @return the source address
     */
    public IpPrefix getSaddr();

    /**
     * Check for IPv4 or IPv6 as well as (S, G) or (*, G).
     */
    public boolean isIp4();

    /**
     * Add the ingress ConnectPoint with a ConnectPoint.
     */
    public void addIngressPoint(ConnectPoint ingress);

    /**
     * Add the ingress Connect Point using. ..
     * @param deviceId device ID
     * @param portNum port number
     */
    public void addIngressPoint(String deviceId, long portNum);

    /**
     * Get the ingress connect point.
     * @return the ingress connect point
     */
    public ConnectPoint getIngressPoint();

    /**
     * Add an egress connect point.
     * @param member the egress ConnectPoint to be added
     */
    public void addEgressPoint(ConnectPoint member);

    /**
     * Add an egress connect point.
     * @param deviceId the device ID of the connect point
     * @param portNum the port number of the connect point
     */
    public void addEgressPoint(String deviceId, long portNum);

    /**
     * Get the egress connect points.
     * @return a set of egress connect points
     */
    public Set<ConnectPoint> getEgressPoints();

    /**
     * Pretty print the the route.
     * @return a pretty string
     */
    public String toString();
}