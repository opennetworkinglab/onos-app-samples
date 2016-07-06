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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service(value = CarrierEthernetManager.class)
public class CarrierEthernetManager {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CarrierEthernetProvisioner ceProvisioner;

    // Keeps track of the next S-VLAN tag the app will try to use
    private static short nextVlanId = 1;

    // TODO: Implement distributed store for EVCs
    // The installed EVCs
    private final Map<String, CarrierEthernetVirtualConnection> evcMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for Forwarding Constructs
    // The installed Forwarding Constructs
    private final Map<String, CarrierEthernetForwardingConstruct> fcMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for CE UNIs
    // The installed CE UNIs
    private final Map<String, CarrierEthernetUni> uniMap = new ConcurrentHashMap<>();
    private final Set<String> removedUniSet = Sets.newConcurrentHashSet();;

    // TODO: Implement distributed store for CE LTPs
    // The installed CE LTPs
    private final Map<String, CarrierEthernetLogicalTerminationPoint> ltpMap = new ConcurrentHashMap<>();

    // The LTP ids that have been explicitly removed (or requested to be removed) from the global LTP map
    private final Set<String> removedLtpSet = Sets.newConcurrentHashSet();;

    /**
     * Activate this component.
     */
    @Activate
    public void activate() {}

    /**
     * Deactivate this component.
     */
    @Deactivate
    public void deactivate() {
        removeAllEvcs();
        removeAllFcs();
    }

    /**
     * Returns the map of installed EVCs.
     *
     * @return map of installed EVCs
     */
    public  Map<String, CarrierEthernetVirtualConnection> evcMap() {
        return this.evcMap;
    }

    // TODO: Add method to remove a UNI from an already installed EVC

    /**
     * Get an installed EVC using its id.
     *
     * @param evcId the EVC id
     * @return the EVC representation or null if the EVC doesn't exist
     */
    public CarrierEthernetVirtualConnection getEvc(String evcId) {
        return ((evcMap.get(evcId) == null) ? null : evcMap.get(evcId));
    }

    /**
     * Get an installed FC using its id.
     *
     * @param fcId the FC id
     * @return the FC representation or null if the EVC doesn't exist
     */
    public CarrierEthernetForwardingConstruct getFc(String fcId) {
        return ((fcMap.get(fcId) == null) ? null : fcMap.get(fcId));
    }

    /**
     * Get the map containing all installed FCs
     *
     * @return the FC map
     */
    public Map<String, CarrierEthernetForwardingConstruct> fcMap() {
        return fcMap;
    }

    /**
     * Get the map containing all global LTPs
     *
     * @return the global LTP map
     */
    public Map<String, CarrierEthernetLogicalTerminationPoint> ltpMap() {
        return ltpMap;
    }

    /**
     * Get the map containing all global UNIs
     *
     * @return the global UNI map
     */
    public Map<String, CarrierEthernetUni> getUniMap() {
        return uniMap;
    }

    /**
     * Verify the validity of an EVC representation taking also into account current network status.
     *
     * @param originalEvc the provided EVC representation
     * @return a valid, potentially modified EVC representation, or null if the EVC could not be validated
     */
    private CarrierEthernetVirtualConnection validateEvc(CarrierEthernetVirtualConnection originalEvc) {

        // Make a copy of the provided EVC, since it may be modified
        CarrierEthernetVirtualConnection evc = originalEvc;

        // Try to set a unique VLAN id for the EVC unless the EVC is being updated
        // TODO: Add different connectivity types
        if (evc.vlanId() == null) {
            evc.setVlanId(generateVlanId());
            if (evc.vlanId() == null) {
                log.error("No available VLAN id found.");
                return null;
            }
        }

        // Verify that CE-VLAN ID is provided to either all UNIs or none and set the virtualEvc flag accordingly
        // Note: Checking also that all NIs are UNIs
        boolean isVirtual = false;
        Iterator<CarrierEthernetNetworkInterface> it = evc.niSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetNetworkInterface ni = it.next();
            if (!ni.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                log.error("All EVC NIs have to be of type UNI.");
                return null;
            }
            if (ni.ceVlanId() == VlanId.NONE && isVirtual) {
                log.error("Could not validate the virtual status of the EVC.");
                return null;
            } else if (ni.ceVlanId() != VlanId.NONE){
                isVirtual = true;
            }
        }
        evc.setIsVirtual(isVirtual);

