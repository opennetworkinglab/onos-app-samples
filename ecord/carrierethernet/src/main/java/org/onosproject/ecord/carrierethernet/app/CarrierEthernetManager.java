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

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;

import org.onlab.util.Bandwidth;
import org.onosproject.ecord.metro.api.MetroConnectivityId;
import org.onosproject.ecord.metro.api.MetroPathEvent;
import org.onosproject.ecord.metro.api.MetroPathListener;
import org.onosproject.ecord.metro.api.MetroPathService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
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
    protected MetroPathService metroPathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CarrierEthernetPacketProvisioner cePktProvisioner;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    private final List<ConfigFactory<?, ?>> factories = ImmutableList.of(
            new ConfigFactory<ConnectPoint, PortVlanConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                    PortVlanConfig.class, PortVlanConfig.CONFIG_KEY) {
                @Override
                public PortVlanConfig createConfig() {
                    return new PortVlanConfig();
                }
            });

    private MetroPathListener metroEventListener = new MetroEventListener();
    private NetworkConfigListener netcfgListener = new InternalNetworkConfigListener();

    // Keeps track of the next S-VLAN tag the app will try to use
    private static short nextVlanId = 1;

    private static final int METRO_CONNECT_TIMEOUT_MILLIS = 7000;

    // If set to false, the setup of optical connectivity using the metro app is bypassed
    private static final boolean PACKET_OPTICAL_TOPO = false;

    // TODO: Implement distributed store for EVCs
    // The installed EVCs
    private final Map<String, CarrierEthernetVirtualConnection> evcMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for Forwarding Constructs
    // The installed Forwarding Constructs
    private final Map<String, CarrierEthernetForwardingConstruct> fcMap = new ConcurrentHashMap<>();

    // TODO: Refactor this part
    private final Map<MetroConnectivityId, MetroPathEvent.Type> metroConnectStatusMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for CE UNIs
    // The installed CE UNIs
    private final Map<String, CarrierEthernetUni> uniMap = new ConcurrentHashMap<>();

    // Map of connect points and corresponding VLAN tag
    private Map<ConnectPoint, VlanId> portVlanMap = new ConcurrentHashMap<>();
    // TODO: Implement distributed store for CE LTPs
    // The installed CE LTPs
    private final Map<String, CarrierEthernetLogicalTerminationPoint> ltpMap = new ConcurrentHashMap<>();

    /**
     * Activate this component.
     */
    @Activate
    public void activate() {
        factories.forEach(cfgRegistry::registerConfigFactory);
        networkConfigService.addListener(netcfgListener);
        metroPathService.addListener(metroEventListener);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    public void deactivate() {
        removeAllEvcs();
        removeAllFcs();
        metroPathService.removeListener(metroEventListener);
        networkConfigService.removeListener(netcfgListener);

        factories.forEach(cfgRegistry::unregisterConfigFactory);
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
     * Get the map containing all installed EVCs
     *
     * @return the EVC map
     */
    public Map<String, CarrierEthernetVirtualConnection> getEvcMap() {
        return evcMap;
    }

    /**
     * Get the map containing all global LTPs
     *
     * @return the global LTP map
     */
    public Map<String, CarrierEthernetLogicalTerminationPoint> getLtpMap() {
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
        boolean isVirtual = false;
        Iterator<CarrierEthernetUni> it = evc.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
            if (uni.ceVlanId() == null && isVirtual) {
                log.error("Could not validate the virtual status of the EVC.");
                return null;
            } else if (uni.ceVlanId() != null){
                isVirtual = true;
            }
        }
        evc.setIsVirtual(isVirtual);

        // Set unique id for the EVC unless the EVC is being updated
        if (evc.id() == null) {
            evc.setId(generateEvcId(evc));
        }

        Set<CarrierEthernetUni> validatedUniSet = new HashSet<>();

        // Check the UNIs of the EVC, possibly removing UNIs that are incompatible with existing ones
        it = evc.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
            // Change the name of the UNI's BWP to the EVC name if it is an EVC BWP
            if (uni.bwp().type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                uni.bwp().setId(evc.id());
            }
            // Check first if UNI already exists by checking against the global UNI Map
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
        evc.setUniSet(validatedUniSet);

        if (evc.uniSet().size() > evc.maxNumUni()) {
            log.error("{} EVC can have at most {} UNIs.", evc.maxNumUni());
            return null;
        }

        if ((evc.type().equals(CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT)
                || evc.type().equals(CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT))
                && (evc.uniSet().size() < 2)) {
            log.error("{} EVC requires at least two UNIs.", evc.type().name());
            return null;
        }

        if (evc.type().equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT) && (evc.uniSet().size() != 2)) {
            log.error("{} EVC requires exactly two UNIs.", evc.type().name());
            return null;
        }

        return evc;
    }

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

        validateEvc(evc);

        if (evc == null) {
            log.error("EVC could not be installed, please check log for details.");
            return null;
        }

        boolean allPairsConnected = true;

        // Temporary set for iterating through EVC UNI pairs
        Set<CarrierEthernetUni> uniSet = new HashSet<>(evc.uniSet());

        // Temporary set for indicating which UNIs were finally included in the EVC
        Set<CarrierEthernetUni> usedUniSet = new HashSet<>();

        Iterator<CarrierEthernetUni> uniIt1 = uniSet.iterator();
        while (uniIt1.hasNext()) {

            CarrierEthernetUni uni1 = uniIt1.next();

            // Iterate through all the remaining UNIs
            Iterator<CarrierEthernetUni> uniIt2 = uniSet.iterator();
            while (uniIt2.hasNext()) {

                CarrierEthernetUni uni2 = uniIt2.next();

                // Skip equals
                if (uni1.equals(uni2)) {
                    continue;
                }

                // Do not establish connectivity between leaf UNIs (applies to Rooted_Multipoint)
                if (uni1.role() == CarrierEthernetUni.Role.LEAF && uni2.role() == CarrierEthernetUni.Role.LEAF) {
                    continue;
                }

                MetroConnectivityId metroConnectId = null;

                if (PACKET_OPTICAL_TOPO) {
                    metroConnectId = setupMetroConnectivity(uni1.cp(), uni2.cp(), uni1.bwp().cir(), evc.latency());

                    if (metroConnectId == null ||
                            metroConnectStatusMap.get(metroConnectId) != MetroPathEvent.Type.PATH_INSTALLED) {
                        log.error("Could not establish metro connectivity between {} and {}" +
                                        " (metro id and status: {}, {})", uni1.cp(), uni2.cp(), metroConnectId,
                                (metroConnectId == null ? "null" : metroConnectStatusMap.get(metroConnectId)));
                        allPairsConnected = false;
                        continue;
                    }

                    if (metroConnectId != null) {
                        evc.setMetroConnectivityId(metroConnectId);
                        evc.setMetroConnectivityStatus(metroConnectStatusMap.get(metroConnectId));
                    }

                    log.info("Metro connectivity id and status for EVC {}: {}, {}", evc.id(),
                            evc.metroConnectivity().id(), evc.metroConnectivity().status());

                    if (metroConnectId != null) {
                        // TODO: find vlanIds for both CO and store to service
                        Optional<VlanId> vlanId = getVlanTag(metroPathService.getPath(metroConnectId));
                        if (vlanId.isPresent()) {
                            log.info("VLAN ID {} is assigned to CE service {}", vlanId.get(), evc.id());
                            evc.setVlanId(vlanId.get());
                        }
                    }
                }

                if (!cePktProvisioner.setupConnectivity(uni1, uni2, evc)) {
                    log.warn("Could not set up packet connectivity between {} and {}", uni1, uni2);
                    removeMetroConnectivity(metroConnectId);
                    allPairsConnected = false;
                    continue;
                }

                // Indicate that connection for at least one UNI pair has been established
                evc.setState(CarrierEthernetVirtualConnection.State.ACTIVE);

                // Add UNIs to the set of UNIs used by the EVC
                usedUniSet.add(uni1);
                usedUniSet.add(uni2);
            }
            // Remove UNI from temporary set so that each pair is visited only once
            uniIt1.remove();
        }

        // Update the EVC UNI set, based on the UNIs actually used
        evc.setUniSet(usedUniSet);

        // If no pair was connected, do not register the EVC
        if (evc.state().equals(CarrierEthernetVirtualConnection.State.ACTIVE)) {
            evcMap.put(evc.id(), evc);
            cePktProvisioner.applyBandwidthProfiles(evc);
            // Apply the BWPs of the EVC UNI to the global UNIs, creating them if needed
            applyBandwidthProfiles(evc.uniSet());
        }

        if (evc.state().equals(CarrierEthernetVirtualConnection.State.ACTIVE)) {
            if (allPairsConnected) {
                evc.setActiveState(CarrierEthernetVirtualConnection.ActiveState.FULL);
            } else {
                evc.setActiveState(CarrierEthernetVirtualConnection.ActiveState.PARTIAL);
            }
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
     * Applies bandwidth profiles to the UNIs of an EVC and adds them if needed to the global UNI map.
     *
     * @param  uniSet set of UNIs that are included in the EVC
     */
    private void applyBandwidthProfiles(Set<CarrierEthernetUni> uniSet) {

        uniSet.forEach(uni -> {
            if (!(uniMap.keySet().contains(uni.id()))) {
                // Just add the UNI as it appears at the EVC
                uniMap.put(uni.id(), uni);
            } else {
                // Add UNI resources (BWP, CE-VLAN ID) to existing global UNI
                CarrierEthernetUni newUni = uniMap.get(uni.id());
                newUni.addEvcUni(uni);
                // Update config identifier
                newUni.setCfgId(uni.cfgId());
                uniMap.put(uni.id(), newUni);
            }
        });
    }

    /**
     * Removes bandwidth profiles from the UNIs of an EVC and removes them if needed from the global UNI map.
     *
     * @param evcId the EVC id
     */
    private void removeBandwidthProfiles(String evcId) {

        evcMap.get(evcId).uniSet().forEach(uni -> {
            // TODO: Check if the bandwidth profile really needs to be removed (e.g. may be CoS)
            cePktProvisioner.removeBandwidthProfiles(evcMap.get(evcId));

            // Remove UNI resources (BWP, CE-VLAN ID) from global UNI
            CarrierEthernetUni newUni = uniMap.get(uni.id());
            newUni.removeEvcUni(uni);
            uniMap.put(uni.id(), newUni);
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
            cePktProvisioner.removeConnectivity(evc);
            cePktProvisioner.removeBandwidthProfiles(evc);
            removeMetroConnectivity(evc.metroConnectivity().id());
            removeBandwidthProfiles(evcId);
            // Avoid excessively incrementing VLAN ids
            nextVlanId = evc.vlanId().toShort();
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
            cePktProvisioner.removeConnectivity(evc);
            cePktProvisioner.removeBandwidthProfiles(evc);
            removeMetroConnectivity(evc.metroConnectivity().id());
            removeBandwidthProfiles(evcId);
            // Avoid excessively incrementing VLAN ids
            nextVlanId = evc.vlanId().toShort();
            evcMap.remove(evcId);
        }
    }

    // FIXME: Rethink this approach
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
        Iterator<CarrierEthernetUni> it = fc.evcLite().uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
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
        it = fc.evcLite().uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
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
            if ((ltp.ni() instanceof CarrierEthernetUni) && (!validatedUniSet.contains(ltp.ni()))) {
                continue;
            }
            validatedLtpSet.add(ltp);
        }
        fc.setLtpSet(validatedLtpSet);

        return fc;
    }

    // FIXME: Rethink this approach
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

        validateFc(fc);

        if (fc == null) {
            log.error("FC could not be installed, please check log for details.");
            return null;
        }

        boolean allPairsConnected = true;

        // Temporary set for iterating through FC NI pairs

        Set<CarrierEthernetNetworkInterface> niSet = new HashSet<>();
        fc.ltpSet().forEach(ltp -> {
            niSet.add(ltp.ni());
        });

        // Temporary set for indicating which NIs were finally included in the FC
        Set<CarrierEthernetNetworkInterface> usedNiSet = new HashSet<>();

        Iterator<CarrierEthernetNetworkInterface> it1 = niSet.iterator();
        while (it1.hasNext()) {

            CarrierEthernetNetworkInterface ni1 = it1.next();

            // Iterate through all the remaining UNIs
            Iterator<CarrierEthernetNetworkInterface> it2 = niSet.iterator();
            while (it2.hasNext()) {

                CarrierEthernetNetworkInterface ni2 = it2.next();

                // Skip equals
                if (ni1.equals(ni2)) {
                    continue;
                }

                // Do not establish connectivity between leaf UNIs (applies to Rooted_Multipoint)
                if (ni1.role() == CarrierEthernetUni.Role.LEAF && ni2.role() == CarrierEthernetUni.Role.LEAF) {
                    continue;
                }

                if (!cePktProvisioner.setupConnectivity(ni1, ni2, fc.evcLite())) {
                    log.warn("Could not set up packet connectivity between {} and {}", ni1, ni2);
                    allPairsConnected = false;
                    continue;
                }

                // Indicate that connection for at least one LTP pair has been established
                fc.setState(CarrierEthernetForwardingConstruct.State.ACTIVE);

                // Add UNIs to the set of UNIs used by the EVC
                usedNiSet.add(ni1);
                usedNiSet.add(ni2);
            }
            // Remove NI from temporary set so that each pair is visited only once
            it1.remove();
        }

        // Update the FC LTP set, based on the NIs actually used
        Set<CarrierEthernetLogicalTerminationPoint> usedLtpSet = new HashSet<>();
        fc.ltpSet().forEach(ltp -> {
            if (usedNiSet.contains(ltp.ni())) {
                usedLtpSet.add(ltp);
            }
        });
        fc.setLtpSet(usedLtpSet);

        // If no pair was connected, do not register the FC
        if (fc.state().equals(CarrierEthernetForwardingConstruct.State.ACTIVE)) {
            fcMap.put(fc.id(), fc);
            evcMap.put(fc.evcLite().id(), fc.evcLite());
            cePktProvisioner.applyBandwidthProfiles(fc.evcLite());
            // Apply the BWPs of the EVC UNI to the global UNIs, creating them if needed
            applyBandwidthProfiles(fc.evcLite().uniSet());
        }

        if (fc.state().equals(CarrierEthernetForwardingConstruct.State.ACTIVE)) {
            if (allPairsConnected) {
                fc.setActiveState(CarrierEthernetForwardingConstruct.ActiveState.FULL);
            } else {
                fc.setActiveState(CarrierEthernetForwardingConstruct.ActiveState.PARTIAL);
            }
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
        });
        fcMap.clear();
    }

    // FIXME: Rethink this approach
    /**
     * Removes all resources associated with a specific FC.
     *
     * @param fcId the FC id
     * */
    public void removeFc(String fcId) {
        if (fcMap.containsKey(fcId)) {
            removeEvc(fcMap.get(fcId).evcLite().id());
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
     * Returns all potential UNIs.
     *
     * @return set of all potential UNIs
     * */
    public Set<CarrierEthernetUni> getGlobalUnis() {

        CarrierEthernetUni uni;
        Set<CarrierEthernetUni> uniSet = new HashSet<>();
        // Generate the device ID/port number identifiers
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                // Consider only physical ports which are currently active
                if (!port.number().isLogical() && port.isEnabled()) {
                    String cpString = device.id().toString() + "/" + port.number();
                    ConnectPoint cp = ConnectPoint.deviceConnectPoint(cpString);
                    // Add the UNI associated with generated connect point only if it doesn't belong to any link
                    // and only if it belongs to a packet switch
                    if (linkService.getEgressLinks(cp).isEmpty() && linkService.getIngressLinks(cp).isEmpty() &&
                            device.type().equals(Device.Type.SWITCH)) {
                        uni = new CarrierEthernetUni(cp, cpString, null, null, null);
                        uniSet.add(uni);
                    }
                }
            }
        }
        return uniSet;
    }

    /**
     * Adds a set of potential UNIs to the global UNI map if they are not already there.
     *
     * @param uniSet set of potential UNIs to add to global UNI map
     * */
    public void addGlobalUnis(Set<CarrierEthernetUni> uniSet) {
        uniSet.forEach(uni -> {
            // Add UNI only if it's not already there
            if (!uniMap.containsKey(uni.id())) {
                uniMap.put(uni.id(), uni);
            }
        });
    }

    /**
     * Returns all potential LTPs.
     *
     * @return set of all potential LTPs
     * */
    public Set<CarrierEthernetLogicalTerminationPoint> getGlobalLtps() {

        CarrierEthernetLogicalTerminationPoint ltp;
        CarrierEthernetUni uni;
        CarrierEthernetInni inni;
        Set<CarrierEthernetLogicalTerminationPoint> ltpSet = new HashSet<>();
        // Generate the device ID/port number identifiers
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                // Consider only physical ports which are currently active
                if (!port.number().isLogical() && port.isEnabled()) {
                    String cpString = device.id().toString() + "/" + port.number();
                    ConnectPoint cp = ConnectPoint.deviceConnectPoint(cpString);
                    // Add LTP associated with generated connect point only if it belongs to a packet switch
                    if (device.type().equals(Device.Type.SWITCH)) {
                        // Add a UNI associated with generated connect point only if it doesn't belong to any link
                        if (linkService.getEgressLinks(cp).isEmpty() && linkService.getIngressLinks(cp).isEmpty()) {
                            uni = new CarrierEthernetUni(cp, cpString, null, null, null);
                            ltp = new CarrierEthernetLogicalTerminationPoint(cpString, uni);
                        } else {
                            inni = new CarrierEthernetInni(cp, cpString, null, null, null, null);
                            ltp = new CarrierEthernetLogicalTerminationPoint(cpString, inni);
                        }
                        ltpSet.add(ltp);
                    }
                }
            }
        }
        return ltpSet;
    }

    /**
     * Adds a set of potential LTPs and their UNIs to the global LTP/UNI maps if they are not already there.
     *
     * @param ltpSet set of potential LTPs to add to global LTP map
     * */
    public void addGlobalLtps(Set<CarrierEthernetLogicalTerminationPoint> ltpSet) {
        ltpSet.forEach(ltp -> {
            // Add LTP only if it's not already there
            if (!ltpMap.containsKey(ltp.id())) {
                ltpMap.put(ltp.id(), ltp);
            }
            // If LTP contains a UNI, add it only if it's not already there
            if (ltp.ni() != null && ltp.ni() instanceof CarrierEthernetUni) {
                if (!uniMap.containsKey(ltp.ni().id())) {
                    uniMap.put(ltp.ni().id(), (CarrierEthernetUni) ltp.ni());
                }
            }
        });
    }

    /**
     * Returns VLAN tag assigned to given path.
     * @param links List of links that composes path
     * @return VLAN tag if found any. empty if not found.
     */
    private Optional<VlanId> getVlanTag(List<Link> links) {
        checkNotNull(links);
        Optional<ConnectPoint> edge = links.stream().flatMap(l -> Stream.of(l.src(), l.dst()))
                .filter(portVlanMap::containsKey)
                .findAny();

        if (edge.isPresent()) {
            return Optional.of(portVlanMap.get(edge.get()));
        }

        return Optional.empty();
    }

    private class MetroEventListener implements MetroPathListener {

        @Override
        public void event(MetroPathEvent event) {
            switch (event.type()) {
                case PATH_INSTALLED: case PATH_REMOVED:
                    log.info("Metro path event {} received for {}.", event.type(), event.subject());
                    metroConnectStatusMap.put(event.subject(), event.type());
                    break;
                default:
                    log.error("Unexpected metro event type.");
                    break;
            }
        }
    }

    private MetroConnectivityId setupMetroConnectivity(ConnectPoint ingress, ConnectPoint egress,
                                                       Bandwidth bandwidth, Duration latency) {
        MetroConnectivityId metroConnectId = metroPathService.setupConnectivity(ingress, egress, bandwidth, latency);
        if (metroConnectId != null) {
            long startTime = System.currentTimeMillis();
            while (((System.currentTimeMillis() - startTime) < (long) METRO_CONNECT_TIMEOUT_MILLIS) &&
                    (metroConnectStatusMap.get(metroConnectId) != MetroPathEvent.Type.PATH_INSTALLED)) {
            }
        }
        return metroConnectId;
    }

    private void removeMetroConnectivity(MetroConnectivityId metroConnectId) {
        if (metroConnectId != null) {
            metroPathService.removeConnectivity(metroConnectId);
            long startTime = System.currentTimeMillis();
            while (((System.currentTimeMillis() - startTime) < (long) METRO_CONNECT_TIMEOUT_MILLIS) &&
                    (metroConnectStatusMap.get(metroConnectId) != MetroPathEvent.Type.PATH_REMOVED)) {
            }
        }
    }


    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(PortVlanConfig.class)) {
                return;
            }

            ConnectPoint cp = (ConnectPoint) event.subject();
            PortVlanConfig config = networkConfigService.getConfig(cp, PortVlanConfig.class);
            if (config != null && config.portVlanId().isPresent()) {
                log.info("VLAN tag {} is assigned to port {}", config.portVlanId().get(), cp);
                portVlanMap.put(cp, config.portVlanId().get());
            } else {
                log.info("VLAN tag is removed from port {}", cp);
                portVlanMap.remove(cp);
            }
        }

    }
}
