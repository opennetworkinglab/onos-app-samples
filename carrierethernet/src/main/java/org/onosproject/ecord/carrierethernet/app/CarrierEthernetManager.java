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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;

import org.onosproject.ecord.carrierethernet.api.CarrierEthernetProvisionerService;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a Carrier Ethernet Manager.
 */
@Component(immediate = true)
@Service
public class CarrierEthernetManager implements CarrierEthernetService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CarrierEthernetProvisionerService ceProvisioner;

    // Keeps track of the next S-VLAN tag the app will try to use
    private static short nextVlanId = 1;

    // Keeps track of the next EVC id the app will try to use
    // TODO: Use Identifier class instead
    private static short nextEvcShortId = 1;

    private boolean evcFragmentationEnabled = false;
    private boolean prevEvcFragmentationStatus = evcFragmentationEnabled;

    // TODO: Implement distributed store for EVCs
    // The installed EVCs
    private final Map<String, CarrierEthernetVirtualConnection> evcMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for Forwarding Constructs
    // The installed Forwarding Constructs
    private final Map<String, CarrierEthernetForwardingConstruct> fcMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for CE UNIs
    // The installed CE UNIs
    private final Map<String, CarrierEthernetUni> uniMap = new ConcurrentHashMap<>();
    private final Set<String> removedUniSet = Sets.newConcurrentHashSet();

    // TODO: Implement distributed store for CE LTPs
    // The installed CE LTPs
    private final Map<String, CarrierEthernetLogicalTerminationPoint> ltpMap = new ConcurrentHashMap<>();

    // The LTP ids that have been explicitly removed (or requested to be removed) from the global LTP map
    private final Set<String> removedLtpSet = Sets.newConcurrentHashSet();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgRegistry;

    private final List<ConfigFactory<?, ?>> factories = ImmutableList.of(
            new ConfigFactory<ConnectPoint, PortVlanConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                                                            PortVlanConfig.class, PortVlanConfig.CONFIG_KEY) {
                @Override
                public PortVlanConfig createConfig() {
                    return new PortVlanConfig();
                }
            });

    // Map of connect points and corresponding VLAN tag
    private final Map<ConnectPoint, VlanId> portVlanMap = new ConcurrentHashMap<>();

    private NetworkConfigListener netcfgListener = new InternalNetworkConfigListener();

    /**
     * Activate this component.
     */
    @Activate
    public void activate() {
        networkConfigService.addListener(netcfgListener);
        factories.forEach(cfgRegistry::registerConfigFactory);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    public void deactivate() {
        networkConfigService.removeListener(netcfgListener);
        factories.forEach(cfgRegistry::unregisterConfigFactory);
        removeAllEvcs();
        removeAllFcs();
    }

    @Override
    public Map<String, CarrierEthernetVirtualConnection> evcMap() {
        return this.evcMap;
    }

    @Override
    public CarrierEthernetVirtualConnection getEvc(String evcId) {
        return ((evcMap.get(evcId) == null) ? null : evcMap.get(evcId));
    }

    @Override
    public CarrierEthernetForwardingConstruct getFc(String fcId) {
        return ((fcMap.get(fcId) == null) ? null : fcMap.get(fcId));
    }

    @Override
    public Map<String, CarrierEthernetForwardingConstruct> fcMap() {
        return fcMap;
    }

    @Override
    public Map<String, CarrierEthernetLogicalTerminationPoint> ltpMap() {
        return ltpMap;
    }

    @Override
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

        // Try to set a unique numerical id for the EVC unless the EVC is being updated
        // FIXME: Check again the EVC update case
        evc.setShortId(generateEvcShortId());
        if (evc.shortId()  == null) {
            log.error("No available EVC id found.");
            return null;
        }

        // Generate and set unique FC id
        evc.setId(generateEvcId(evc));

        // Verify that CE-VLAN ID is provided to either all UNIs or none
        // and set the virtualEvc flag accordingly
        // Note: Checking also that all NIs are UNIs
        boolean isVirtual = false;
        Iterator<CarrierEthernetUni> it = evc.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni ni = it.next();
            if (ni.ceVlanId() == VlanId.NONE && isVirtual) {
                log.error("Could not validate the virtual status of the EVC.");
                return null;
            } else if (ni.ceVlanId() != VlanId.NONE) {
                isVirtual = true;
            }
        }
        evc.setIsVirtual(isVirtual);

        // Set unique id for the EVC unless the EVC is being updated
        if (evc.id() == null) {
            evc.setId(generateEvcId(evc));
        }

        Set<CarrierEthernetUni> validatedUniSet = new HashSet<>();

        // TODO: Refactor according to the validateFc method
        // Note: Cannot use the validateFc method here,
        // because FCs can also be standalone

        // Check the UNIs of the EVC, possibly removing UNIs that are
        // incompatible with existing global ones
        it = evc.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
            // Change the name of the UNI's BWP to the EVC name if it is an EVC BWP
            if (uni.bwp().type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                uni.bwp().setId(evc.id());
            }
            // Check first if corresponding global UNI already exists
            // by checking against the global UNI Map
            if (uniMap.keySet().contains(uni.id())) {
                CarrierEthernetUni existingUni = uniMap.get(uni.id());
                // Check if the EVC-specific UNI is compatible with the global one
                if (!(existingUni.validateEcNi(uni))) {
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

        // TODO: Check that an ROOT_MULTIPOINT EVC has at most one ROOT

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

    @Override
    public CarrierEthernetVirtualConnection installEvc(CarrierEthernetVirtualConnection evc) {

        // If EVC already exists, remove it and reestablish with new parameters
        if (evc.id() != null && evcMap.containsKey(evc.id())) {
            return updateEvc(evc);
        } else {
            // id will be generated during validation below
            evc.setId(null);
        }

        if (validateEvc(evc) == null) {
            log.error("EVC could not be installed, please check log for details.");
            return null;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////
        // This is the "orchestration" part of the CE app
        //////////////////////////////////////////////////////////////////////////////////////////////////

        // TODO: Add configurable parameter to determine if fragmentation will take place
        if (evcFragmentationEnabled) {
            evc.setFcSet(fragmentEvc(evc));
        } else {
            evc.setFcSet(Collections.singleton(fcFromEvc(evc)));
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////

        // Assign S-TAGs to FCs
        // If network configuration is there, get tags from corresponding ports
        // else generate unique tags to be used
        // FIXME: This was supposed to be done in the validateFc method
        // FIXME: but we need a vlanId here already, so that S-TAGs can be assigned below among paired INNIs/ENNIs
        Set<VlanId> excludedVlans = usedVlans();
        evc.fcSet().forEach(fc -> {
            Optional<VlanId> cfgVlanId = getCfgVlan(fc);
            if (cfgVlanId.isPresent()) {
                fc.setVlanId(cfgVlanId.get());
            } else {
                fc.setVlanId(generateVlanId(excludedVlans));
            }
            excludedVlans.add(fc.vlanId());
        });

        // For each INNI/ENNI of each FC, find the paired INNI/ENNI and assign S-TAG according to the other FC's vlanId
        for (CarrierEthernetForwardingConstruct fc : evc.fcSet()) {
            for (CarrierEthernetLogicalTerminationPoint ltp : fc.ltpSet()) {
                if (!ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                    // Find the cp at the other end of the link
                    Link link = linkService.getEgressLinks(ltp.ni().cp()).iterator().next();
                    String ltpId = link.dst().deviceId().toString() + "/" + link.dst().port().toString();
                    // Find the corresponding FC - assuming LTP ids are the same as connect point ids
                    CarrierEthernetForwardingConstruct neighborFc = getFcFromLtpId(ltpId, evc.fcSet());
                    if (neighborFc != null) {
                        if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.INNI)) {
                            ((CarrierEthernetInni) ltp.ni()).setSVlanId(neighborFc.vlanId());
                        } else if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.ENNI)) {
                            ((CarrierEthernetEnni) ltp.ni()).setSVlanId(neighborFc.vlanId());
                        }
                    }
                }
            }
        }

        // Install the constituent FCs
        evc.fcSet().forEach(fc -> {
            // Increment the FC refCount
            fc.refCount().incrementAndGet();
            installFc(fc);
        });

        // Update the EVC UNI set based on the LTPs used during FC connectivity
        Set<CarrierEthernetUni> usedUniSet = new HashSet<>();
        evc.fcSet().forEach(fc -> usedUniSet.addAll(fc.uniSet()));
        evc.setUniSet(usedUniSet);

        // Determine EVC state based on the state of the constituent FCs
        evc.setState(CarrierEthernetVirtualConnection.State.ACTIVE);
        Iterator<CarrierEthernetForwardingConstruct> fcIt = evc.fcSet().iterator();
        while (fcIt.hasNext()) {
            CarrierEthernetForwardingConstruct fc = fcIt.next();
            evc.setState(CarrierEthernetVirtualConnection.State.valueOf(fc.state().name()));
            if (!evc.isActive()) {
                break;
            }
        }

        if (evc.isActive()) {
            // If EVC installation was successful, then register the EVC
            evcMap.put(evc.id(), evc);
        } else {
            // If EVC installation was not successful, then do not register the EVC and rollback FC installations
            evc.fcSet().forEach(fc -> {
                // Decrement the FC refCount to make removal possible
                fc.refCount().decrementAndGet();
                removeFc(fc.id());
            });
        }

        return evc;
    }

    /**
     * Creates a single FC out of an EVC.
     *
     * @param evc the EVC representation
     * @return the equivalent FC
     */
    private CarrierEthernetForwardingConstruct fcFromEvc(CarrierEthernetVirtualConnection evc) {
        Set<CarrierEthernetLogicalTerminationPoint> ltpSet = new HashSet<>();
        evc.uniSet().forEach(uni -> ltpSet.add(new CarrierEthernetLogicalTerminationPoint(null, uni)));
        return CarrierEthernetForwardingConstruct.builder()
                .type(evc.type())
                .ltpSet(ltpSet)
                .build();
    }

    /**
     * Fragments an EVC into multiple FCs.
     *
     * @param evc the EVC representation
     * @return the set of FCs constituting the EVC
     */
    private Set<CarrierEthernetForwardingConstruct> fragmentEvc(CarrierEthernetVirtualConnection evc) {

        Set<CarrierEthernetForwardingConstruct> fcSet = new HashSet<>();

        // Each LTP can only belong to a single FC, hence using LTP_id -> LTP_set map
        Map<String, Set<CarrierEthernetLogicalTerminationPoint>> ltpSetMap = new HashMap<>();

        // Temporary set to browse through all EVC UNI pairs
        Set<CarrierEthernetUni> tempUniSet = new HashSet<>(evc.uniSet());

        Iterator<CarrierEthernetUni> uniIt1 = tempUniSet.iterator();
        while (uniIt1.hasNext()) {

            CarrierEthernetUni uni1 = uniIt1.next();

            // Iterate through all the remaining NIs
            Iterator<CarrierEthernetUni> uniIt2 = tempUniSet.iterator();
            while (uniIt2.hasNext()) {

                CarrierEthernetUni uni2 = uniIt2.next();

                // Skip equals
                if (uni1.equals(uni2)) {
                    continue;
                }

                // Do not establish connectivity between leaf NIs
                // (applies to Rooted_Multipoint)
                if (uni1.role().equals(CarrierEthernetUni.Role.LEAF)
                        && uni2.role().equals(CarrierEthernetUni.Role.LEAF)) {
                    continue;
                }

                // Note: INNIs should always appear in pairs
                List<Pair<CarrierEthernetLogicalTerminationPoint,
                        CarrierEthernetLogicalTerminationPoint>> ltpPairList
                        = new ArrayList<>();

                // If uni1 and uni2 are on same device, skip path calculation
                // and directly generate a single LTP pair to be used below
                if (uni1.cp().deviceId().equals(uni2.cp().deviceId())) {
                    ltpPairList.add(Pair.of(new CarrierEthernetLogicalTerminationPoint(null, uni1),
                                            new CarrierEthernetLogicalTerminationPoint(null, uni2)));
                } else {
                    // Calculate path assuming return paths are the same
                    // TODO: Handle the congruent paths case?
                    Set<Path> paths;
                    if (evc.type().equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT)) {
                        // For point-to-point connectivity use the pre-calculated paths
                        // to make sure the shortest paths are chosen
                        paths = pathService.getPaths(uni1.cp().deviceId(), uni2.cp().deviceId());
                    } else {
                        // Recalculate path so that it's over the pre-calculated spanning tree
                        // FIXME: Find a more efficient way (avoid recalculating paths)
                        paths = pathService.getPaths(uni1.cp().deviceId(), uni2.cp().deviceId(),
                                                     new CarrierEthernetSpanningTreeWeight(topologyService));
                    }

                    // Just select any of the returned paths
                    // TODO: Select path in more sophisticated way and return null
                    // if any of the constraints cannot be met
                    Path path = paths.iterator().hasNext() ? paths.iterator().next() : null;

                    if (path == null) {
                        return null;
                    }

                    List<Link> links = new ArrayList<>();
                    links.add(createEdgeLink(uni1.cp(), true));
                    links.addAll(path.links());
                    links.add(createEdgeLink(uni2.cp(), false));

                    ////////////////////////////////////////////////////////////
                    // Get LTP pairs of ingress/egress NIs along the link path
                    // (non-LTP connect points are ignored)
                    ////////////////////////////////////////////////////////////

                    CarrierEthernetLogicalTerminationPoint srcLtp = null, dstLtp = null;
                    // These are the roles that will be used for all pairs found below
                    CarrierEthernetLogicalTerminationPoint.Role srcLtpRole, dstLtpRole;
                    // The source in any pair will always have the same role as the LTP from which the paths starts
                    srcLtpRole = CarrierEthernetLogicalTerminationPoint.Role.valueOf((uni1).role().name());
                    // The destination in any pair will always have the same role as the LTP at which the path ends
                    dstLtpRole = CarrierEthernetLogicalTerminationPoint.Role.valueOf((uni2).role().name());
                    for (int i = 0; i < links.size(); i++) {
                        // Try to get the destination LTP of a pair
                        if (srcLtp != null && i != 0) {
                            // If this is the last, use existing EVC UNI, else create a new FC LTP and set Role
                            dstLtp = (i == links.size() - 1) ?
                                    new CarrierEthernetLogicalTerminationPoint(null, uni2) :
                                    fcLtpFromCp(links.get(i).src(), dstLtpRole);
                        }
                        if (dstLtp != null) {
                            // Create a new LTP pair and null the srcLtp
                            // so that we can continue searching for a new pair
                            ltpPairList.add(Pair.of(srcLtp, dstLtp));
                            srcLtp = null;
                        }
                        // Try to get the source LTP of a pair
                        if (srcLtp == null && i != links.size() - 1) {
                            // If this is the first, use existing EVC UNI, else create a new FC LTP and set Role
                            srcLtp = (i == 0) ?
                                    new CarrierEthernetLogicalTerminationPoint(null, uni1) :
                                    fcLtpFromCp(links.get(i).dst(), srcLtpRole);
                        }
                    }
                }

                ////////////////////////////////////////////////////////////////
                // Go through all the LTP pairs found and map each LTP to a set
                // of LTPs (create it if it doesn't exist)
                ////////////////////////////////////////////////////////////////

                // Note: Each LTP can only belong to a single set, so each set
                // will eventually correspond to an FC

                ltpPairList.forEach(ltpPair -> {
                    CarrierEthernetLogicalTerminationPoint ltp1 = ltpPair.getLeft();
                    CarrierEthernetLogicalTerminationPoint ltp2 = ltpPair.getRight();
                    if (ltpSetMap.containsKey(ltp1.id()) && !ltpSetMap.containsKey(ltp2.id())) {
                        // If one of the LTPs is already contained in a set, add the other one as well in that set
                        ltpSetMap.get(ltp1.id()).add(ltp2);
                        ltpSetMap.put(ltp2.id(), ltpSetMap.get(ltp1.id()));
                    } else if (ltpSetMap.containsKey(ltp2.id()) & !ltpSetMap.containsKey(ltp1.id())) {
                        // If one of the LTPs is already contained in a set, add the other one as well in that set
                        ltpSetMap.get(ltp2.id()).add(ltp1);
                        ltpSetMap.put(ltp1.id(), ltpSetMap.get(ltp2.id()));
                    } else if (!ltpSetMap.containsKey(ltp1.id()) && !ltpSetMap.containsKey(ltp2.id())) {
                        // Create a new LTP set containing the two LTPs and map both to it
                        ltpSetMap.put(ltp1.id(), Sets.newHashSet(ltp1, ltp2));
                        ltpSetMap.put(ltp2.id(), ltpSetMap.get(ltp1.id()));
                    }
                });
            }
            // Remove UNI from temporary set so that each pair is visited only once
            uniIt1.remove();
        }

        //////////////////////////////////////////////////////////////////////////////////
        // Go through all unique LTP sets generated above and create the corresponding FCs
        //////////////////////////////////////////////////////////////////////////////////

        ltpSetMap.values().stream().collect(Collectors.toSet()).forEach(ltpSet -> {
            CarrierEthernetForwardingConstruct.Builder fcBuilder =
                    CarrierEthernetForwardingConstruct.builder().ltpSet(ltpSet);
            // Type is determined by number and type of LTPs in each set
            CarrierEthernetVirtualConnection.Type fcType =
                    ltpSet.size() == 2 ? CarrierEthernetVirtualConnection.Type.POINT_TO_POINT
                            : CarrierEthernetConnection.Type.MULTIPOINT_TO_MULTIPOINT;
            // If one of the LTPs is LEAF, indicate FC as ROOT_MULTIPOINT
            for (CarrierEthernetLogicalTerminationPoint ltp : ltpSet) {
                if (ltp.role().equals(CarrierEthernetLogicalTerminationPoint.Role.LEAF)) {
                    fcType = CarrierEthernetConnection.Type.ROOT_MULTIPOINT;
                    break;
                }
            }
            fcSet.add(fcBuilder.type(fcType).build());
            log.info("Created ForwardingConstruct comprising LogicalTerminationPoints {}",
                     ltpSet.stream()
                             .map(CarrierEthernetLogicalTerminationPoint::id)
                             .collect(Collectors.toList()));
        });

        return fcSet;
    }

    @Override
    public CarrierEthernetVirtualConnection updateEvc(CarrierEthernetVirtualConnection evc) {
        // Just checking again
        if (evcMap.containsKey(evc.id())) {
            log.info("Updating existing EVC {}", evc.id());
            removeEvc(evc.id());
        }
        return installEvc(evc);
    }

    /**
     * Applies FC- specific LTP attributes to global LTPs or adds them to the global LTP map if not there.
     *
     * @param ltpSet set of FC-specific LTPs the attributes of which will be applied to the global LTPs
     */
    private void applyFcToGlobalLtps(Set<CarrierEthernetLogicalTerminationPoint> ltpSet) {
        ltpSet.forEach(ltp -> {
            if (!(ltpMap.keySet().contains(ltp.id()))) {
                // Just add the LTP as it appears at the FC
                addGlobalLtp(ltp);
            } else {
                // Add LTP resources (BWP, CE-VLAN ID, S-TAG) to existing global LTP
                ltpMap.get(ltp.id()).ni().addEcNi(ltp.ni());
                // Update config identifier
                ltpMap.get(ltp.id()).ni().setCfgId(ltp.ni().cfgId());
            }
        });
    }

    /**
     * Removes bandwidth profiles from the UNIs of an FC.
     *
     * @param fc the FC representation
     */
    // TODO: Remove LTPs if needed from the global LTP/UNI map
    private void removeFcFromGlobalLtps(CarrierEthernetForwardingConstruct fc) {
        // TODO: Check if the bandwidth profile really needs to be removed (e.g. may be CoS)
        ceProvisioner.removeBandwidthProfiles(fc);
        // Remove LTP resources (BWP, CE-VLAN ID, S-TAG) from corresponding global LTPs
        fc.ltpSet().forEach(ltp -> ltpMap.get(ltp.id()).ni().removeEcNi(ltp.ni()));
    }

    @Override
    public void removeAllEvcs() {
        evcMap.keySet().forEach(evcId -> removeEvc(evcId));
    }

    @Override
    public void removeEvc(String evcId) {
        if (evcMap.containsKey(evcId)) {
            CarrierEthernetVirtualConnection evc = evcMap.get(evcId);
            evc.fcSet().forEach(fc -> {
                // Decrement the FC refCount to make removal possible
                fc.refCount().decrementAndGet();
                removeFc(fc.id());
            });
            // Avoid excessively incrementing EVC ids
            nextEvcShortId = evc.shortId() < nextEvcShortId ? evc.shortId() : nextEvcShortId;
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
        // FIXME: This is an extra check to be able to generate/set VLAN id for FC before calling installFc
        if (fc.vlanId() == null) {
            fc.setVlanId(generateVlanId(usedVlans()));
        }
        if (fc.vlanId() == null) {
            log.error("No available VLAN id found.");
            return null;
        }

        // Generate and set unique FC id
        fc.setId(generateFcId(fc));

        Set<CarrierEthernetLogicalTerminationPoint> validatedLtpSet = new HashSet<>();

        // Check the NIs of the FC, possibly removing NIs that are incompatible with existing ones
        Iterator<CarrierEthernetLogicalTerminationPoint> ltpIt = fc.ltpSet().iterator();
        while (ltpIt.hasNext()) {
            CarrierEthernetLogicalTerminationPoint ltp = ltpIt.next();
            boolean ltpValidated = true;
            if (ltp.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                CarrierEthernetUni uni = (CarrierEthernetUni) ltp.ni();
                // Change the name of the UNI's BWP to the FC name if it is an EVC BWP
                if (uni.bwp().type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                    // FIXME: Find a way to use the EVC name instead
                    uni.bwp().setId(fc.id());
                }
            }
            // Check first if LTP already exists by checking against the global LTP Map
            if (ltpMap.keySet().contains(ltp.id())) {
                CarrierEthernetNetworkInterface existingNi = ltpMap.get(ltp.id()).ni();
                // Check if the FC-specific NI is compatible with the global one
                if (!(existingNi.validateEcNi(ltp.ni()))) {
                    ltpValidated = false;
                }
            }
            if (!ltpValidated) {
                // If EVC is of ROOT_MULTIPOINT type and we have removed the root, return null
                if (fc.type() == CarrierEthernetForwardingConstruct.Type.ROOT_MULTIPOINT &&
                        ltp.role() == CarrierEthernetLogicalTerminationPoint.Role.ROOT) {
                    log.error("Root LTP could not be added to %s FC.", fc.type().name());
                    return null;
                }
                log.warn("LTP {} could not be added to FC.", ltp.id());
                continue;
            } else {
                // Add LTP to FC description
                validatedLtpSet.add(ltp);
            }
        }

        fc.setLtpSet(validatedLtpSet);

        return fc;
    }

    @Override
    public CarrierEthernetForwardingConstruct installFc(CarrierEthernetForwardingConstruct fc) {

        // If FC already exists, remove it and reestablish with new parameters
        if (fc.id() != null && fcMap.containsKey(fc.id())) {
            return updateFc(fc);
        } else {
            fc.setId(null);
        }

        if (validateFc(fc) == null) {
            log.error("FC could not be installed, please check log for details.");
            return null;
        }

        // Create BW profiles first so that they will be available if needed during the connectivity phase
        ceProvisioner.createBandwidthProfiles(fc);

        ceProvisioner.setupConnectivity(fc);

        // If connectivity was not successful, then do not register the FC and do not apply BW profiles
        // If not, the BW profiles that were created earlier need to be removed
        if (fc.state().equals(CarrierEthernetForwardingConstruct.State.ACTIVE)) {
            // Apply BWP-related resources (e.g. Meters) to the packet switches
            ceProvisioner.applyBandwidthProfiles(fc);
            // Apply the BWPs of the FC UNIs to the global UNIs, creating them if needed
            //applyEvcToGlobalUnis(fc.uniSet());
            applyFcToGlobalLtps(fc.ltpSet());
            // Increment the global LTP and corresponding NI refCount
            fc.ltpSet().forEach(ltp -> ltpMap.get(ltp.id()).refCount().incrementAndGet());
            fcMap.put(fc.id(), fc);
        } else {
            ceProvisioner.removeBandwidthProfiles(fc);
        }

        return fc;
    }

    @Override
    public CarrierEthernetForwardingConstruct updateFc(CarrierEthernetForwardingConstruct fc) {
        // Just checking again
        if (fcMap.containsKey(fc.id())) {
            log.info("Updating existing FC {}", fc.id());
            // Keep the VLAN ID of the original FC
            fc.setVlanId(fcMap.get(fc.id()).vlanId());
            // FIXME: Currently FC update only possible for standalone FCs
            removeFc(fc.id());
        }
        return installFc(fc);
    }

    @Override
    public void removeAllFcs() {
        fcMap.keySet().forEach(fcId -> removeFc(fcId));
    }

    @Override
    public CarrierEthernetForwardingConstruct removeFc(String fcId) {
        if (fcMap.containsKey(fcId)) {
            CarrierEthernetForwardingConstruct fc = fcMap.get(fcId);
            if (fc.refCount().get() != 0) {
                log.warn("Could not remove FC {}: RefCount is not zero", fc.id());
                return null;
            }
            ceProvisioner.removeConnectivity(fc);
            ceProvisioner.removeBandwidthProfiles(fc);
            removeFcFromGlobalLtps(fc);
            // Avoid excessively incrementing FC VLAN ids
            nextVlanId = (fcMap.get(fcId).vlanId().toShort() < nextVlanId ?
                                       fcMap.get(fcId).vlanId().toShort() :
                                       nextVlanId);
            // Decrement the global LTP and corresponding NI refCount
            fcMap.get(fcId).ltpSet().forEach(ltp -> ltpMap.get(ltp.id()).refCount().decrementAndGet());
            fcMap.remove(fcId);
            return fc;
        }
        return null;
    }

    /**
     * Returns the unique S-TAGs currently used by FCs across the CE network.
     *
     * @return the S-TAGs currently used
     */
    private Set<VlanId> usedVlans() {
        return fcMap.values().stream().map(CarrierEthernetForwardingConstruct::vlanId)
                .collect(Collectors.toSet());
    }

    /**
     * Generates a new vlanId excluding the provided ones.
     *
     * @param excludedVlans the vlanIds that are not allowed
     * @return the generated vlanId; null if none found
     */
    private VlanId generateVlanId(Set<VlanId> excludedVlans) {
        // If all vlanIds are being used return null, else try to find the next available one
        if (excludedVlans.size() <  VlanId.MAX_VLAN - 1) {
            while (excludedVlans.contains(VlanId.vlanId(nextVlanId))) {
                // Get next valid short
                nextVlanId = (nextVlanId >= VlanId.MAX_VLAN || nextVlanId <= 0 ?
                        1 : (short) (nextVlanId + 1));
            }
        }
        return excludedVlans.contains(VlanId.vlanId(nextVlanId)) ?
                null : VlanId.vlanId(nextVlanId);
    }

    /**
     * Generates a unique vlanId in the context of the CE app.
     *
     * @return the generated vlanId or null if none found
     */
    private Short generateEvcShortId() {

        List<Short> evcShortIdList = evcMap.values()
                .stream()
                .map(evc -> Short.valueOf(evc.shortId()))
                .collect(Collectors.toList());

        // If all vlanIds are being used return null, else try to find the next available one
        if (evcShortIdList.size() <  Short.MAX_VALUE - 1) {
            while (evcShortIdList.contains(nextEvcShortId)) {
                // Get next valid short
                nextEvcShortId =
                        (nextEvcShortId >= Short.MAX_VALUE || nextEvcShortId <= 0 ? 1 : (short) (nextEvcShortId + 1));
            }
        }

        return evcShortIdList.contains(nextEvcShortId) ? null : nextEvcShortId;
    }

    /**
     * Generates a unique EVC id in the context of the CE app.
     *
     * @param evc the EVC representation
     * @return the generated EVC id or null if none found
     */
    private String generateEvcId(CarrierEthernetVirtualConnection evc) {

        // TODO: Add different connectivity types

        String tmpType;

        if (evc.type().equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT)) {
            tmpType = "Line";
        } else if (evc.type().equals(CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT)) {
            tmpType = "LAN";
        } else {
            tmpType = "Tree";
        }

        return "E" + (evc.isVirtual() ? "V" : "") + "P-" + tmpType + "-" +
                evc.shortId().toString();
    }

    /**
     * Generates a unique FC id in the context of the CE app.
     *
     * @param fc the FC representation
     * @return the generated FC id or null if none found
     */
    private String generateFcId(CarrierEthernetForwardingConstruct fc) {

        // TODO: Add different connectivity types

        return "FC-" + fc.vlanId().toString();
    }

    @Override
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
        // Add LTP to removed set
        removedLtpSet.add(ltpId);
        if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            removeGlobalUni(ltp.ni().id());
            // Add UNI to removed set
            // TODO: Check if this is right
            removedUniSet.add(ltp.ni().id());
        }

        // Find cp at other end of link and try to remove both LTPs - assuming LTP ids are the same as connect point ids
        if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.INNI)) {
            Link link = linkService.getEgressLinks(ltp.ni().cp()).iterator().next();
            String pairLtpId = link.dst().deviceId().toString() + "/" + link.dst().port().toString();
            ltpMap.remove(pairLtpId);
            // Add LTP to removed set
            removedLtpSet.add(pairLtpId);
        }

        return ltp;
    }

    @Override
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

        // Add UNI and LTP to removed set
        removedUniSet.add(uniId);
        removedLtpSet.add(uniId);

        return uni;
    }

    @Override
    public Set<CarrierEthernetUni> getUnisFromTopo(boolean excludeAdded, boolean includeRemoved) {

        CarrierEthernetUni uni;
        Set<CarrierEthernetUni> uniSet = new HashSet<>();
        // Generate the device ID/port number identifiers
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                if (!port.number().isLogical()) {
                    String cpString = device.id().toString() + "/" + port.number();
                    ConnectPoint cp = ConnectPoint.deviceConnectPoint(cpString);
                    uni = generateUni(cp);
                    // Check if UNI was generated and whether it's currently removed
                    if (uni != null
                            && (includeRemoved || !removedUniSet.contains(uni.id()))
                            && (!excludeAdded || !uniMap.containsKey(uni.id()))) {
                        uniSet.add(uni);
                    }
                }
            }
        }
        return uniSet;
    }

    @Override
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

        return CarrierEthernetUni.builder()
                .cp(cp)
                .cfgId(uniId)
                .build();
    }

    @Override
    public CarrierEthernetUni addGlobalUni(CarrierEthernetUni uni) {
        // Add UNI only if it's not already there. If corresponding LTP already exists, link them, otherwise create it
        if (!uniMap.containsKey(uni.id())) {
            // Add LTP only if it's not already there
            // FIXME: Assumes LTP and UNI id are the same
            if (!ltpMap.containsKey(uni.id())) {
                ltpMap.put(uni.id(), new CarrierEthernetLogicalTerminationPoint(uni.id(), uni));
                // Remove LTP from deleted set
                removedLtpSet.remove(uni.id());
            }
            uniMap.put(uni.id(), uni);
            // Remove UNI from deleted set
            removedUniSet.remove(uni.id());
            return  uni;
        } else {
            return null;
        }
    }

    @Override
    public Set<CarrierEthernetLogicalTerminationPoint> getLtpsFromTopo(boolean excludeAdded, boolean includeRemoved) {

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
                    if (ltp != null
                            && (includeRemoved || !removedLtpSet.contains(ltp.id()))
                            && (!excludeAdded || !ltpMap.containsKey(ltp.id()))) {
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

    @Override
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

        return new CarrierEthernetLogicalTerminationPoint(cp, ltpId, ltpType, null);
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
     */
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

    @Override
    public CarrierEthernetLogicalTerminationPoint addGlobalLtp(CarrierEthernetLogicalTerminationPoint ltp) {
        // If LTP contains a UNI, add it only if it's not already there, else point to the existing UNI
        // FIXME: Assumes LTP and UNI id are the same
        if (ltp.ni() != null && ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            if (!uniMap.containsKey(ltp.ni().id())) {
                uniMap.put(ltp.ni().id(), (CarrierEthernetUni) ltp.ni());
                // Remove UNI from deleted set
                removedUniSet.remove(ltp.id());
            } else {
                ltp.setNi(uniMap.get(ltp.ni().id()));
            }
        }
        // Add LTP only if it's not already there
        if (!ltpMap.containsKey(ltp.id())) {
            // Try to create and add INNI LTP at other end of link as well
            if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.INNI)) {
                Link link = linkService.getEgressLinks(ltp.ni().cp()).iterator().next();
                CarrierEthernetLogicalTerminationPoint pairLtp =
                        generateLtp(link.dst(), CarrierEthernetNetworkInterface.Type.INNI);
                if (pairLtp == null) {
                    return null;
                }
                if (!ltpMap.containsKey(pairLtp.id())) {
                    ltpMap.put(pairLtp.id(), pairLtp);
                } else {
                    return null;
                }
            }
            ltpMap.put(ltp.id(), ltp);
            // Remove LTP from deleted set
            removedLtpSet.remove(ltp.id());
            return ltp;
        } else {
            return null;
        }
    }

    /**
     * Utility method to obtain an FC-specific LTP (UNI/INNI or ENNI) associated with a connect point.
     *
     * @param cp the connect point to check
     * @return a new FC-specific LTP associated with cp if the corresponding global LTP exists or null otherwise
     */
    private CarrierEthernetLogicalTerminationPoint fcLtpFromCp(ConnectPoint cp,
                                                               CarrierEthernetLogicalTerminationPoint.Role ltpRole) {
        // Check first if cp is associated with a device
        if (cp.deviceId() == null) {
            return null;
        }
        // Assuming LTP id is the same as the connect point id
        String cpId = cp.deviceId().toString() + "/" + cp.port().toString();
        if (ltpMap.containsKey(cpId)) {
            CarrierEthernetLogicalTerminationPoint ltp =
                    new CarrierEthernetLogicalTerminationPoint(cp, cpId, ltpMap.get(cpId).type(), ltpRole);
            return ltp;
        } else {
            return null;
        }
    }

    /**
     * Utility method to obtain the first FC in a set which contains the LTP with the provided id.
     *
     * @param ltpId the LTP id to search
     * @param fcSet the FC set to search
     * @return the first FC found in fcSet which contains an LTP with id ltpId, or null if no such FC is found
     */
    // FIXME: Find more efficient way to do that
    private CarrierEthernetForwardingConstruct getFcFromLtpId(String ltpId,
                                                              Set<CarrierEthernetForwardingConstruct> fcSet) {
        // Get the first FC that contains the LTP with the provided id
        for (CarrierEthernetForwardingConstruct fc : fcSet) {
            if (!fc.ltpSet().stream().filter(ltp -> ltp.id().equals(ltpId)).collect(Collectors.toList()).isEmpty()) {
                return fc;
            }
        }
        return null;
    }

    @Override
    public void setEvcFragmentation(boolean evcFragmentationEnabled) {
        prevEvcFragmentationStatus = this.evcFragmentationEnabled;
        this.evcFragmentationEnabled = evcFragmentationEnabled;
    }

    @Override
    public boolean getEvcFragmentation() {
        return evcFragmentationEnabled;
    }

    @Override
    public void resetEvcFragmentation() {
        this.evcFragmentationEnabled = prevEvcFragmentationStatus;
    }

    /**
     * Returns the VLAN tag associated with an FC via network configuration.
     *
     * The VLAN tag to be selected should be configured in at least one of the
     * FC LTPs and no different tag should be present in the rest of the FC LTPs.
     *
     * @param fc the FC to check
     * @return an Optional object with the VLAN to be associated with the FC if
     * one was found; an empty Optional object otherwise
     */
    private Optional<VlanId> getCfgVlan(CarrierEthernetForwardingConstruct fc) {
        VlanId cfgVlan = null;
        for (CarrierEthernetLogicalTerminationPoint ltp : fc.ltpSet()) {
            VlanId tmpVlan = portVlanMap.get(ltp.cp());
            if (tmpVlan == null) {
                continue;
            } else if (cfgVlan != null && cfgVlan != tmpVlan) {
                log.warn("Multiple configured S-TAGs for the same FC");
                return Optional.empty();
            } else {
                cfgVlan = tmpVlan;
            }
        }
        return cfgVlan == null ? Optional.empty() : Optional.of(cfgVlan);
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {

        /**
         * Negative events.
         */
        private final EnumSet<NetworkConfigEvent.Type> negative
                = EnumSet.of(NetworkConfigEvent.Type.CONFIG_UNREGISTERED,
                             NetworkConfigEvent.Type.CONFIG_REMOVED);

        /**
         * Actual configuration events.
         */
        private final EnumSet<NetworkConfigEvent.Type> actualConfig
                = EnumSet.of(NetworkConfigEvent.Type.CONFIG_ADDED,
                             NetworkConfigEvent.Type.CONFIG_REMOVED,
                             NetworkConfigEvent.Type.CONFIG_UPDATED);

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(PortVlanConfig.class) &&
                    actualConfig.contains(event.type());
        }

        @Override
        public void event(NetworkConfigEvent event) {

            if (!isRelevant(event)) {
                return;
            }

            ConnectPoint cp = (ConnectPoint) event.subject();
            PortVlanConfig config = networkConfigService.getConfig(cp, PortVlanConfig.class);

            if (config == null) {
                log.info("VLAN tag config is removed from port {}", cp);
                portVlanMap.remove(cp);
                return;
            }

            if (config.portVlanId().isPresent() && !negative.contains(event.type())) {
                VlanId assignedVlan = config.portVlanId().get();
                if (usedVlans().contains(assignedVlan)) {
                    log.warn("VLAN tag {} is already used in the CE network", assignedVlan);
                } else {
                    log.info("VLAN tag {} is assigned to port {}", assignedVlan, cp);
                    portVlanMap.put(cp, assignedVlan);
                }
            } else {
                log.info("VLAN tag is removed from port {}", cp);
                portVlanMap.remove(cp);
            }
        }
    }
}
