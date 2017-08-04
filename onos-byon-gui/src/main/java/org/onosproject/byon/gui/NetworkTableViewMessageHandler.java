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
import com.google.common.collect.ImmutableSet;
import org.onos.byon.NetworkService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Message handler for UI Ref table-view.
 */
public class NetworkTableViewMessageHandler extends UiMessageHandler {

    private static final String BYON_NETWORKS_DATA_REQ = "byonNetworkDataRequest";
    private static final String BYON_NETWORKS_DATA_RESP = "byonNetworkDataResponse";
    private static final String BYON_NETWORKS = "byonNetworks";

    private static final String BYON_NETWORKS_DETAIL_REQ = "byonNetworkDetailsRequest";
    private static final String BYON_NETWORKS_DETAIL_RESP = "byonNetworkDetailsResponse";
    private static final String DETAILS = "details";

    private static final String ID = "id";
    private static final String HOST_COUNT = "hostCount";

    private static final String[] COLUMN_IDS = {ID, HOST_COUNT};

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ByonNetworkDataRequestHandler(),
                new ByonNetworkDetailRequestHandler()
        );
    }

    // handler for table view data requests
    private final class ByonNetworkDataRequestHandler extends TableRequestHandler {

        private ByonNetworkDataRequestHandler() {
            super(BYON_NETWORKS_DATA_REQ, BYON_NETWORKS_DATA_RESP, BYON_NETWORKS);
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return null;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            NetworkService service = get(NetworkService.class);
            for (String name: service.getNetworks()) {
                int hostCount = service.getHosts(name).size();
                populateRow(tm.addRow(), name, hostCount);
            }
        }

        private void populateRow(TableModel.Row row, String name, int hostCount) {
            row.cell(ID, name)
                    .cell(HOST_COUNT, hostCount);
        }
    }

    // handler for table view item details requests
    private final class ByonNetworkDetailRequestHandler extends RequestHandler {

        public static final String HOSTS = "hosts";

        private ByonNetworkDetailRequestHandler() {
            super(BYON_NETWORKS_DETAIL_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String name = string(payload, ID, "(none)");

            NetworkService networkService = get(NetworkService.class);
            HostService hostService = get(HostService.class);
            ObjectNode rootNode = objectNode();
            ObjectNode data = objectNode();
            rootNode.set(DETAILS, data);

            ArrayNode hosts = arrayNode();
            data.set(HOSTS, hosts);
            data.put(ID, name);

            for (HostId hostId : networkService.getHosts(name)) {
                hosts.add(hostData(hostService.getHost(hostId)));
            }
            sendMessage(BYON_NETWORKS_DETAIL_RESP, 0, rootNode);
        }

        private ObjectNode hostData(Host host) {
            return objectNode()
                    .put("mac", host.mac().toString())
                    .put("ip", host.ipAddresses().toString())
                    .put("loc", host.location().toString());
        }
    }

}
