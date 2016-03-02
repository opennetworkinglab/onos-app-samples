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

import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;


import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a Carrier Ethernet UNI.
 * Class can be used in different two ways:
 * 1. As a global UNI descriptor containing one or more BW profiles
 * 2. As a service-specific UNI descriptor containing a single BW profile and including a type (root, leaf)
 */
public class CarrierEthernetUni {

    private final Logger log = getLogger(getClass());

    protected DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

    public enum Type {
        ROOT, LEAF
    }

    protected ConnectPoint connectPoint;
    protected String uniId;
    protected String uniCfgId;
    protected Type type;
    protected Set<VlanId> ceVlanIdSet;
    protected Bandwidth capacity;
    protected Bandwidth usedCapacity;

    // Note: INTERFACE BWP map can only have up to one element
    protected final Map<CarrierEthernetBandwidthProfile.Type, Map<String, CarrierEthernetBandwidthProfile>> bwpMap =
            new HashMap<>();

    // TODO: May be needed to add refCount for CoS BWPs - only applicable to global UNIs

    public CarrierEthernetUni(ConnectPoint connectPoint, String uniCfgId, Type type, VlanId ceVlanId,
                              CarrierEthernetBandwidthProfile bwp) {
        // TODO: Check for null
        this.connectPoint = connectPoint;
        this.uniId = this.connectPoint.deviceId().toString() + "/" + this.connectPoint.port().toString();
        this.uniCfgId = (uniCfgId == null ? this.uniId : uniCfgId);
        this.type = type;
        this.ceVlanIdSet = new HashSet<>();
        if (ceVlanId != null) {
            this.ceVlanIdSet.add(ceVlanId);
        }
        this.capacity = Bandwidth.mbps(deviceService.getPort(connectPoint.deviceId(), connectPoint.port())
                .portSpeed());
        this.usedCapacity = Bandwidth.mbps((double) 0);
        for (CarrierEthernetBandwidthProfile.Type bwpType : CarrierEthernetBandwidthProfile.Type.values()) {
            this.bwpMap.put(bwpType, new HashMap<>());
        }

        if (bwp != null) {
            // Limit the CIR of the provided bwp according to UNI capacity
            if (bwp.cir().bps() > this.capacity.bps()) {
                log.warn("UNI {}: Limiting provided CIR ({} bps) to UNI capacity ({} bps)",
                        this.uniId, (long) bwp.cir().bps(), this.capacity);
            }
            bwp.setCir(Bandwidth.bps(Math.min(bwp.cir().bps(), this.capacity.bps())));

            // Limit the EIR of the provided bwp according to the UNI capacity minus CIR
            if (bwp.eir().bps() > this.capacity.bps() - bwp.cir().bps()) {
                log.warn("UNI {}: Limiting provided EIR ({} bps) to UNI capacity minus CIR ({} bps)",
                        this.uniId, bwp.eir().bps(), this.capacity.bps() - bwp.cir().bps());
            }
            bwp.setEir(Bandwidth.bps(Math.min(bwp.eir().bps(), this.capacity.bps() - bwp.cir().bps())));

            addBandwidthProfile(bwp);
        }
    }

    /**
     * Adds the resources associated with a service-specific UNI to a global UNI.
     *
     * @param uni the service UNI to be added
     */
    public void addServiceUni(CarrierEthernetUni uni) {

        // Add CE-VLAN ID
        if (uni.ceVlanId() != null) {
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
    public void removeServiceUni(CarrierEthernetUni uni) {

        // Remove UNI CE-VLAN ID
        ceVlanIdSet.remove(uni.ceVlanId());

        // Remove UNI BWP
        CarrierEthernetBandwidthProfile bwp = uni.bwp();
        Map<String, CarrierEthernetBandwidthProfile> subBwpMap = this.bwpMap.get(bwp.type());
        subBwpMap.remove(bwp.id());
        this.bwpMap.put(bwp.type(), subBwpMap);
        // Redundant check - should be avoided by check in validateBwp
        this.usedCapacity = Bandwidth.bps(Math.max(this.usedCapacity.bps() - bwp.cir().bps(), 0));
    }

    /**
     * Validates whether a service-specific UNI is compatible with a global UNI.
     *
     * @param uni the service-specific UNI
     * @return boolean value indicating whether the UNIs are compatible
     */
    public boolean validateServiceUni(CarrierEthernetUni uni) {

        // Check if the CE-VLAN ID of the UNI is already included in global UNI
        if (uni.ceVlanId() != null) {
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
     * Returns associated connect point.
     *
     * @return associated connect point
     */
    public ConnectPoint cp() {
        return connectPoint;
    }

    /**
     * Returns UNI string identifier.
     *
     * @return UNI string identifier
     */
    public String id() {
        return uniId;
    }

    /**
     * Returns UNI string config identifier.
     *
     * @return UNI string config identifier
     */
    public String cfgId() {
        return uniCfgId;
    }

    /**
     * Returns UNI type (ROOT or LEAF) - applicable only to service-specific UNIs.
     *
     * @return UNI type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the CE-VLAN id associated with a local UNI, or the first CE-VLAN ID found for a global UNI.
     *
     * @return CE-VLAN id
     */
    public VlanId ceVlanId() {
        if (ceVlanIdSet.isEmpty()) {
            return null;
        } else {
            return ceVlanIdSet.iterator().next();
        }
    }

    /**
     * Returns the set of CE-VLAN ids associated with the UNI.
     *
     * @return CE-VLAN id set
     */
    public Set<VlanId> ceVlanIdSet() {
        return ceVlanIdSet;
    }

    /**
     * Returns the first non-null BWP of the UNI found - used mainly for service-specific UNIs.
     * Note: The Service-specific UNI representation will only have one BWP
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

    /**
     * Returns UNI capacity in bps.
     *
     * @return UNI capacity
     */
    public Bandwidth capacity() {
        return capacity;
    }

    /**
     * Sets UNI string identifier.
     *
     * @param uniId the UNI string identifier to set
     */
    public void setId(String uniId) {
        this.uniId = uniId;
    }

    /**
     * Sets UNI string config identifier.
     *
     * @param uniCfgId the UNI string config identifier to set
     */
    public void setCfgId(String uniCfgId) {
        this.uniCfgId = uniCfgId;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
                .add("id", uniId)
                .add("cfgId", uniCfgId)
                .add("type", type)
                .add("ceVlanIds", ceVlanIdSet)
                .add("capacity", capacity)
                .add("usedCapacity", usedCapacity)
                .add("bandwidthProfiles", this.bwps()).toString();
    }

}
