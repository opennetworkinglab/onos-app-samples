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
 * Service consumed by the southbound components to advertise
 * remote host events to the provider.
 */
public interface IconaSBHostService {

    /**
     * Signals the domain provider to add a set of remote domain hosts.
     *
     * @param domainId remote domain identifier
     * @param domainHostDescriptions hosts to be added
     */
    void addRemoteHosts(DomainId domainId, Set<DomainHostDescription> domainHostDescriptions);

    /**
     * Signals the domain provider to replace the set of domain hosts.
     *
     * @param domainId remote domain identifier
     * @param domainHostDescriptions hosts to be updated
     */
    void replaceRemoteHosts(DomainId domainId, Set<DomainHostDescription> domainHostDescriptions);

    /**
     * Signals the domain provider to remove the given remote hosts.
     *
     * @param domainId domain identifier
     * @param hostIds host identifiers
     */
    void removeRemoteHosts(DomainId domainId, Set<HostId> hostIds);
}
