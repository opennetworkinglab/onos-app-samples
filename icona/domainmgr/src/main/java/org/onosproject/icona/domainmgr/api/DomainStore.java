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

package org.onosproject.icona.domainmgr.api;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.store.Store;

import java.util.Map;
import java.util.Set;

/**
 * Domain store interface.
 */
public interface DomainStore extends Store<DomainEvent, DomainStoreDelegate> {

    /**
     * Returns the set of domains that have an associated topology.
     * @return set of domain identifiers
     */
    Set<DomainId> getDomainIds();

    /**
     * Returns the set of the devices of the specified domain.
     * @param domainId domain identifier
     * @return set of device objects
     */
    Set<DeviceId> getDeviceIds(DomainId domainId);

    //TODO: get the topology of the topologies of all domain

    /**
     * Adds a new domain ID to the store.
     * @param domainId domain id
     */
    void addDomain(DomainId domainId);

    /**
     * Removes the specified domain ID from the store.
     * @param domainId domain identifier
     */
    void removeDomain(DomainId domainId);

    /**
     * Adds a new device to the store.
     * @param domainId domain ientifier
     * @param deviceId device identifier
     */
    void addDevice(DomainId domainId, DeviceId deviceId);

    /**
     * Removes a device from the store.
     * @param domainId domain identifier
     * @param deviceId device identifier
     */
    void removeDevice(DomainId domainId, DeviceId deviceId);

    /**
     * Gets the set of host identifiers of the specified domain.
     * @param domainId domain identifier
     * @return set of host identifiers
     */
    Set<HostId> getHostIds(DomainId domainId);

    /**
     * Adds a host to the domain store.
     * @param domainId domain identifier
     * @param hostId host identifier
     */
    void addHost(DomainId domainId, HostId hostId);

    /**
     * Removes a host from a domain.
     * @param domainId domain identifier
     * @param hostId host identifier
     */
    void removeHost(DomainId domainId, HostId hostId);

    /**
     * Gets the set of interlinks having source port within the specified domains.
     * @param endDomains end domains identifier
     * @return set of links
     */
    Set<Link> getInterLinks(Pair<DomainId, DomainId> endDomains);

    /**
     * Adds an interlink to the store. Link source port is within the specified domain.
     * @param endDomains end domains identifier
     * @param link link object
     */
    void addOrUpdateInterLink(Pair<DomainId, DomainId> endDomains, Link link);

    /**
     * Removes an interlink from the store.
     * @param endDomains end domain identifiers
     * @param link link object
     */
    void removeInterLink(Pair<DomainId, DomainId> endDomains, Link link);

    /**
     * Returns the mapping between the port numbers
     * of the specified device exposed to a peer and the physical ports.
     * @param deviceId device identifier of the local virtual device exposed to a peer
     * @return the mapping between the ports seen by the other domain and a local host
     */
    Map<PortNumber, ConnectPoint> getVirtualPortToPortMapping(DeviceId deviceId);

    /**
     * Adds a new mapping between virtual and physical ports
     * exposed to other domains.
     * @param deviceId identifier of the domain device
     */
    void setVirtualPortToPortMapping(DeviceId deviceId, Map<PortNumber, ConnectPoint> virtualPortToPortMap);
}
