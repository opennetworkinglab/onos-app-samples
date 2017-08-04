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


import java.util.List;

/**
 * Topology config policy class.
 * Depending on the topology abstraction type, the end-points will reside on some virtual devices,
 * currently only BIG_SWITCH (one device) and FULL_MESH (as many devices as those where the end-points, or hosts,
 * are attached to) are supported.
 * The concept of end-points is still to be defined, currently it is a mac address of a NIC.
 */
public class TopologyConfig {

    /**
     * Type of topology exposed to/from a domain.
     */
    public enum Type {
        BIG_SWITCH,
        FULL_MESH
    }

    private Type type;
    private List<String> endPointIds;

    /**
     * Creates a topology configuration object using the supplied information.
     *
     * @param type topology type
     * @param endPointIds list of end-point mac addresses
     */
    public TopologyConfig(Type type, List<String> endPointIds) {
        this.type = type;
        this.endPointIds = endPointIds;
    }

    /**
     * Returns the type of topology abstraction for a specific domain.
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the list of end points that are exposed to a specific domain.
     *
     * @return list of endpoints
     */
    public List<String> endPointIds() {
        return endPointIds;
    }

}
