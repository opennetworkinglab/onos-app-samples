/*
 * Copyright 2016 Open Networking Foundation
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

import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;

/**
 * Represents a metro optical connection.
 */

class CarrierEthernetMetroConnectivity {

    // TODO: In the future this may be replaced by a connectivity intent
    // FIXME: Need to keep a set of OpticalConnectivityIds

    private OpticalConnectivityId id;
    private OpticalPathEvent.Type status;

    public CarrierEthernetMetroConnectivity(OpticalConnectivityId id, OpticalPathEvent.Type status) {
        this.id = id;
        this.status = status;
    }

    public OpticalConnectivityId id() {
        return this.id;
    }

    public OpticalPathEvent.Type status() {
        return this.status;
    }

    public void setId(OpticalConnectivityId id) {
        this.id = id;
    }

    public void setStatus(OpticalPathEvent.Type status) {
        this.status = status;
    }

}
