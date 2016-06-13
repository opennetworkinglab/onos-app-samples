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
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a Carrier Ethernet UNI.
 * Class can be used in different two ways:
 * 1. As a global UNI descriptor containing one or more BW profiles
 * 2. As a service-specific UNI descriptor containing a single BW profile and including a type (root, leaf)
 */
public class CarrierEthernetUni extends CarrierEthernetNetworkInterface {

    private final Logger log = getLogger(getClass());

    public enum Role {

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
    protected Set<VlanId> ceVlanIdSet;

    // Note: INTERFACE BWP map can only have up to one element
    protected final Map<CarrierEthernetBandwidthProfile.Type, Map<String, CarrierEthernetBandwidthProfile>> bwpMap =
            new HashMap<>();

    // TODO: May be needed to add refCount for CoS BWPs - only applicable to global UNIs

    public CarrierEthernetUni(ConnectPoint connectPoint, String uniCfgId, Role role, VlanId ceVlanId,
                              CarrierEthernetBandwidthProfile bwp) {
        super(connectPoint, uniCfgId);
        this.role = role;
        // FIXME: Set the NI scope directly instead?
        this.scope = (role == null ? Scope.GLOBAL : Scope.SERVICE);
        this.ceVlanIdSet = Sets.newConcurrentHashSet();
        if (ceVlanId != null) {
            this.ceVlanIdSet.add(ceVlanId);
        }
        for (CarrierEthernetBandwidthProfile.Type bwpType : CarrierEthernetBandwidthProfile.Type.values()) {
            this.bwpMap.put(bwpType, new HashMap<>());
        }

        if (bwp != null) {
            // Limit the CIR of the provided bwp according to UNI capacity
            if (bwp.cir().bps() > this.capacity.bps()) {
                log.warn("UNI {}: Limiting provided CIR ({} bps) to UNI capacity ({} bps)",
                        this.id, (long) bwp.cir().bps(), this.capacity);
            }
            bwp.setCir(Bandwidth.bps(Math.min(bwp.cir().bps(), this.capacity.bps())));

            // Limit the EIR of the provided bwp according to the UNI capacity minus CIR
            if (bwp.eir().bps() > this.capacity.bps() - bwp.cir().bps()) {
                log.warn("UNI {}: Limiting provided EIR ({} bps) to UNI capacity minus CIR ({} bps)",
                        this.id, bwp.eir().bps(), this.capacity.bps() - bwp.cir().bps());
            }
            bwp.setEir(Bandwidth.bps(Math.min(bwp.eir().bps(), this.capacity.bps() - bwp.cir().bps())));

            addBandwidthProfile(bwp);
        }
    }

    /**
     * Adds the resources associated with an EVC-specific UNI to a global UNI.
     *
     * @param uni the EVC UNI to be added
     */
    public void addEvcUni(CarrierEthernetUni uni) {

        // Add CE-VLAN ID
        if (uni.ceVlanId() != VlanId.NONE) {
            this.ceVlanIdSet.add(uni.ceVlanId());
        }

        // Add UNI BWP
        CarrierEthernetBandwidthProfile bwp = uni.bwp();
        Map<String, CarrierEthernetBandwidthProfile> subBwpMap = this.bwpMap.get(bwp.type());
        subBwpMap.put(bwp.id(), bwp);
        this.bwpMap.put(bwp.type(), subBwpMap);
        // Used capacity cannot be more than UNI capacity (redundant check - should be avoided by check in validateBwp)
        this.usedCapacity = Bandwidth.bps(Math.min(this.usedCapacity.bps() + bwp.cir().bps(), this.capacity.bps()));
    }

    /**
     * Adds a BW profile to a UNI.
     *
     * @param bwp the BWP to be added
     */
    public void addBandwidthProfile(CarrierEthernetBandwidthProfile bwp) {

        Map<String, CarrierEthernetBandwidthProfile> subBwpMap = this.bwpMap.get(bwp.type());
        subBwpMap.put(bwp.id(), bwp);
        this.bwpMap.put(bwp.type(), subBwpMap);
        // Used capacity cannot be more than UNI capacity (redundant check - should be avoided by check in validateBwp)
        this.usedCapacity = Bandwidth.bps(Math.min(this.usedCapacity.bps() + bwp.cir().bps(), this.capacity.bps()));
    }

    /**
     * Removes the resources associated with a service-specific UNI from a global UNI.
     *
     * @param uni the service UNI to be added
     */
    public void removeEvcUni(CarrierEthernetUni uni) {

        // Remove UNI CE-VLAN ID
        if (uni.ceVlanId() != VlanId.NONE) {
            ceVlanIdSet.remove(uni.ceVlanId());
        }

        // Remove UNI BWP
        CarrierEthernetBandwidthProfile bwp = uni.bwp();
        Map<String, CarrierEthernetBandwidthProfile> subBwpMap = this.bwpMap.get(bwp.type());
        subBwpMap.remove(bwp.id());
        this.bwpMap.put(bwp.type(), subBwpMap);
        // Redundant check - should be avoided by check in validateBwp
        this.usedCapacity = Bandwidth.bps(Math.max(this.usedCapacity.bps() - bwp.cir().bps(), 0));
    }

