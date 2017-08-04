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

import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.net.HostId;

import java.util.Set;

/**
 * Methods implemented by the southbound components to react to the (local topology) host events.
 */
public interface IconaSBHostListener {

    /**
     * Advertises a set of new domain hosts to a domain.
     *
     * @param domainId domain interested to this addition
     * @param hosts set of hosts to add
     */
    void addHosts(DomainId domainId, Set<DomainHostDescription> hosts);

    /**
     * Replaces the information about an existing set of domain hosts.
     *
     * @param domainId domain interested to this update
     * @param hosts set of hosts to update
     */
    void replaceHosts(DomainId domainId, Set<DomainHostDescription> hosts);

    /**
     * Removes a set of domain hosts exposed to a domain.
     *
     * @param domainId domain interested to this deletion
     * @param hostIds set of host identifiers
     */
    void removeHosts(DomainId domainId, Set<HostId> hostIds);
}
