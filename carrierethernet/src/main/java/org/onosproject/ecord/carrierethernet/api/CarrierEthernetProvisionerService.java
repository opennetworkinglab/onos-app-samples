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

package org.onosproject.ecord.carrierethernet.api;

import com.google.common.annotations.Beta;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetForwardingConstruct;

/**
 * Service interface for provisioning of Carrier Ethernet connections.
 */
@Beta
public interface CarrierEthernetProvisionerService {
    /**
     * Creates bandwidth profiles at the UNIs of an FC.
     *
     * @param fc the forwarding construct
     */
    void createBandwidthProfiles(CarrierEthernetForwardingConstruct fc);

    /**
     * Applies bandwidth profiles to the UNIs of an FC.
     *
     * @param fc the forwarding construct
     */
    void applyBandwidthProfiles(CarrierEthernetForwardingConstruct fc);

    /**
     * Removes bandwidth profiles from the UNIs of an FC.
     *
     * @param fc the forwarding construct
     */
    void removeBandwidthProfiles(CarrierEthernetForwardingConstruct fc);

    /**
     * Establishes connectivity for the provided FC by installing all necessary forwarding rules.
     *
     * @param fc the forwarding construct
     */
    void setupConnectivity(CarrierEthernetForwardingConstruct fc);

    /**
     * Removes connectivity for the provided FC by removing all installed forwarding rules.
     *
     * @param fc the forwarding construct
     */
    void removeConnectivity(CarrierEthernetForwardingConstruct fc);

}