    /**
     * Validates whether an EVC-specific UNI is compatible with a global UNI.
     *
     * @param uni the EVC-specific UNI
     * @return boolean value indicating whether the UNIs are compatible
     */
    public boolean validateEvcUni(CarrierEthernetUni uni) {

        // Check if the CE-VLAN ID of the UNI is already included in global UNI
        if (uni.ceVlanId() != VlanId.NONE) {
            if (ceVlanIdSet.contains(uni.ceVlanId())) {
                log.error("CE-VLAN ID {} already exists in UNI {}", uni.ceVlanId().toString(), this.id());
                return false;
            }
        }

        CarrierEthernetBandwidthProfile bwp = uni.bwp();

        // Check if the UNI BW profile is allowed based on its type and id and the existing profiles on the global UNI
        for (CarrierEthernetBandwidthProfile.Type bwpType : CarrierEthernetBandwidthProfile.Type.values()) {
            Map<String, CarrierEthernetBandwidthProfile> subBwpMap = this.bwpMap.get(bwpType);
            if (!(subBwpMap.isEmpty())) {
                if (bwpType != bwp.type()) {
                    log.error("Different bandwidth profile type than {} already exists in UNI {}",
                            bwp.type().name(), this.id());
                    return false;
                } else if (subBwpMap.containsKey(bwp.id())) {
                    log.error("Bandwidth profile {} already exists in UNI {}", bwp.id(), this.id());
                    return false;
                } else if (bwp.type().equals(CarrierEthernetBandwidthProfile.Type.INTERFACE)) {
                    log.error("Another bandwidth profile already exists in UNI {}", this.id());
                    return false;
                }
            }
        }

        // Check whether there are enough available resources on the UNI
        if (usedCapacity.bps() + bwp.cir().bps() > capacity.bps()) {
            log.error("Bandwidth profile {} cannot be added to UNI {} due to lack of resources", bwp.id(), this.id());
            return false;
        }

        return true;
    }

    /**
     * Returns UNI role (ROOT or LEAF) - applicable only to service-specific UNIs.
     *
     * @return UNI role
     */
    public Role role() {
        return role;
    }

    /**
     * Returns the CE-VLAN id associated with a local UNI, or the first CE-VLAN ID found for a global UNI.
     *
     * @return CE-VLAN id
     */
    public VlanId ceVlanId() {
        if (ceVlanIdSet.isEmpty()) {
            return VlanId.NONE;
        } else {
            return ceVlanIdSet.iterator().next();
        }
    }

    /**
     * Always returns null, since S-TAGs are not associated with UNIs.
     *
     * @return null
     */
    @Override
    public VlanId sVlanId() {
        return null;
    }

    /**
     * Returns the set of CE-VLAN ids associated with the UNI.
     *
     * @return CE-VLAN id set
     */
    public Set<VlanId> ceVlanIdSet() {
        return ImmutableSet.copyOf(ceVlanIdSet);
    }

    /**
     * Returns the first non-null BWP of the UNI found - used mainly for service-specific UNIs.
     * Note: The EVC-specific UNI representation will only have one BWP
     *
     * @return first non-null BWP of the UNI
     */
    public CarrierEthernetBandwidthProfile bwp() {

        for (CarrierEthernetBandwidthProfile.Type bwpType : CarrierEthernetBandwidthProfile.Type.values()) {
            if (!(this.bwpMap.get(bwpType).isEmpty())) {
                return bwpMap.get(bwpType).entrySet().iterator().next().getValue();
            }
        }
        return null;
    }

    /**
     * Returns a collection of all BWPs of the UNI
     *
     * @return all BWPs of the UNI
     */
    public Collection<CarrierEthernetBandwidthProfile> bwps() {

        for (CarrierEthernetBandwidthProfile.Type bwpType : CarrierEthernetBandwidthProfile.Type.values()) {
            if (!(this.bwpMap.get(bwpType).isEmpty())) {
                return bwpMap.get(bwpType).values();
            }
        }
        // Return an empty collection if no BWPs exist
        return Collections.emptyList();
    }

    @Override
    public String toString() {

        return toStringHelper(this)
                .add("id", this.id)
                .add("cfgId", this.cfgId)
                .add("role", role)
                .add("ceVlanIds", ceVlanIdSet)
                .add("capacity", this.capacity)
                .add("usedCapacity", this.usedCapacity)
                .add("bandwidthProfiles", this.bwps()).toString();
    }

}
