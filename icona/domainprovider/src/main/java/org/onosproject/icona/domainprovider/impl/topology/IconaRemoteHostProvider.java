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

package org.onosproject.icona.domainprovider.impl.topology;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainprovider.api.host.DomainHostDescription;
import org.onosproject.icona.domainprovider.api.host.IconaSBHostService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.icona.domainprovider.impl.topology.IconaRemoteDeviceProvider.PROVIDER_ID;
import static org.onosproject.icona.domainprovider.impl.topology.IconaRemoteDeviceProvider.PROVIDER_NAME;

/**
 * Exposes remote domain hosts to the core.
 */
@Component(immediate = true)
@Service(IconaSBHostService.class)
public class IconaRemoteHostProvider implements HostProvider, IconaSBHostService {

    private final Logger log = getLogger(getClass());

    private static final boolean REPLACE_IPS = true;
    private static final boolean MERGE_IPS = false;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    protected HostProviderService hostProviderService;

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);
        hostProviderService = hostProviderRegistry.register(this);
    }

    @Deactivate
    public void deactivate() {
        hostProviderRegistry.unregister(this);
    }


    /**
     * Notify the core system that a new host is reachable through a domain device.
     *
     * @param domainHostDescription host description of the remote host
     */
    private void advertiseHost(DomainHostDescription domainHostDescription) {
        HostId hostId = HostId.hostId(domainHostDescription.hwAddress(), domainHostDescription.vlan());
        hostProviderService.hostDetected(hostId, domainHostDescription, REPLACE_IPS);
    }

    /**
     * Remove a remote domain host.
     *
     * @param hostId id of the vanished host
     */
    private void removeHost(HostId hostId) {
        hostProviderService.hostVanished(hostId);
    }

    // IconaSBHostService interface
    @Override
    public void addRemoteHosts(DomainId domainId, Set<DomainHostDescription> newHosts) {
        newHosts.forEach(this::advertiseHost);
    }

    @Override
    public void replaceRemoteHosts(DomainId domainId, Set<DomainHostDescription> domainHostDescriptions) {
        // TODO
    }

    @Override
    public void removeRemoteHosts(DomainId domainId, Set<HostId> hostIds) {
        // TODO
    }

    // HostProvider interface
    @Override
    public void triggerProbe(Host host) {
        // TODO Auto-generated method stub
    }
}