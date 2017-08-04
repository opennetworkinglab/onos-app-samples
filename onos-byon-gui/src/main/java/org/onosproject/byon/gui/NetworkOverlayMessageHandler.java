/*
 * Copyright 2014-2015 Open Networking Foundation
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

package org.onosproject.byon.gui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onos.byon.NetworkService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.TopoJson;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

/**
 * Message handler for Network topology overlay events.
 */
public class NetworkOverlayMessageHandler extends UiMessageHandler {

    private static final String BYON_FETCH_NETWORKS_REQ = "byonFetchNetworksRequest";
    private static final String BYON_FETCH_NETWORKS_RESP = "byonFetchNetworksResponse";

    private static final String NETWORKS = "networks";

    private NetworkService networkService;
    private HostService hostService1;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        networkService = directory.get(NetworkService.class);
        hostService1 = get(HostService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new FetchNetworksHandler()
        );
    }

    // === -------------------------
    // === Handler classes

    private class FetchNetworksHandler extends RequestHandler {
        public FetchNetworksHandler() {
            super(BYON_FETCH_NETWORKS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            ObjectNode rootNode = objectNode();
            ArrayNode networks = arrayNode();
            rootNode.set(NETWORKS, networks);

            for (String name : networkService.getNetworks()) {
                networks.add(networkData(name));
            }
            sendMessage(BYON_FETCH_NETWORKS_RESP, 0, rootNode);
        }

        private ObjectNode networkData(String name) {
            return objectNode()
                    .put("name", name)
                    .put("hostCount", networkService.getHosts(name).size());
        }

    }
}
