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

package org.onosproject.icona.domainprovider.impl.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Set;

import static org.onosproject.icona.domainprovider.impl.config.TopologyConfig.Type.BIG_SWITCH;
import static org.onosproject.icona.domainprovider.impl.config.TopologyConfig.Type.FULL_MESH;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Icona provider configuration class.
 *  Look at tools/sample_configs/icona-provider-config.json
 *  for a sample configuration
 */
public class IconaConfig extends Config<ApplicationId> {

    private final Logger log = getLogger(getClass());

    private static final String BIG_SWITCH_PREFIX_ID = "bigSwitchPrefixId";
    private static final String DRIVER = "driver";
    private static final String MANUFACTURER = "manufacturer";
    private static final String SW_VERSION = "swVersion";
    private static final String HW_VERSION = "hwVersion";
    private static final String DOMAINS = "domains";
    private static final String DOMAIN_ID = "domainId";
    private static final String TOPOLOGY_TYPE = "topologyType";
    private static final String END_POINTS = "endPointIds";
    private static final String PORT_SPEED = "portSpeed";

    /**
     * Gets the id of the local big switch abstraction.
     * @return big switch identifier
     */
    public String getBigSwitchPrefixId() {
        return object.get(BIG_SWITCH_PREFIX_ID).asText();
    }

    /**
     * Gets port speed from configuration.
     * @return port speed in mbps
     */
    // TODO: think a better way to handle this
    public int portSpeed() {
        return object.get(PORT_SPEED).asInt();
    }

    /**
     * Returns the domain device driver config parameters.
     * @return driver configuration object
     */
    public DriverConfig getDriverConfig() {
        JsonNode driverJson = object.get(DRIVER);
        String manufacturer = driverJson.get(MANUFACTURER).asText();
        String swVersion = driverJson.get(SW_VERSION).asText();
        String hwVersion = driverJson.get(HW_VERSION).asText();
        return new DriverConfig(manufacturer, swVersion, hwVersion);
    }

    /**
     * Parses the list of peers from the configuration json object.
     *
     * @return set of domain configuration objects
     */
    public Set<DomainConfig> getPeersConfig() {

        Set<DomainConfig> peers = Sets.newHashSet();

        JsonNode abstractionsNode = object.get(DOMAINS);

        abstractionsNode.forEach(peerNode -> {

            String id = peerNode.path(DOMAIN_ID).asText();

            DomainId domainId = new DomainId(id);

            TopologyConfig.Type type;
            switch (peerNode.path(TOPOLOGY_TYPE)
                    .asText()) {
                case "bigSwitch":
                    type = BIG_SWITCH;
                    break;
                case "fullMesh":
                    type = FULL_MESH;
                    break;
                default:
                    type = BIG_SWITCH;
            }
            ArrayList<String> endPointIds = new ArrayList<>();
            peerNode.path(END_POINTS).forEach(
                    endPointId -> endPointIds.add(endPointId.asText())
            );
            TopologyConfig topologyConfig = new TopologyConfig(type, endPointIds);

            peers.add(new DomainConfig(domainId, topologyConfig));
        });
        return peers;
    }

    /**
     * Domain configuration class.
     */
    public static class DomainConfig {

        private final DomainId domainId;
        private final TopologyConfig topologyConfig;

        public DomainConfig(DomainId domainId, TopologyConfig topologyConfig) {
            this.domainId = checkNotNull(domainId);
            this.topologyConfig = topologyConfig;
        }

        public DomainId domainId() {
            return domainId;
        }

        public TopologyConfig topologyConfig() {
            return topologyConfig;
        }
    }

    /**
     * Domain device driver configuration class.
     */
    public static class DriverConfig {
        private final String manufacturer;
        private final String swVersion;
        private final String hwVersion;

        public DriverConfig(String manufacturer, String swVersion, String hwVersion) {
            this.manufacturer = manufacturer;
            this.swVersion = swVersion;
            this.hwVersion = hwVersion;
        }

        public String manufacturer() {
            return manufacturer;
        }

        public String swVersion() {
            return swVersion;
        }

        public String hwVersion() {
            return hwVersion;
        }
    }
}
