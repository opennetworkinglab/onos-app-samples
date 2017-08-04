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
package org.onosproject.ecord.carrierethernet.app;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Weight class to cause path selection only on the pre-calculated spanning tree.
 */
public class CarrierEthernetSpanningTreeWeight implements LinkWeight {

    private final Logger log = getLogger(getClass());

    protected TopologyService topologyService = null;

    public CarrierEthernetSpanningTreeWeight(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @Override
    public double weight(TopologyEdge edge) {
        if (!isBroadCastPoint(edge.link().src()) || !isBroadCastPoint(edge.link().dst())) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Checks if a connect point is on the pre-calculated spanning tree.
     *
     * @param cp the connect point to check
     * @return true if the connect point is on the spanning tree and false otherwise
     */
    private boolean isBroadCastPoint(ConnectPoint cp) {
        // TODO: Get topology snapshot so that same spanning tree is used by all pairs if topology changes
        if (cp == null) {
            log.info("Cp is null!");
        }
        if (topologyService == null) {
            log.info("topologyservice is null!");
        }
        return topologyService.isBroadcastPoint(topologyService.currentTopology(), cp);
    }
}
