/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ovsdbrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Configuration info to reach the OVSDB server.
 */
public class OvsdbNodeConfig extends Config<ApplicationId> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String NODES = "nodes";
    private static final String OVSDB_PORT = "ovsdbPort";
    private static final String OVSDB_IP = "ovsdbIp";

    public Set<OvsdbNode> getNodes() {
        Set<OvsdbNode> nodes = Sets.newConcurrentHashSet();

        JsonNode jsnoNodes = object.path(NODES);
        jsnoNodes.forEach(node -> {
            IpAddress ovsdbIp = IpAddress.valueOf(node.path(OVSDB_IP).textValue());
            TpPort port = TpPort.tpPort(Integer.parseInt(node.path(OVSDB_PORT).asText()));
            log.info("Ovsdb port: " + port.toString());
            nodes.add(new OvsdbNode(ovsdbIp, port));
        });
        return nodes;
    }

    public static class OvsdbNode {
        private final IpAddress ovsdbIp;
        private final TpPort ovsdbPort;

        public OvsdbNode(IpAddress ovsdbIp, TpPort ovsdbPort) {
            this.ovsdbIp = ovsdbIp;
            this.ovsdbPort = ovsdbPort;
        }

        public IpAddress ovsdbIp() {
            return ovsdbIp;
        }

        public TpPort ovsdbPort() {
            return ovsdbPort;
        }

        public DeviceId ovsdbId() {
            return DeviceId.deviceId("ovsdb:" + ovsdbIp.toString());
        }
    }
}
