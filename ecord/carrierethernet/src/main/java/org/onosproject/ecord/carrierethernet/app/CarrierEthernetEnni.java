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

import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;


import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

/**
 * Representation of a Carrier Ethernet ENNI.
 * Class can be used in different two ways:
 * 1. As a global ENNI descriptor containing one or more BW profiles
 * 2. As a service-specific ENNI descriptor containing a single S-VLAN tag and including a type (e.g. hub, spoke)
 */
public class CarrierEthernetEnni extends CarrierEthernetNetworkInterface {

    private final Logger log = getLogger(getClass());

    public enum Role {

        HUB("Hub"),
        SPOKE("Spoke");

        private String value;

        Role(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    protected Role role;
    protected Set<VlanId> sVlanIdSet;
    String tpid;

    public CarrierEthernetEnni(ConnectPoint connectPoint, String uniCfgId, Role role, VlanId sVlanId, String tpid,
                               Bandwidth usedCapacity) {

        super(connectPoint, uniCfgId);
        // TODO: Check for null
        this.role = role;
        this.sVlanIdSet = Sets.newConcurrentHashSet();
        // The following applies only to service-specific ENNIs
        if (sVlanId != null) {
            this.sVlanIdSet.add(sVlanId);
            // TODO: Use Hub/Spoke role correctly
            this.role = Role.HUB;
            this.usedCapacity = usedCapacity;
            this.tpid = tpid;
        }
    }

    /**
     * Adds the resources associated with an EVC-specific ENNI to a global ENNI.
     *
     * @param enni the EVC ENNI to be added
     */
    // TODO: Make these methods abstract
    public void addEvcEnni(CarrierEthernetEnni enni) {

        // Add S-VLAN ID
        if (enni.sVlanId() != VlanId.NONE) {
            this.sVlanIdSet.add(enni.sVlanId());
        }
        // Used capacity cannot be more than ENNI capacity
        this.usedCapacity = Bandwidth.bps(Math.min(this.usedCapacity.bps() + enni.usedCapacity().bps(), this.capacity.bps()));
    }

    /**
     * Removes the resources associated with a service-specific ENNI from a global ENNI.
     *
     * @param enni the service ENNI to be added
     */
    public void removeEvcEnni(CarrierEthernetEnni enni) {

        // Remove UNI CE-VLAN ID
        sVlanIdSet.remove(enni.sVlanId());

        // Redundant check - should be avoided by check in validateBwp
        this.usedCapacity = Bandwidth.bps(Math.max(this.usedCapacity.bps() - enni.usedCapacity().bps(), 0));
    }

    /**
     * Validates whether an EVC-specific ENNI is compatible with a global ENNI.
     *
     * @param enni the EVC-specific UNI
     * @return boolean value indicating whether the UNIs are compatible
     */
    public boolean validateEvcEnni(CarrierEthernetEnni enni) {

        // Check if the S-VLAN ID of the ENNI is already included in global ENNI
        if (enni.sVlanId() != VlanId.NONE) {
            if (sVlanIdSet.contains(enni.sVlanId())) {
                log.error("S-VLAN ID {} already exists in ENNI {}", enni.sVlanId().toString(), this.id());
                return false;
            }
        }

        // Check whether there are enough available resources on the ENNI
        if (usedCapacity.bps() + enni.usedCapacity().bps() > capacity.bps()) {
            log.error("ENNI {} cannot be added to global ENNI {} due to lack of resources", enni.id(), this.id());
            return false;
        }

        return true;
    }

    /**
     * Returns ENNI role - applicable only to service-specific ENNIs.
     *
     * @return ENNI role
     */
    public Role role() {
        return role;
    }

    /**
     * Returns the S-VLAN id associated with a service ENNI, or the first S-VLAN ID found for a global ENNI.
     *
     * @return S-VLAN id
     */
    public VlanId sVlanId() {
        if (sVlanIdSet.isEmpty()) {
            return VlanId.NONE;
        } else {
            return sVlanIdSet.iterator().next();
        }
    }

    /**
     * Always returns null, since CE-VLAN IDs are not associated with ENNIs.
     *
     * @return null
     */
    @Override
    public VlanId ceVlanId() {
        return null;
    }

    /**
     * Returns ENNI tpid - applicable only to service-specific ENNIs.
     *
     * @return tpid
     */
    public String tpid() {
        return tpid;
    }

    /**
     * Returns the set of S-VLAN ids associated with the ENNI.
     *
     * @return S-VLAN id set
     */
    public Set<VlanId> sVlanIdSet() {
        return sVlanIdSet;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
                .add("id", this.id)
                .add("cfgId", this.cfgId)
                .add("role", role)
                .add("sVlanIds", sVlanIdSet)
                .add("capacity", this.capacity)
                .add("usedCapacity", this.usedCapacity).toString();
    }

}
