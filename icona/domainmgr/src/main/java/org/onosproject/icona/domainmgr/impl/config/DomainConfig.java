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

package org.onosproject.icona.domainmgr.impl.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.core.ApplicationId;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainmgr.api.LinkId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.Link.Type.*;
import static org.onosproject.net.Link.Type.INDIRECT;

/**
 * Configuration class for domain identifiers.
 * Look at tools/sample_configs/icona-config.json
 * for a sample configuration
 */
public class DomainConfig extends Config<ApplicationId> {

    private static final String DOMAIN_ID = "domainId";
    private static final String LOCAL_DOMAIN = "localDomain";
    private static final String REMOTE_DOMAINS = "remoteDomains";
    private static final String INTER_LINK_PORTS = "interlinkPorts";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String LINK_ID = "id";
    private static final String LINK_TYPE = "type";

    /**
     * Gets local domain identifier from configuration.
     * @return string identifier
     */
    public DomainId getLocalId() {
        return DomainId.domainId(object.get(LOCAL_DOMAIN).asText());
    }

    /**
     * Gets the remote domain identifiers.
     * @return set of domain IDs
     */
    public Set<DomainId> remoteDomainIds() {
        Set<DomainId> remoteDomainIds = Sets.newConcurrentHashSet();
        object.path(REMOTE_DOMAINS).forEach(domainElem -> remoteDomainIds.add(
                new DomainId(domainElem.path(DOMAIN_ID).asText())
        ));
        return ImmutableSet.copyOf(remoteDomainIds);
    }

    public Map<LinkId, Pair<Link.Type, ConnectPoint>> interlinkMap() {
        Map<LinkId, Pair<Link.Type, ConnectPoint>> map = new HashMap<>();

        JsonNode domainElems = object.path(REMOTE_DOMAINS);
        // for each domain
        domainElems.forEach(domainElem -> {
            JsonNode interlinksJson = domainElem.path(INTER_LINK_PORTS);

            // for each interlink of a single domain
            interlinksJson.forEach(interlinkJson -> {
                LinkId linkId = LinkId.linkId(interlinkJson.path(LINK_ID).asText());
                DeviceId deviceId = DeviceId.deviceId(interlinkJson.path(DEVICE_ID).asText());
                PortNumber portNumber = PortNumber.fromString(interlinkJson.path(PORT_NUMBER).asText());
                String type = interlinkJson.path(LINK_TYPE).asText();
                Link.Type ilType;
                switch (type) {
                    case "virtual":
                        ilType = VIRTUAL;
                        break;
                    case "optical":
                        ilType = OPTICAL;
                        break;
                    case "tunnel":
                        ilType = TUNNEL;
                        break;
                    case "direct":
                        ilType = DIRECT;
                        break;
                    default:
                        ilType = INDIRECT;

                }
                map.put(linkId, Pair.of(ilType, new ConnectPoint(deviceId, portNumber)));
            });
        });

        return map;
    }
}

