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

package org.onosproject.icona.domainprovider.api;

import org.onosproject.icona.domainprovider.api.device.DomainDevice;
import org.onosproject.icona.domainprovider.api.host.DomainHostDescription;
import org.onosproject.icona.domainprovider.api.link.IntraLinkDescription;

import java.util.Set;
/**
 * Methods for describing a topology exposed by a domain.
 */
public interface DomainTopology extends DomainElement {

    /**
     * Returns the set of "virtual" devices of the domain.
     * @return set of devices
     */
    Set<DomainDevice> domainDevices();

    /**
     * @return set of intra-links
     */
    Set<IntraLinkDescription> domainLinks();

    /**
     * Returns set of hosts exposed by the domain.
     *
     * @return set of domain hosts
     */
    Set<DomainHostDescription> domainHosts();
}
