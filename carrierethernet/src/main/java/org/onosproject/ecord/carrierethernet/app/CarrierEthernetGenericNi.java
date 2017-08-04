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

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of a Generic Carrier Ethernet NI.
 * Class is only meant to be used for establishing forwarding in CarrierEthernetPacketNodeManagers
 */
public class CarrierEthernetGenericNi extends CarrierEthernetNetworkInterface<CarrierEthernetGenericNi> {

    public enum Role {

        NONE("None");

        private String value;

        Role(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public CarrierEthernetGenericNi(ConnectPoint connectPoint, String uniCfgId) {
        super(connectPoint, Type.GENERIC, uniCfgId);
    }

    @Override
    public Role role() {
        return Role.NONE;
    }

    /**
     * Always returns null, since CE-VLAN IDs are not associated with Generic NIs.
     *
     * @return null
     */
    @Override
    public VlanId ceVlanId() {
        return null;
    }

    /**
     * Always returns null, since S-TAGs are not associated with Generic NIs.
     *
     * @return null
     */
    @Override
    public VlanId sVlanId() {
        return null;
    }

    /**
     * Dummy implementation of abstract method (for generic NI type there is no concept of EVC vs. global NIs).
     *
     * @param gni a generic NI
     */
    @Override
    public void addEcNi(CarrierEthernetGenericNi gni) {}

    /**
     * Dummy implementation of abstract method (for generic NI type there is no concept of EVC vs. global NIs).
     *
     * @param gni a generic NI
     */
    @Override
    public void removeEcNi(CarrierEthernetGenericNi gni) {}

    /**
     * Dummy implementation of abstract method (for generic NI type there is no concept of EVC vs. global NIs).
     *
     * @param gni a generic NI
     * @return true
     */
    @Override
    public boolean validateEcNi(CarrierEthernetGenericNi gni) {
        return true;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
                .add("id", this.id)
                .add("cfgId", this.cfgId)
                .add("refCount", refCount)
                .add("capacity", this.capacity)
                .add("usedCapacity", this.usedCapacity).toString();
    }

}
