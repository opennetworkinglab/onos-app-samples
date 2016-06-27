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
 * Representation of a Carrier Ethernet INNI.
 * Class can be used in different two ways:
 * 1. As a global INNI descriptor containing one or more BW profiles
 * 2. As a service-specific INNI descriptor containing a single S-VLAN tag and including a type (e.g. hub, spoke)
 */
public class CarrierEthernetInni extends CarrierEthernetNetworkInterface {

    private final Logger log = getLogger(getClass());

    public enum Role {

        HUB("Hub"),
        // FIXME: Remove that after hackathon?
        TRUNK("Trunk"),
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

    public CarrierEthernetInni(ConnectPoint connectPoint, String uniCfgId, Role role, VlanId sVlanId, String tpid,
                               Bandwidth usedCapacity) {

        super(connectPoint, uniCfgId);
        // TODO: Check for null
        this.role = role;
        this.sVlanIdSet = Sets.newConcurrentHashSet();
        // The following applies only to service-specific INNIs
        if (sVlanId != null) {
            this.sVlanIdSet.add(sVlanId);
            // TODO: Use role correctly
            this.role = Role.HUB;
            this.usedCapacity = usedCapacity;
            this.tpid = tpid;
        }
    }

    /**
     * Adds the resources associated with an EVC-specific INNI to a global INNI.
     *
     * @param inni the EVC INNI to be added
     */
    // TODO: Make these methods abstract
    public void addEvcInni(CarrierEthernetInni inni) {

        // Add S-VLAN ID
        if (inni.sVlanId() != VlanId.NONE) {
            this.sVlanIdSet.add(inni.sVlanId());
        }
        // Used capacity cannot be more than INNI capacity
        this.usedCapacity = Bandwidth.bps(Math.min(this.usedCapacity.bps() + inni.usedCapacity().bps(),
                this.capacity.bps()));
    }

    /**
     * Removes the resources associated with a service-specific INNI from a global INNI.
     *
     * @param inni the service INNI to be added
     */
    public void removeEvcInni(CarrierEthernetInni inni) {

        // Remove UNI CE-VLAN ID
        sVlanIdSet.remove(inni.sVlanId());

        // Redundant check - should be avoided by check in validateBwp
        this.usedCapacity = Bandwidth.bps(Math.max(this.usedCapacity.bps() - inni.usedCapacity().bps(), 0));
    }

    /**
     * Validates whether an EVC-specific INNI is compatible with a global INNI.
     *
     * @param inni the EVC-specific UNI
     * @return boolean value indicating whether the UNIs are compatible
     */
    public boolean validateEvcInni(CarrierEthernetInni inni) {

        // Check if the S-VLAN ID of the INNI is already included in global INNI
        if (inni.sVlanId() != null) {
            if (sVlanIdSet.contains(inni.sVlanId())) {
                log.error("S-VLAN ID {} already exists in INNI {}", inni.sVlanId().toString(), this.id());
                return false;
            }
        }

        // Check whether there are enough available resources on the INNI
        if (usedCapacity.bps() + inni.usedCapacity().bps() > capacity.bps()) {
            log.error("INNI {} cannot be added to global INNI {} due to lack of resources", inni.id(), this.id());
            return false;
        }

        return true;
    }

    /**
     * Returns INNI role - applicable only to service-specific INNIs.
     *
     * @return INNI role
     */
    public Role role() {
        return role;
    }

    /**
     * Returns the S-VLAN id associated with a service INNI, or the first S-VLAN ID found for a global INNI.
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
     * Always returns null, since CE-VLAN IDs are not associated with INNIs.
     *
     * @return null
     */
    @Override
    public VlanId ceVlanId() {
        return null;
    }

    /**
     * Returns the set of S-VLAN ids associated with the INNI.
     *
     * @return S-VLAN id set
     */
    public Set<VlanId> sVlanIdSet() {
        return ImmutableSet.copyOf(sVlanIdSet);
    }

    /**
     * Returns INNI tpid - applicable only to service-specific INNIs.
     *
     * @return tpid
     */
    public String tpid() {
        return tpid;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
                .add("id", this.id)
                .add("cfgId", this.cfgId)
                .add("role", role)
                .add("refCount", refCount)
                .add("sVlanIds", sVlanIdSet)
                .add("capacity", this.capacity)
                .add("usedCapacity", this.usedCapacity).toString();
    }

}
