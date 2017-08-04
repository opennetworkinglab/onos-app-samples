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

import com.google.common.collect.ImmutableSet;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainprovider.api.device.DomainDevice;
import org.onosproject.icona.domainprovider.api.host.DomainHostDescription;
import org.onosproject.icona.domainprovider.api.link.IntraLinkDescription;

import java.util.Set;

/**
 * Default implementation of {@link DomainTopology}.
 */
public class DefaultDomainTopology implements DomainTopology {

    private final DomainId domainId;
    private final Set<DomainDevice> devices;
    private final Set<IntraLinkDescription> intraLinkDescriptions;
    private final Set<DomainHostDescription> domainHostDescriptions;

    /**
     * Creates a domain topology object with the supplied elements.
     *
     * @param domainId domain identifier
     * @param devices set of devices
     * @param intraLinkDescriptions set of links inside the domain
     * @param domainHostDescriptions set of hosts
     */
    public DefaultDomainTopology(DomainId domainId, Set<DomainDevice> devices,
                                 Set<IntraLinkDescription> intraLinkDescriptions,
                                 Set<DomainHostDescription> domainHostDescriptions) {
        this.domainId = domainId;
        this.devices = devices;
        this.intraLinkDescriptions = intraLinkDescriptions;
        this.domainHostDescriptions = domainHostDescriptions;
    }

    @Override
    public DomainId domainId() {
        return domainId;
    }

    @Override
    public Set<DomainDevice> domainDevices() {
        return ImmutableSet.copyOf(devices);
    }

    @Override
    public Set<IntraLinkDescription> domainLinks() {
        return ImmutableSet.copyOf(intraLinkDescriptions);
    }

    @Override
    public Set<DomainHostDescription> domainHosts() {
       return ImmutableSet.copyOf(domainHostDescriptions);
    }
}
