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
import org.onosproject.net.Device;
import org.onosproject.net.HostId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;

import java.util.Map;
import java.util.Set;

/**
 * Service to access domain topology elements.
 */
public interface DomainService {

    /**
     * Adds a new domain id to the store.
     * @param domainId domain identifier
     */
    void registerDomainId(DomainId domainId);

    /**
     * Removes the specified domain id from the store.
     * @param domainId domain identifier
     */
    void unregisterDomainId(DomainId domainId);

    /**
     * Returns the set of domains that have an associated topology.
     *
     * @return set of domain identifiers
     */
    Set<DomainId> getDomainIds();

    /**
     * Returns the set of the devices of the specified domain.
     *
     * @param domainId domain identifier
     * @return set of device objects
     */
    Set<DeviceId> getDeviceIds(DomainId domainId);

    /**
     * Gets all the devices of the specified domain.
     * @param domainId domain identifier.
     * @return set of devices
     */
    Set<Device> getDevices(DomainId domainId);

    /**
     * Returns the set of hosts of the specified domain.
     * @param domainId domain id
     * @return set of host objects
     */
    Set<HostId> getHostIds(DomainId domainId);

    /**
     * Gets the hosts of the specified domain.
     * @param domainId domain id
     * @return set of host objects
     */
    Set<Host> getHosts(DomainId domainId);

    /**
     * Gets the list of interlinks between the specified domains.
     * @return set of interlinks
     */
    Set<Link> getInterLinks(Pair<DomainId, DomainId> endDomains);

    /**
     * Gets the list of links within a domain.
     * @param domainId domain id
     * @return set of intra-domain link
     */
    Set<Link> getIntraLinks(DomainId domainId);

    /**
     * Gets from the distributed store the mapping between the port numbers
     * of the specified device exposed to a peer domain and the local connect points.
     * @param deviceId device identifier
     * @return map between virtual ports and physical hosts
     */
    Map<PortNumber, ConnectPoint> getVirtualPortToPortMapping(DeviceId deviceId);

    /**
     * Adds a new mapping between virtual ports and local connect point for
     * a specified domain device.
     * @param deviceId identifier of the domain device
     */
    void setVirtualPortToPortMapping(DeviceId deviceId, Map<PortNumber, ConnectPoint> map);
}
