/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.icona.domainprovider.api.host;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.IpAddress;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Host;
import org.onosproject.net.host.DefaultHostDescription;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of {@link DomainHostDescription}.
 */
public class DefaultDomainHostDescription extends DefaultHostDescription
        implements DomainHostDescription {

    private final DomainId domainId;
    private final Set<Ip4Prefix> ip4Subnets;
    private final Set<Ip6Prefix> ip6Subnets;

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param domainId    domain identifier
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ips         host IP addresses
     * @param ip4Subnets  ipv4 subnets
     * @param annotations optional key/value annotations
     */
    public DefaultDomainHostDescription(DomainId domainId,
                                        MacAddress mac,
                                        VlanId vlan,
                                        HostLocation location,
                                        Set<IpAddress> ips,
                                        Set<Ip4Prefix> ip4Subnets,
                                        SparseAnnotations... annotations) {
        super(mac, vlan, location, ips, annotations);

        this.domainId = domainId;
        this.ip4Subnets = ip4Subnets;
        this.ip6Subnets = Sets.newHashSet();
    }

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param domainId    domain identifier
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ips         host IP addresses
     * @param ip4Subnets  ipv4 subnets
     * @param ip6Subnets  ip6 subnets
     * @param annotations optional key/value annotations
     */
    public DefaultDomainHostDescription(DomainId domainId,
                                        MacAddress mac,
                                        VlanId vlan,
                                        HostLocation location,
                                        Set<IpAddress> ips,
                                        Set<Ip4Prefix> ip4Subnets,
                                        Set<Ip6Prefix> ip6Subnets,
                                        SparseAnnotations... annotations) {
        super(mac, vlan, location, ips, annotations);

        this.domainId = domainId;
        this.ip4Subnets = ip4Subnets;
        this.ip6Subnets = ip6Subnets;
    }

    @Override
    public DomainId domainId() {
        return domainId;
    }

    @Override
    public Set<Ip4Prefix> ip4Subnets() {
        return ImmutableSet.copyOf(ip4Subnets);
    }

    @Override
    public Set<Ip6Prefix> ip6Subnets() {
        return ImmutableSet.copyOf(ip6Subnets);

    }

    @Override
    public boolean isHost(Host host) {
        return Objects.equals(this.hwAddress(), host.mac());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), domainId, ip4Subnets, ip6Subnets);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultDomainHostDescription) {
            final DefaultDomainHostDescription other = (DefaultDomainHostDescription) obj;
            return  Objects.equals(this.hwAddress(), other.hwAddress()) &&
                    Objects.equals(this.ipAddress(), other.ipAddress()) &&
                    Objects.equals(this.location(), other.location()) &&
                    Objects.equals(this.domainId, other.domainId) &&
                    Objects.equals(this.ip4Subnets, other.ip4Subnets()) &&
                    Objects.equals(this.ip6Subnets, other.ip6Subnets());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mac", hwAddress())
                .add("vlan", vlan())
                .add("location", location())
                .add("ipAdresses", ipAddress())
                .add("ip4Subnets", ip4Subnets)
                .add("ip6Subnets", ip6Subnets)
                .add("domain", domainId)
                .add("annotations", annotations())
                .toString();
    }
}
