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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of a Carrier Ethernet INNI.
 * Class can be used in different two ways:
 * 1. As a global INNI descriptor containing one or more S-VLAN tags
 * 2. As a service-specific INNI descriptor containing a single S-VLAN tag and including a role (e.g. hub, spoke)
 */
public final class CarrierEthernetInni extends CarrierEthernetNetworkInterface<CarrierEthernetInni> {

    private final Logger log = getLogger(getClass());

    public enum Role {

        HUB("Hub"),
        // FIXME: Remove that after hackathon?
        TRUNK("Trunk"),
        SPOKE("Spoke"),
        // FIXME: Remove these after LTP-NI role mapping is fixed
        ROOT("Root"),
        LEAF("Leaf");

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
    // TODO: Associate TPIDs with S-TAGs
    protected String tpid;

    // TODO: Change sVlanId to Collection<VlanId>
    private CarrierEthernetInni(ConnectPoint connectPoint, String uniCfgId,
                               Role role, VlanId sVlanId, String tpid,
                               Bandwidth usedCapacity) {

        super(connectPoint, Type.INNI, uniCfgId);

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
     * Adds the resources associated with an FC-specific INNI to a global INNI.
     *
     * @param inni the FC-specific INNI to be added
     */
    @Override
    public void addEcNi(CarrierEthernetInni inni) {
        // Add S-VLAN ID
        if (inni.sVlanId() != VlanId.NONE) {
            this.sVlanIdSet.add(inni.sVlanId());
        }
        // Used capacity cannot be more than INNI capacity
        this.usedCapacity = Bandwidth.bps(Math.min(this.usedCapacity.bps() + inni.usedCapacity().bps(),
                this.capacity.bps()));
    }

    /**
     * Removes the resources associated with an FC-specific INNI from a global INNI.
     *
     * @param inni the FC-specific INNI to be removed
     */
    @Override
    public void removeEcNi(CarrierEthernetInni inni) {
        // Remove UNI CE-VLAN ID
        sVlanIdSet.remove(inni.sVlanId());
        // Redundant check - should be avoided by check in validateBwp
        this.usedCapacity = Bandwidth.bps(Math.max(this.usedCapacity.bps() - inni.usedCapacity().bps(), 0));
    }

    /**
     * Validates whether an FC-specific INNI is compatible with the corresponding global INNI.
     *
     * @param inni the FC-specific INNI
     * @return boolean value indicating whether the INNIs are compatible
     */
    @Override
    public boolean validateEcNi(CarrierEthernetInni inni) {

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
    @Override
    public Role role() {
        return role;
    }

    /**
     * Returns the S-VLAN id associated with a service INNI, or the first S-VLAN ID found for a global INNI.
     * This is assumed to be the S-TAG of another FC interconnected with this INNI.
     *
     * @return S-VLAN id
     */
    @Override
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
     * Those are assumed to be the S-TAGs of other FCs interconnected with this INNI.
     *
     * @return S-VLAN id set
     */
    public Set<VlanId> sVlanIdSet() {
        return ImmutableSet.copyOf(sVlanIdSet);
    }

    // FIXME: Find a better way to implement this method
    /**
     * Sets the S-VLAN id associated with an FC INNI.
     *
     * @param sVlanId S-VLAN id to set
     */
    public void setSVlanId(VlanId sVlanId) {
        sVlanIdSet.add(sVlanId);
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

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of CarrierEthernetInni entities.
     */
    public static final class Builder {

        private ConnectPoint cp;
        private String cfgId;
        private Role role;
        private VlanId sVlanId;
        private String tpid;
        private Bandwidth usedCapacity;

        /**
         * Sets the cp of this builder.
         *
         * @param cp the builder cp to set
         * @return this builder instance
         */
        public Builder cp(ConnectPoint cp) {
            this.cp = cp;
            return this;
        }

        /**
         * Sets the cfgId of this builder.
         *
         * @param cfgId the builder cfgId to set
         * @return this builder instance
         */
        public Builder cfgId(String cfgId) {
            this.cfgId = cfgId;
            return this;
        }

        /**
         * Sets the role of this builder.
         *
         * @param role the builder role to set
         * @return this builder instance
         */
        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        /**
         * Sets the sVlanId of this builder.
         *
         * @param sVlanId the builder sVlanId to set
         * @return this builder instance
         */
        public Builder sVlanId(VlanId sVlanId) {
            this.sVlanId = sVlanId;
            return this;
        }

        /**
         * Sets the tpid of this builder.
         *
         * @param tpid the builder tpid to set
         * @return this builder instance
         */
        public Builder tpid(String tpid) {
            this.tpid = tpid;
            return this;
        }

        /**
         * Sets the usedCapacity of this builder.
         *
         * @param usedCapacity the builder usedCapacity to set
         * @return this builder instance
         */
        public Builder usedCapacity(Bandwidth usedCapacity) {
            this.usedCapacity = usedCapacity;
            return this;
        }

        /**
         * Builds a new CarrierEthernetInni instance.
         * based on this builder's parameters
         *
         * @return a new CarrierEthernetInni instance
         */
        public CarrierEthernetInni build() {
            return new CarrierEthernetInni(cp, cfgId, role, sVlanId, tpid, usedCapacity);
        }
    }
}
