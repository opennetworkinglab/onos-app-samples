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

import org.onosproject.net.ConnectPoint;

/**
 * Abstraction of a class used to control Carrier Ethernet nodes according to their control protocol.
 */
public abstract class CarrierEthernetPacketNodeManager {

    abstract void setNodeForwarding(CarrierEthernetService service, CarrierEthernetUni srcUni,
                                    CarrierEthernetUni dstUni, ConnectPoint ingress, ConnectPoint egress,
                                    boolean first, boolean last);

    abstract void applyBandwidthProfileResources(String serviceId, CarrierEthernetUni uni);

    abstract void removeBandwidthProfileResources(String serviceId, CarrierEthernetUni uni);

    abstract void removeAllForwardingResources(CarrierEthernetService service);

}
