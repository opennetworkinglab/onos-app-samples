/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.icona.domainprovider.impl.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.core.ApplicationId;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainprovider.api.link.LinkId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.onosproject.icona.domainprovider.impl.config.TopologyConfig.Type.BIG_SWITCH;
import static org.onosproject.icona.domainprovider.impl.config.TopologyConfig.Type.FULL_MESH;
import static org.onosproject.net.Link.Type.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Provider configuration class.
 *  Refer to tools/sample_configs/multidomain-config.json, under
 *  "org.onosproject.icona.domainprovider" for a sample configuration
 */
public class IconaConfig extends Config<ApplicationId> {

    private final Logger log = getLogger(getClass());

    private static final String LOCAL_DOMAIN_ID = "localDomainId";
    private static final String BIG_SWITCH_PREFIX_ID = "bigSwitchPrefixId";
    private static final String PEERS = "peers";
    private static final String DOMAIN_ID = "domainId";

    private static final String DOMAIN_INTER_LINKS = "interlinks";
    private static final String INTER_LINK_DEVICE_ID = "deviceId";
    private static final String INTER_LINK_DEVICE_PORTNUM = "portNumber";
    private static final String INTER_LINK_ID = "interlinkId";
    private static final String INTER_LINK_TYPE = "interlinkType";

    private static final String TOPOLOGY = "topology";
    private static final String END_POINTS = "endPointIds";

    /**
     * Gets local domain identifier from configuration.
     *
     * @return string identifier
     */
    public DomainId getLocalId() {
        return DomainId.domainId(object.get(LOCAL_DOMAIN_ID).asText());
    }

    /**
     * Gets the ID of the local big switch abstraction.
     *
     * @return big switch identifier
     */
    public String getBigSwitchPrefixId() {
        return object.get(BIG_SWITCH_PREFIX_ID).asText();
    }

    /**
     * Parses the list of peers from the configuration json object.
     *
     * @return set of domain configuration objects
     */
    public Set<DomainConfig> getPeersConfig() {

        Set<DomainConfig> peers = Sets.newHashSet();

        JsonNode peerNodes = object.get(PEERS);

        peerNodes.forEach(peerNode -> {

            String id = peerNode.path(DOMAIN_ID).asText();

            DomainId domainId = new DomainId(id);

            JsonNode cpsNode = peerNode.path(DOMAIN_INTER_LINKS);

            Map<LinkId, Pair<Link.Type, ConnectPoint>> interLinkConnectPointMap = Maps.newHashMap();

            cpsNode.forEach(il -> {
                // real internal deviceId where the inter-link is attached
                DeviceId deviceId = DeviceId.deviceId(il.path(INTER_LINK_DEVICE_ID).asText());
                // TODO: LinkDiscovery must be disabled for this port
                PortNumber portNumber = PortNumber.portNumber(il.path(INTER_LINK_DEVICE_PORTNUM).asText());
                String ilid = il.path(INTER_LINK_ID).asText();
                String type = il.path(INTER_LINK_TYPE).asText();
                Link.Type ilType;
                switch (type) {
                    case "indirect":
                        ilType = INDIRECT;
                        break;
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
                interLinkConnectPointMap.put(LinkId.linkId(ilid),
                        Pair.of(ilType, new ConnectPoint(deviceId, portNumber)));

            });

            JsonNode topologyNode = peerNode.path(TOPOLOGY);
            TopologyConfig.Type type;
            switch (topologyNode.path("type")
                    .asText()) {
                case "bigSwitch":
                    type = BIG_SWITCH;
                    break;
                case "fullMesh":
                    type = FULL_MESH;
                    break;
                default:
                    type = TopologyConfig.Type.BIG_SWITCH;
            }
            ArrayList<String> endPointIds = new ArrayList<>();
            topologyNode.path(END_POINTS).forEach(
                    endPointId -> endPointIds.add(endPointId.asText())
            );
            TopologyConfig topologyConfig = new TopologyConfig(type, endPointIds);

            peers.add(new DomainConfig(domainId, interLinkConnectPointMap, topologyConfig));
        });

        return peers;
    }

    /**
     * Domain configuration class.
     */
    public static class DomainConfig {

        private final DomainId domainId;
        private final Map<LinkId, Pair<Link.Type, ConnectPoint>> interLinkConnectPointMap;
        private final TopologyConfig topologyConfig;

        public DomainConfig(DomainId domainId, Map<LinkId, Pair<Link.Type, ConnectPoint>> interLinkConnectPointMap,
                            TopologyConfig topologyConfig) {
            this.domainId = checkNotNull(domainId);
            this.interLinkConnectPointMap = interLinkConnectPointMap;
            this.topologyConfig = topologyConfig;
        }

        public DomainId peerId() {
            return domainId;
        }

        public Map<LinkId, Pair<Link.Type, ConnectPoint>> interLinkConnectPointMap() {
            return interLinkConnectPointMap;
        }

        public TopologyConfig topologyConfig() {
            return topologyConfig;
        }

    }
}
