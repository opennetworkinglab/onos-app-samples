/*
 * Copyright 2016 Open Networking Laboratory
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

import org.onosproject.ecord.metro.api.MetroConnectivityId;
import org.onosproject.ecord.metro.api.MetroPathEvent;

/**
 * Represents a metro optical connection.
 */

class CarrierEthernetMetroConnectivity {

    // TODO: In the future this may be replaced by a connectivity intent
    // FIXME: Need to keep a set of MetroConnectivityIds

    private MetroConnectivityId id;
    private MetroPathEvent.Type status;

    public CarrierEthernetMetroConnectivity(MetroConnectivityId id, MetroPathEvent.Type status) {
        this.id = id;
        this.status = status;
    }

    public MetroConnectivityId id() {
        return this.id;
    }

    public MetroPathEvent.Type status() {
        return this.status;
    }

    public void setId(MetroConnectivityId id) {
        this.id = id;
    }

    public void setStatus(MetroPathEvent.Type status) {
        this.status = status;
    }

}
