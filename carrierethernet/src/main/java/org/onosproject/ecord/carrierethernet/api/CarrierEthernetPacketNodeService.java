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
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetNetworkInterface;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;

import java.util.Set;

/**
 * Service interface used to control Carrier Ethernet nodes according to their control protocol.
 */
@Beta
public interface CarrierEthernetPacketNodeService {

    /**
     * Creates and submits FlowObjectives depending on role of the device in the FC and ingress/egress NI types.
     *
     * @param fc the forwarding construct
     * @param srcNi the source network interface
     * @param dstNiSet the set of destination network interfaces
     */
    void setNodeForwarding(CarrierEthernetForwardingConstruct fc, CarrierEthernetNetworkInterface srcNi,
                           Set<CarrierEthernetNetworkInterface> dstNiSet);

    /**
     * Creates and stores meters based on the UNI's bandwidth profile.
     *
     * @param fc the forwarding construct
     * @param uni the user to network interface
     */
    void createBandwidthProfileResources(CarrierEthernetForwardingConstruct fc, CarrierEthernetUni uni);

    /**
     * Applies meters to flows.
     *
     * @param fc the forwarding construct
     * @param uni the user to network interface
     */
    void applyBandwidthProfileResources(CarrierEthernetForwardingConstruct fc, CarrierEthernetUni uni);

    /**
     * Removes the meters associated with a specific UNI of an FC.
     *
     * @param fc the forwarding construct
     * @param uni the user to network interface
     */
    void removeBandwidthProfileResources(CarrierEthernetForwardingConstruct fc, CarrierEthernetUni uni);

    /**
     * Removes all installed flow objectives associated with a specific FC.
     *
     * @param fc the forwarding construct
     */
    void removeAllForwardingResources(CarrierEthernetForwardingConstruct fc);

}