        // Set unique id for the EVC unless the EVC is being updated
        if (evc.id() == null) {
            evc.setId(generateEvcId(evc));
        }

        Set<CarrierEthernetNetworkInterface> validatedUniSet = new HashSet<>();

        // Check the UNIs of the EVC, possibly removing UNIs that are incompatible with existing global ones
        it = evc.niSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = (CarrierEthernetUni) it.next();
            // Change the name of the UNI's BWP to the EVC name if it is an EVC BWP
            if (uni.bwp().type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                uni.bwp().setId(evc.id());
            }
            // Check first if corresponding global UNI already exists by checking against the global UNI Map
            if (uniMap.keySet().contains(uni.id())) {
                CarrierEthernetUni existingUni = uniMap.get(uni.id());
                // Check if the EVC-specific UNI is compatible with the global one
                if (!(existingUni.validateEvcUni(uni))) {
                    // If EVC is of ROOT_MULTIPOINT type and we have removed the root, return null
                    if (evc.type() == CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT &&
                            uni.role() == CarrierEthernetUni.Role.ROOT) {
                        log.error("Root UNI could not be added to %s EVC.", evc.type().name());
                        return null;
                    }
                    log.warn("UNI {} could not be added to EVC.", uni.id());
                    continue;
                } else {
                    // Add UNI to EVC
                    validatedUniSet.add(uni);
                }
            } else {
                // Add UNI to EVC
                validatedUniSet.add(uni);
            }
        }

        // Update the EVC UNI set, based on the validated UNIs
        evc.setNiSet(validatedUniSet);

        if (evc.niSet().size() > evc.maxNumUni()) {
            log.error("{} EVC can have at most {} UNIs.", evc.maxNumUni());
            return null;
        }

        if ((evc.type().equals(CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT)
                || evc.type().equals(CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT))
                && (evc.niSet().size() < 2)) {
            log.error("{} EVC requires at least two UNIs.", evc.type().name());
            return null;
        }

        if (evc.type().equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT) && (evc.niSet().size() != 2)) {
            log.error("{} EVC requires exactly two UNIs.", evc.type().name());
            return null;
        }

        return evc;
    }

    // TODO: Refactor so that EVCs are always made of FCs
    /**
     * Establish connectivity according to the EVC type (E-Line, E-Tree, E-LAN) and the EVC parameters.
     *
     * @param evc the EVC representation
     * @return the (potentially modified) EVC that was installed or null if EVC connectivity could not be established
     */
    public CarrierEthernetVirtualConnection establishConnectivity(CarrierEthernetVirtualConnection evc) {

        // If EVC already exists, remove it and reestablish with new parameters
        if (evc.id() != null && evcMap.containsKey(evc.id())) {
            return updateEvc(evc);
        } else {
            evc.setId(null);
        }

        evc = validateEvc(evc);

        if (evc == null) {
            log.error("EVC could not be installed, please check log for details.");
            return null;
        }

        ceProvisioner.setupConnectivity(evc.niSet(), evc);

        // If no pair was connected, do not register the EVC
        if (evc.state().equals(CarrierEthernetVirtualConnection.State.ACTIVE)) {
            // Apply BWP-related resources (e.g. Meters) to the packet switches
            ceProvisioner.applyBandwidthProfiles(evc);
            // Apply the BWPs of the EVC UNI to the global UNIs, creating them if needed
            applyBandwidthProfiles(evc.niSet());
            // Increment the global UNI reference count
            // FIXME: Remove this as soon as EVCs are made of FCs
            evc.niSet().forEach(uni -> uniMap.get(uni.id()).refCount().incrementAndGet());
            evcMap.put(evc.id(), evc);
        }

        return evc;
    }

    /**
     * Reestablish connectivity for an existing EVC.
     *
     * @param originalEvc the updated EVC definition
     * @return the (potentially modified) EVC that was installed or null if EVC connectivity could not be established
     */
    public CarrierEthernetVirtualConnection updateEvc(CarrierEthernetVirtualConnection originalEvc) {
        // Just checking again
        if (evcMap.containsKey(originalEvc.id())) {
            log.info("Updating existing EVC {}", originalEvc.id());
            // Keep the VLAN ID of the original EVC
            originalEvc.setVlanId(evcMap.get(originalEvc.id()).vlanId());
            removeEvc(originalEvc.id());
        }
        return establishConnectivity(originalEvc);
    }

    /**
     * Applies bandwidth profiles to the UNIs of an EVC and if needed adds the UNIs to the global UNI map.
     *
     * @param  niSet set of UNIs that are included in the EVC
     */
    private void applyBandwidthProfiles(Set<CarrierEthernetNetworkInterface> niSet) {

        niSet.forEach(uni -> {
            if (!(uniMap.keySet().contains(uni.id()))) {
                // Just add the UNI as it appears at the EVC
                uniMap.put(uni.id(), (CarrierEthernetUni) uni);
            } else {
                // Add UNI resources (BWP, CE-VLAN ID) to existing global UNI
                uniMap.get(uni.id()).addEvcUni((CarrierEthernetUni) uni);
                // Update config identifier
                uniMap.get(uni.id()).setCfgId(uni.cfgId());
            }
        });
    }

    /**
     * Removes bandwidth profiles from the UNIs of an EVC and removes them if needed from the global UNI map.
     *
     * @param evcId the EVC id
     */
    private void removeBandwidthProfiles(String evcId) {

        evcMap.get(evcId).niSet().forEach(uni -> {
            // TODO: Check if the bandwidth profile really needs to be removed (e.g. may be CoS)
            ceProvisioner.removeBandwidthProfiles(evcMap.get(evcId));

            // Remove UNI resources (BWP, CE-VLAN ID) from global UNI
            uniMap.get(uni.id()).removeEvcUni((CarrierEthernetUni) uni);
        });
    }

    /**
     * Removes all installed EVCs and the associated resources.
     *
     * This will be called either from the deactivate method or as a response to a CLI/REST command.
     * */
    public void removeAllEvcs() {
        evcMap.keySet().forEach(evcId -> {
            CarrierEthernetVirtualConnection evc = evcMap.get(evcId);
            ceProvisioner.removeConnectivity(evc);
            ceProvisioner.removeBandwidthProfiles(evc);
            removeBandwidthProfiles(evcId);
            // Avoid excessively incrementing VLAN ids
            nextVlanId = (evc.vlanId().toShort() < nextVlanId ? evc.vlanId().toShort() : nextVlanId);
            // Decrement the global UNI and corresponding NI refCount
            // FIXME: Remove this as soon as EVCs are always made of FCs
            evc.niSet().forEach(uni -> uniMap.get(uni.id()).refCount().decrementAndGet());
        });
        evcMap.clear();
    }

    /**
     * Removes all resources associated with a specific installed EVC.
     *
     * @param evcId the EVC id
     * */
    public void removeEvc(String evcId) {
        if (evcMap.containsKey(evcId)) {
            CarrierEthernetVirtualConnection evc = evcMap.get(evcId);
            ceProvisioner.removeConnectivity(evc);
            ceProvisioner.removeBandwidthProfiles(evc);
            removeBandwidthProfiles(evcId);
            // Avoid excessively incrementing VLAN ids
            nextVlanId = (evc.vlanId().toShort() < nextVlanId ? evc.vlanId().toShort() : nextVlanId);
            // Decrement the global UNI and corresponding NI refCount
            // FIXME: Remove this as soon as EVCs are always made of FCs
            evc.niSet().forEach(uni -> uniMap.get(uni.id()).refCount().decrementAndGet());
            evcMap.remove(evcId);
        }
    }

    /**
     * Verify the validity of an FC representation taking also into account current network status.
     *
     * @param fc the provided FC representation
     * @return a valid, potentially modified FC representation, or null if the FC could not be validated
     */
    private CarrierEthernetForwardingConstruct validateFc(CarrierEthernetForwardingConstruct fc) {

        // Try to set a unique VLAN id for the FC unless the EVC is being updated
        // TODO: Add different connectivity types
        fc.evcLite().setVlanId(generateVlanId());
        if (fc.evcLite().vlanId() == null) {
            log.error("No available VLAN id found.");
            return null;
        }

        // Verify that CE-VLAN ID is provided to either all UNIs or none and set the virtualEvc flag accordingly
        boolean isVirtual = false;
        Iterator<CarrierEthernetNetworkInterface> it = fc.evcLite().niSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = (CarrierEthernetUni) it.next();
            if (uni.ceVlanId() == null && isVirtual) {
                log.error("Could not validate the virtual status of the EVC.");
                return null;
            } else if (uni.ceVlanId() != null){
                isVirtual = true;
            }
        }
        fc.evcLite().setIsVirtual(isVirtual);

        // Generate and set unique FC id
        fc.setId(generateEvcId(fc.evcLite()));
        fc.evcLite().setId(fc.id());

        Set<CarrierEthernetUni> validatedUniSet = new HashSet<>();

        // Check the UNIs of the EVC, possibly removing UNIs that are incompatible with existing ones
        it = fc.evcLite().niSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = (CarrierEthernetUni) it.next();
            // Change the name of the UNI's BWP to the EVC name if it is an EVC BWP
            if (uni.bwp().type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                uni.bwp().setId(fc.evcLite().id());
            }
            // Check first if UNI already exists by checking against the global UNI Map
            if (uniMap.keySet().contains(uni.id())) {
                CarrierEthernetUni existingUni = uniMap.get(uni.id());
                // Check if the EVC-specific UNI is compatible with the global one
                if (!(existingUni.validateEvcUni(uni))) {
                    // If EVC is of ROOT_MULTIPOINT type and we have removed the root, return null
                    if (fc.evcLite().type() == CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT &&
                            uni.role() == CarrierEthernetUni.Role.ROOT) {
                        log.error("Root UNI could not be added to %s EVC.", fc.evcLite().type().name());
                        return null;
                    }
                    log.warn("UNI {} could not be added to EVC.", uni.id());
                    continue;
                } else {
                    // Add UNI to evc description
                    validatedUniSet.add(uni);
                }
            } else {
                // Add UNI to EVC description
                validatedUniSet.add(uni);
            }
        }

        // TODO: Add validation for INNIs/ENNIs as well

        // Update the FC LTP set, based on the UNIs actually used
        Set<CarrierEthernetLogicalTerminationPoint> validatedLtpSet = new HashSet<>();
        Iterator<CarrierEthernetLogicalTerminationPoint> ltpIt = fc.ltpSet().iterator();
        while(ltpIt.hasNext()) {
            CarrierEthernetLogicalTerminationPoint ltp = ltpIt.next();
            if ((ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI))
                    && (!validatedUniSet.contains(ltp.ni()))) {
                continue;
            }
            validatedLtpSet.add(ltp);
        }
        fc.setLtpSet(validatedLtpSet);

        return fc;
    }

    /**
     * Installs all resources associated with a specific FC.
     *
     * @param fc the FC to install
     * @return the FC that was installed
     * */
    public CarrierEthernetForwardingConstruct installFc(CarrierEthernetForwardingConstruct fc) {

        // If FC already exists, remove it and reestablish with new parameters
        if (fc.id() != null && fcMap.containsKey(fc.id())) {
            return updateFc(fc);
        } else {
            fc.setId(null);
        }

        fc = validateFc(fc);

        if (fc == null) {
            log.error("FC could not be installed, please check log for details.");
            return null;
        }

        // Temporary set for iterating through FC NI pairs
        Set<CarrierEthernetNetworkInterface> niSet = new HashSet<>();
        fc.ltpSet().forEach(ltp -> {
            niSet.add(ltp.ni());
        });

        ceProvisioner.setupConnectivity(niSet, fc.evcLite());

        // FIXME: Update the LTP set within setupConnectivity()
        // Update the FC LTP set, based on the NIs actually used
        Set<CarrierEthernetLogicalTerminationPoint> usedLtpSet = new HashSet<>();
        Iterator<CarrierEthernetLogicalTerminationPoint> ltpIt = fc.ltpSet().iterator();
        while (ltpIt.hasNext()) {
            CarrierEthernetLogicalTerminationPoint ltp = ltpIt.next();
            if (fc.evcLite().niSet().contains(ltp.ni())) {
                usedLtpSet.add(ltp);
            }
        }
        fc.setLtpSet(usedLtpSet);

        // Set FC state according to EVC Lite state
        fc.setState(CarrierEthernetForwardingConstruct.State.valueOf(fc.evcLite().state().name()));
        fc.setActiveState(CarrierEthernetForwardingConstruct.ActiveState.valueOf(fc.evcLite().activeState().name()));

        // If no pair was connected, do not register the FC
        if (fc.state().equals(CarrierEthernetForwardingConstruct.State.ACTIVE)) {
            // Apply BWP-related resources (e.g. Meters) to the packet switches
            // FIXME: Hack - Resetting the evcLite set to only UNIs until setupConnectivity() uses FCs
            Set<CarrierEthernetNetworkInterface> tempNiSet = new HashSet<>();
            fc.ltpSet().forEach(ltp -> {
                if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                    tempNiSet.add(ltp.ni());
                }
            });
            fc.evcLite().setNiSet(tempNiSet);
            ceProvisioner.applyBandwidthProfiles(fc.evcLite());
            // Apply the BWPs of the EVC UNI to the global UNIs, creating them if needed
            applyBandwidthProfiles(fc.evcLite().niSet());
            // Increment the global LTP and corresponding NI refCount
            fc.ltpSet().forEach(ltp -> ltpMap.get(ltp.id()).refCount().incrementAndGet());
            fcMap.put(fc.id(), fc);
            evcMap.put(fc.evcLite().id(), fc.evcLite());
        }

        return fc;
    }

    /**
     * Reestablish connectivity for an existing FC.
     *
     * @param fc the updated FC representation
     * @return the possibly modified FC that was installed or null if updated FC could not be installed
     */
    public CarrierEthernetForwardingConstruct updateFc(CarrierEthernetForwardingConstruct fc) {
        // Just checking again
        if (fcMap.containsKey(fc.id())) {
            log.info("Updating existing FC {}", fc.id());
            // Keep the VLAN ID of the original FC
            fc.evcLite().setVlanId(fcMap.get(fc.id()).evcLite().vlanId());
            removeFc(fc.id());
        }
        return installFc(fc);
    }

    /**
     * Removes all resources associated with the application.
     *
     * This will be called either from the deactivate method or as a response to a CLI command.
     * */
    public void removeAllFcs() {
        fcMap.keySet().forEach(fcId -> {
            removeEvc(fcMap.get(fcId).evcLite().id());
            // FIXME: Remove the UNI constraint as soon as EVCs are always made of FCs
            fcMap.get(fcId).ltpSet()
                    .forEach(ltp -> {
                        if (!(ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI))) {
                            ltpMap.get(ltp.id()).refCount().decrementAndGet();
                        }
                    });
        });
        fcMap.clear();
    }

    /**
     * Removes all resources associated with a specific FC.
     *
     * @param fcId the FC id
     * */
    public void removeFc(String fcId) {
        if (fcMap.containsKey(fcId)) {
            // FIXME: For now, UNI refCount will be updated in removeEvc()
            removeEvc(fcMap.get(fcId).evcLite().id());
            // Decrement the global LTP and corresponding NI refCount
            // FIXME: Remove the UNI constraint as soon as EVCs are always constructed of FCs
            fcMap.get(fcId).ltpSet()
                    .forEach(ltp -> {
                        if (!(ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI))) {
                            ltpMap.get(ltp.id()).refCount().decrementAndGet();
                        }
                    });
            fcMap.remove(fcId);
        }
    }

    /**
     * Generates a unique vlanId in the context of the CE app.
     *
     * @return the generated vlanId or null if none found
     * */
    public VlanId generateVlanId() {

        List<VlanId> vlanList = evcMap.values().stream().map(CarrierEthernetVirtualConnection::vlanId)
                .collect(Collectors.toList());

        // If all vlanIds are being used return null, else try to find the next available one
        if (vlanList.size() <  VlanId.MAX_VLAN - 1) {
            while (vlanList.contains(VlanId.vlanId(nextVlanId))) {
                // Get next valid short
                nextVlanId = (nextVlanId >= VlanId.MAX_VLAN || nextVlanId <= 0 ? 1 : (short) (nextVlanId + 1));
            }
        }

        return (vlanList.contains(VlanId.vlanId(nextVlanId)) ? null : VlanId.vlanId(nextVlanId));
    }

    /**
     * Generates a unique EVC id in the context of the CE app.
     *
     * @param evc the EVC representation
     * @return the generated EVC id or null if none found
     * */
    public String generateEvcId(CarrierEthernetVirtualConnection evc) {

        // TODO: Add different connectivity types

        String tmpType;

        if (evc.type().equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT)) {
            tmpType = "Line";
        } else if (evc.type().equals(CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT)) {
            tmpType = "LAN";
        } else {
            tmpType = "Tree";
        }

        String evcId = "E" + (evc.isVirtual() ? "V" : "") + "P-" + tmpType + "-" +
                evc.vlanId().toString();

        return evcId;
    }

    /**
     * Remove an LTP from the set of global LTPs.
     *
     *
     * @param ltpId the id of the LTP to be removed
     * @return the LTP that was removed or null in case of failure (didn't exist of refCount was not 0)
     * */
    public CarrierEthernetLogicalTerminationPoint removeGlobalLtp(String ltpId) {

        if (!ltpMap.containsKey(ltpId)) {
            log.warn("Could not remove LTP {}: Does not exist", ltpId);
            return null;
        }

        if (ltpMap.get(ltpId).refCount().get() != 0) {
            log.warn("Could not remove LTP {}: RefCount is not zero", ltpId);
            return null;
        }

        // Remove LTP from ltpMap and (if needed) UNI from uniMap
        CarrierEthernetLogicalTerminationPoint ltp = ltpMap.remove(ltpId);
        if (ltp.ni() instanceof CarrierEthernetUni) {
            removeGlobalUni(ltp.ni().id());
        }

        // Add LTP to removed set
        removedLtpSet.add(ltpId);

        return ltp;
    }

    /**
     * Remove an UNI from the set of global UNIs.
     *
     * @param uniId the id of the UNI to be removed
     * @return the UNI that was removed or null in case of failure (didn't exist of refCount was not 0)
     * */
    public CarrierEthernetUni removeGlobalUni(String uniId) {

        if (!uniMap.containsKey(uniId)) {
            log.warn("Could not remove UNI {}: Does not exist", uniId);
            return null;
        }
        if (uniMap.get(uniId).refCount().get() != 0) {
            log.warn("Could not remove UNI {}: RefCount is not zero", uniId);
            return null;
        }

        // Remove UNI from uniMap and corresponding LTP (if any) from ltpMp
        CarrierEthernetUni uni = uniMap.remove(uniId);
        // FIXME: For now, find LTP assuming ltpId is the same as uniId
        // Note: If refCount for UNI is not zero, then it should be for the corresponding LTP as well
        ltpMap.remove(uniId);

        // Add UNI to removed set
        removedUniSet.add(uniId);
        removedLtpSet.add(uniId);

        return uni;
    }

    /**
     * Returns all potential UNIs from the topology.
     *
     * @return set of all potential UNIs in the topology
     * */
    public Set<CarrierEthernetUni> getUnisFromTopo() {

        CarrierEthernetUni uni;
        Set<CarrierEthernetUni> uniSet = new HashSet<>();
        // Generate the device ID/port number identifiers
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                if (!port.number().isLogical()) {
                    String cpString = device.id().toString() + "/" + port.number();
                    ConnectPoint cp = ConnectPoint.deviceConnectPoint(cpString);
                    uni = generateUni(cp);
                    // Check if LTP was generated and whether it's currently removed
                    if (uni != null && !removedUniSet.contains(uni.id())) {
                        uniSet.add(uni);
                    }
                }
            }
        }
        return uniSet;
    }

    /**
     * Creates a new UNI associated with the provided connect point.
     *
     * Conditions for validating an UNI:
     * - ConnectPoint deviceId and Port are valid
     * - Port is enabled
     *
     * @param cp the connect point to be associated with the generated UNI
     * @return a new validated UNI or null if the validation failed
     * */
    public CarrierEthernetUni generateUni(ConnectPoint cp) {

        String uniId = cp.deviceId().toString() + "/" + cp.port().toString();

        if (deviceService.getDevice(cp.deviceId()) == null) {
            log.error("Could not generate UNI {}: Invalid deviceId {}", uniId, cp.deviceId());
            return null;
        }
        if (deviceService.getPort(cp.deviceId(), cp.port()) == null) {
            log.error("Could not generate UNI {}: Invalid port {} at device {}", uniId, cp.port(), cp.deviceId());
            return null;
        }
        if (!deviceService.getDevice(cp.deviceId()).type().equals(Device.Type.SWITCH)) {
            log.debug("Could not generate UNI {}: Device {} is not a switch", uniId, cp.deviceId());
            return null;
        }

        Port port = deviceService.getPort(cp.deviceId(), cp.port());

        if (!port.isEnabled())  {
            log.debug("Could not generate UNI {}: Port {} is not enabled", uniId, port.number().toString());
            return null;
        }

        if (validateLtpType(cp, CarrierEthernetNetworkInterface.Type.UNI) == null) {
            return null;
        }

        return new CarrierEthernetUni(cp, uniId);
    }

    /**
     * Adds a potential UNI to the global UNI map if they are not already there.
     *
     * @param uni the potential UNI to add to global UNI map
     * @return the UNI that was added or null if UNI existed already
     * */
    public CarrierEthernetUni addGlobalUni(CarrierEthernetUni uni) {
        // Add UNI only if it's not already there. If corresponding LTP already exists, link them, otherwise create it
        if (!uniMap.containsKey(uni.id())) {
            // Add LTP only if it's not already there
            // FIXME: Assumes LTP and UNI id are the same
            if (!ltpMap.containsKey(uni.id())) {
                ltpMap.put(uni.id(), new CarrierEthernetLogicalTerminationPoint(uni.id(), uni));
            }
            uniMap.put(uni.id(), uni);
            return  uni;
        } else {
            return null;
        }
    }

    /**
     * Returns all potential LTPs from the topology.
     *
     * @return set of all potential LTPs in the topology
     * */
    public Set<CarrierEthernetLogicalTerminationPoint> getLtpsFromTopo() {

        CarrierEthernetLogicalTerminationPoint ltp;
        Set<CarrierEthernetLogicalTerminationPoint> ltpSet = new HashSet<>();
        // Generate the device ID/port number identifiers
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                if (!port.number().isLogical()) {
                    String cpString = device.id().toString() + "/" + port.number();
                    ConnectPoint cp = ConnectPoint.deviceConnectPoint(cpString);
                    ltp = generateLtp(cp, null);
                    // Check if LTP was generated and whether it's currently removed
                    if (ltp != null && !removedLtpSet.contains(ltp.id())) {
                        // Check additionally if associated UNI is currently removed
                        if (!(ltp.ni() instanceof CarrierEthernetUni) || !removedUniSet.contains(ltp.ni().id())) {
                            ltpSet.add(ltp);
                        }
                    }
                }
            }
        }
        return ltpSet;
    }

    /**
     * Creates a new LTP of the provided type and associated with the provided connect point.
     *
     * Conditions for validating an LTP:
     * - ConnectPoint deviceId and Port are valid
     * - Port is enabled
     *
     * @param cp the connect point to be associated with the generated LTP
     * @param ltpType the type of the LTP to be generated (UNI/INNI/ENNI)
     * @return a new validated LTP or null if the validation failed
     * */
    public CarrierEthernetLogicalTerminationPoint generateLtp(ConnectPoint cp,
                                                               CarrierEthernetNetworkInterface.Type ltpType) {

        String ltpId = cp.deviceId().toString() + "/" + cp.port().toString();

        if (deviceService.getDevice(cp.deviceId()) == null) {
            log.error("Could not generate LTP {}: Invalid deviceId {}", ltpId, cp.deviceId());
            return null;
        }
        if (deviceService.getPort(cp.deviceId(), cp.port()) == null) {
            log.error("Could not generate LTP {}: Invalid port {} at device {}", ltpId, cp.port(), cp.deviceId());
            return null;
        }
        if (!deviceService.getDevice(cp.deviceId()).type().equals(Device.Type.SWITCH)) {
            log.debug("Could not generate LTP {}: Device {} is not a switch", ltpId, cp.deviceId());
            return null;
        }

        Port port = deviceService.getPort(cp.deviceId(), cp.port());

        if (!port.isEnabled())  {
            log.debug("Could not generate LTP {}: Port {} is not enabled", ltpId, port.number().toString());
            return null;
        }

        ltpType = validateLtpType(cp, ltpType);

        if (ltpType == null) {
            log.warn("Could not generate LTP {}: Type could not be validated", ltpId, port.number().toString());
            return null;
        }

        return new CarrierEthernetLogicalTerminationPoint(cp, ltpId, ltpType);
    }

    /**
     * Validates whether the provided connect point can be associated with an LTP of the provided type.
     *
     * Conditions for validating the LTP type:
     * - If UNI: ConnectPoint is not associated with any link
     * - If INNI/ENNI: ConnectPoint is associated with a link
     *
     * @param cp the connect point associated with the LTP to be validated
     * @param ltpType the type of the LTP to be validated or null in case a type is to be decided by the method
     * @return the ltpType if validation succeeded, a new type depending on cp and topo, or null if validation failed
     * */
    private CarrierEthernetNetworkInterface.Type validateLtpType(
            ConnectPoint cp, CarrierEthernetNetworkInterface.Type ltpType) {
        if (linkService.getEgressLinks(cp).isEmpty() && linkService.getIngressLinks(cp).isEmpty()) {
            // A connect point can be a UNI only if it doesn't belong to any link
            if (ltpType == null) {
                // If provided type is null, decide about the LTP type based on connectivity
                return CarrierEthernetNetworkInterface.Type.UNI;
            } else if (ltpType.equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                // Validate type
                return ltpType;
            } else {
                return null;
            }
        } else {
            // A connect point can be an INNI or ENNI only if it belongs to a link
            if (ltpType == null) {
                // If provided type is null, decide about the LTP type based on connectivity
                return CarrierEthernetNetworkInterface.Type.INNI;
            } else if (ltpType.equals(CarrierEthernetNetworkInterface.Type.INNI) ||
                    ltpType.equals(CarrierEthernetNetworkInterface.Type.ENNI)) {
                // Validate type
                return ltpType;
            } else {
                return null;
            }
        }
    }

    /**
     * Adds a potential LTP and its UNI to the global LTP/UNI maps if it's not already there.
     *
     * @param ltp the potential LTP to add to global LTP map
     * @return the LTP that was added or null if it already existed
     * */
    public CarrierEthernetLogicalTerminationPoint addGlobalLtp(CarrierEthernetLogicalTerminationPoint ltp) {
        // If LTP contains a UNI, add it only if it's not already there, else point to the existing UNI
        if (ltp.ni() != null && ltp.ni() instanceof CarrierEthernetUni) {
            if (!uniMap.containsKey(ltp.ni().id())) {
                uniMap.put(ltp.ni().id(), (CarrierEthernetUni) ltp.ni());
            } else {
                ltp.setNi(uniMap.get(ltp.ni().id()));
            }
        }
        // Add LTP only if it's not already there
        if (!ltpMap.containsKey(ltp.id())) {
            ltpMap.put(ltp.id(), ltp);
            return ltp;
        } else {
            return null;
        }
    }

}
