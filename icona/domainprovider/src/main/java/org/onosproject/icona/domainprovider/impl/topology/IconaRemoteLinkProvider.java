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

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainprovider.api.link.InterLinkDescription;
import org.onosproject.icona.domainprovider.api.link.IntraLinkDescription;
import org.onosproject.icona.domainprovider.api.link.IconaSBLinkService;
import org.onosproject.icona.domainmgr.api.LinkId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Link;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.onosproject.icona.domainprovider.impl.topology.IconaTopologyManager.DOMAIN_ID;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.icona.domainprovider.impl.topology.IconaRemoteDeviceProvider.PROVIDER_ID;
import static org.onosproject.icona.domainprovider.impl.topology.IconaRemoteDeviceProvider.PROVIDER_NAME;

/**
 * Exposes remote domain links to the core.
 */
@Component(immediate = true)
@Service(IconaSBLinkService.class)
public class IconaRemoteLinkProvider implements LinkProvider, IconaSBLinkService {
    private static final String SRC_DOMAIN_ID = "srcDomainId";
    private static final String DST_DOMAIN_ID = "dstDomainId";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    protected LinkProviderService linkProviderService;

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);
        linkProviderService = linkProviderRegistry.register(this);
    }

    @Deactivate
    public void deactivate() {
        linkProviderRegistry.unregister(this);
    }

    @Override
    public void addRemoteLink(IntraLinkDescription link) {
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(DOMAIN_ID, link.domainId().id())
                .build();
        LinkDescription linkDescription = new DefaultLinkDescription(link.src(), link.dst(),
                link.type(), true, annotations);
        linkProviderService.linkDetected(link);
    }

    @Override
    public void addInterLink(InterLinkDescription link) {
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(SRC_DOMAIN_ID, link.endDomains().getLeft().id())
                .set(DST_DOMAIN_ID, link.endDomains().getLeft().id())
                .build();
        LinkDescription linkDescription = new DefaultLinkDescription(link.src(), link.dst(),
                link.type(), true, annotations);
        linkProviderService.linkDetected(linkDescription);
    }

    @Override
    public void updateRemoteLinkState(DomainId domainId, LinkId id, Link.State state) {
        // TODO
    }

    @Override
    @Beta
    public void updateRemoteLinkMetric(DomainId domainId, LinkId id, int metric) {
        // TODO
        // the metric is meaningful only among links within the same domain and only if there is trust between domains
    }

    @Override
    public void removeRemoteLink(DomainId domainId, LinkId id) {
        // TODO
    }
}
