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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;


import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

/**
 * Representation of a Generic Carrier Ethernet NI.
 * Class is only mean to be used for establishing forwarding in CarrierEthernetPacketNodeManagers
 */
public class CarrierEthernetGenericNi extends CarrierEthernetNetworkInterface {

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
