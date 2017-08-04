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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Bandwidth;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetPacketNodeService;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetProvisionerService;
import org.onosproject.net.Link;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Device;
import org.onosproject.net.topology.PathService;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;
import org.onosproject.newoptical.api.OpticalPathListener;
import org.onosproject.newoptical.api.OpticalPathService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Carrier Ethernet provisioner of connectivity for forwarding constructs and bandwidth profiles.
 */
@Component(immediate = true)
@Service
public class CarrierEthernetProvisioner implements CarrierEthernetProvisionerService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CarrierEthernetPacketNodeService cePktNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpticalPathService opticalPathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private OpticalPathListener opticalEventListener = new OpticalEventListener();

    private static final int OPTICAL_CONNECT_TIMEOUT_MILLIS = 5000;

    // If set to false, the setup of optical connectivity using the metro app is bypassed
    // TODO: Use the Component Configuration mechanism to set this parameter
    private boolean pktOpticalTopo = false;

    // TODO: Refactor this part
    private final Map<OpticalConnectivityId, OpticalPathEvent.Type> opticalConnectStatusMap = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        opticalPathService.addListener(opticalEventListener);
    }

    @Deactivate
    protected void deactivate() {
        opticalPathService.removeListener(opticalEventListener);
    }

    @Override
    public void setupConnectivity(CarrierEthernetForwardingConstruct fc) {

        boolean allPairsConnected = true;

        HashMap<CarrierEthernetNetworkInterface, HashSet<CarrierEthernetNetworkInterface>> ingressEgressNiMap =
                new HashMap<>();

        // Temporary set for iterating through LTP pairs
        Set<CarrierEthernetLogicalTerminationPoint> tempLtpSet = new HashSet<>(fc.ltpSet());

        // Temporary set for indicating which LTPs were finally included
        Set<CarrierEthernetLogicalTerminationPoint> usedLtpSet = new HashSet<>();

        Iterator<CarrierEthernetLogicalTerminationPoint> ltpIt1 = tempLtpSet.iterator();
        while (ltpIt1.hasNext()) {

            CarrierEthernetLogicalTerminationPoint ltp1 = ltpIt1.next();

            // Iterate through all the remaining NIs
            Iterator<CarrierEthernetLogicalTerminationPoint> ltpIt2 = tempLtpSet.iterator();
            while (ltpIt2.hasNext()) {

                CarrierEthernetLogicalTerminationPoint ltp2 = ltpIt2.next();

                // Skip equals
                if (ltp1.equals(ltp2)) {
                    continue;
                }

                // Do not establish connectivity between leaf NIs (applies to Rooted_Multipoint)
                // FIXME: Use proper LTP roles
                if (ltp1.role().equals(CarrierEthernetLogicalTerminationPoint.Role.LEAF)
                        && ltp2.role().equals(CarrierEthernetLogicalTerminationPoint.Role.LEAF)) {
                    continue;
                }

                OpticalConnectivityId opticalConnectId = null;

                if (pktOpticalTopo) {

                    Bandwidth reqBw;

                    if (ltp1.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                        reqBw = ((CarrierEthernetUni) ltp1.ni()).bwp().cir();
                    } else if (ltp2.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                        reqBw = ((CarrierEthernetUni) ltp2.ni()).bwp().cir();
                    } else {
                        reqBw = Bandwidth.bps((double) 0);
                    }

                    opticalConnectId = setupOpticalConnectivity(ltp1.ni().cp(), ltp2.ni().cp(), reqBw, fc.maxLatency());

                    if (opticalConnectId == null ||
                            opticalConnectStatusMap.get(opticalConnectId) != OpticalPathEvent.Type.PATH_INSTALLED) {
                        log.error("Could not establish optical connectivity between {} and {}" +
                                        " (optical id and status: {}, {})",
                                ltp1.ni().cp(), ltp2.ni().cp(), opticalConnectId,
                                (opticalConnectId == null ? "null" : opticalConnectStatusMap.get(opticalConnectId)));
                        allPairsConnected = false;
                        continue;
                    }

                    if (opticalConnectId != null) {
                        fc.setMetroConnectivityId(opticalConnectId);
                        fc.setMetroConnectivityStatus(opticalConnectStatusMap.get(opticalConnectId));
                    }

                    log.info("Metro connectivity id and status for FC {}: {}, {}", fc.id(),
                            fc.metroConnectivity().id(), fc.metroConnectivity().status());
                }

                // Update the ingress-egress NI map based on the calculated paths
                if (!updateIngressEgressNiMap(ltp1.ni(), ltp2.ni(), ingressEgressNiMap,
                        fc.congruentPaths(), fc.type())) {
                    removeOpticalConnectivity(opticalConnectId);
                    allPairsConnected = false;
                    continue;
                }

                // Indicate that connection for at least one NI pair has been established
                fc.setState(CarrierEthernetForwardingConstruct.State.ACTIVE);

                // Add NIs to the set of NIs used by the EVC
                usedLtpSet.add(ltp1);
                usedLtpSet.add(ltp2);
            }
            // Remove NI from temporary set so that each pair is visited only once
            ltpIt1.remove();
        }

        // Establish connectivity using the ingressEgressNiMap
        ingressEgressNiMap.keySet().forEach(srcNi -> {
            // Set forwarding only on packet switches
            if (deviceService.getDevice(srcNi.cp().deviceId()).type().equals(Device.Type.SWITCH)) {
                cePktNodeService.setNodeForwarding(fc, srcNi, ingressEgressNiMap.get(srcNi));
            }
        });

        // Update the NI set, based on the NIs actually used
        fc.setLtpSet(usedLtpSet);

        if (fc.isActive()) {
            if (!allPairsConnected) {
                fc.setState(CarrierEthernetConnection.State.PARTIAL);
            }
        }
    }

    /**
     * Select feasible link paths between two NIs in both directions and update
     * ingressEgressNiMap accordingly.
     *
     * @param ni1 the first NI
     * @param ni2 the second NI
     * @param ingressEgressNiMap the method will add here any ingress-egress NI associations
     * @param congruentPaths if true indicates that n1->n2 will follow the same path as n2->n1
     * @return true if the path was updated and false if a path could not be found in any of the directions
     */
    private boolean updateIngressEgressNiMap(CarrierEthernetNetworkInterface ni1, CarrierEthernetNetworkInterface ni2,
                                      HashMap<CarrierEthernetNetworkInterface,
                                              HashSet<CarrierEthernetNetworkInterface>> ingressEgressNiMap,
                                boolean congruentPaths, CarrierEthernetVirtualConnection.Type evcType) {

        // Find the paths for both directions at the same time, so that we can skip the pair if needed
        List<Link> forwardLinks = generateLinkList(ni1.cp(), ni2.cp(), evcType);
        List<Link> backwardLinks =
                congruentPaths ? generateInverseLinkList(forwardLinks) : generateLinkList(ni2.cp(), ni1.cp(), evcType);

        // Skip this UNI pair if no feasible path could be found
        if (forwardLinks == null || (backwardLinks == null)) {
            log.warn("There are no feasible paths between {} and {}.",
                    ni1.cp().deviceId(), ni2.cp().deviceId());
            return false;
        }

        // Populate the ingress/egress NI map for the forward and backward paths
        populateIngressEgressNiMap(ni1, ni2, forwardLinks, ingressEgressNiMap);
        populateIngressEgressNiMap(ni2, ni1, backwardLinks, ingressEgressNiMap);

        return true;
    }

    private void populateIngressEgressNiMap(CarrierEthernetNetworkInterface srcNi,
                                            CarrierEthernetNetworkInterface dstNi,
                                            List<Link> linkList,
                                            HashMap<CarrierEthernetNetworkInterface,
                                                    HashSet<CarrierEthernetNetworkInterface>> ingressEgressNiMap
                                            ) {
        // FIXME: Fix the method - avoid generating GENERIC NIs if not needed
        // Add the src and destination NIs as well as the associated Generic NIs
        ingressEgressNiMap.putIfAbsent(srcNi, new HashSet<>());
        // Add last hop entry only if srcNi, dstNi aren't on same device (in which case srcNi, ingressNi would coincide)
        if (!srcNi.cp().deviceId().equals(dstNi.cp().deviceId())) {
            // If srcNi, dstNi are not on the same device, create mappings to/from new GENERIC NIs
            ingressEgressNiMap.get(srcNi).add(new CarrierEthernetGenericNi(linkList.get(1).src(), null));
            CarrierEthernetGenericNi ingressNi =
                    new CarrierEthernetGenericNi(linkList.get(linkList.size() - 2).dst(), null);
            ingressEgressNiMap.putIfAbsent(ingressNi, new HashSet<>());
            ingressEgressNiMap.get(ingressNi).add(dstNi);
        } else {
            // If srcNi, dstNi are on the same device, this is the only mapping that will be created
            ingressEgressNiMap.get(srcNi).add(dstNi);
        }

        // Go through the links and create/add the intermediate NIs
        for (int i = 1; i < linkList.size() - 2; i++) {
            CarrierEthernetGenericNi ingressNi = new CarrierEthernetGenericNi(linkList.get(i).dst(), null);
            ingressEgressNiMap.putIfAbsent(ingressNi, new HashSet<>());
            ingressEgressNiMap.get(ingressNi).add(new CarrierEthernetGenericNi(linkList.get(i + 1).src(), null));
        }
    }

    private List<Link> generateLinkList(ConnectPoint cp1, ConnectPoint cp2,
                                        CarrierEthernetVirtualConnection.Type evcType) {
        Set<Path> paths;
        Path path = null;

        if (!cp1.deviceId().equals(cp2.deviceId())) {
            // If cp1 and cp2 are not on the same device a path must be found
            if (evcType.equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT)) {
                // For point-to-point connectivity use pre-calculated paths to make sure the shortest paths are chosen
                paths = pathService.getPaths(cp1.deviceId(), cp2.deviceId());
            } else {
                // Recalculate path so that it's over the pre-calculated spanning tree
                // FIXME: Find a more efficient way (avoid recalculating paths)
                paths = pathService.getPaths(cp1.deviceId(), cp2.deviceId(),
                        new CarrierEthernetSpanningTreeWeight(topologyService));
            }

            // Just select any of the returned paths
            // TODO: Select path in more sophisticated way and return null if any of the constraints cannot be met
            path = paths.iterator().hasNext() ? paths.iterator().next() : null;

            if (path == null) {
                return null;
            }
        }

        List<Link> links = new ArrayList<>();
        links.add(createEdgeLink(cp1, true));
        if (!cp1.deviceId().equals(cp2.deviceId())) {
            links.addAll(path.links());
        }
        links.add(createEdgeLink(cp2, false));

        return links;
    }

    private List<Link> generateInverseLinkList(List<Link> originalLinks) {

        if (originalLinks == null) {
            return null;
        }

        List<Link> inverseLinks = new ArrayList<>();

        inverseLinks.add(createEdgeLink(originalLinks.get(originalLinks.size() - 1).src(), true));

        for (int i = originalLinks.size() - 2; i > 0; i--) {
            // FIXME: Check again the Link creation parameters
            inverseLinks.add(DefaultLink.builder()
                    .src(originalLinks.get(i).dst())
                    .dst(originalLinks.get(i).src())
                    .type(Link.Type.DIRECT)
                    .providerId(new ProviderId("none", "none"))
                    .build());
        }
        inverseLinks.add(createEdgeLink(originalLinks.get(0).dst(), false));

        return inverseLinks;
    }

    @Override
    public void removeConnectivity(CarrierEthernetForwardingConstruct fc) {
        cePktNodeService.removeAllForwardingResources(fc);
        removeOpticalConnectivity(fc.metroConnectivity().id());
    }

    @Override
    public void createBandwidthProfiles(CarrierEthernetForwardingConstruct fc) {
        fc.uniSet().forEach(uni -> cePktNodeService.createBandwidthProfileResources(fc, uni));
    }

    @Override
    public void applyBandwidthProfiles(CarrierEthernetForwardingConstruct fc) {
        //  TODO: Select node manager depending on device protocol
        fc.uniSet().forEach(uni -> cePktNodeService.applyBandwidthProfileResources(fc, uni));
    }

    @Override
    public void removeBandwidthProfiles(CarrierEthernetForwardingConstruct fc) {
        //  TODO: Select node manager depending on device protocol
        fc.ltpSet().forEach((ltp -> {
            if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                cePktNodeService.removeBandwidthProfileResources(fc, (CarrierEthernetUni) ltp.ni());
            }
        }));
    }

    private class OpticalEventListener implements OpticalPathListener {

        @Override
        public void event(OpticalPathEvent event) {
            switch (event.type()) {
                case PATH_INSTALLED: case PATH_REMOVED:
                    log.info("Optical path event {} received for {}.", event.type(), event.subject());
                    opticalConnectStatusMap.put(event.subject(), event.type());
                    break;
                default:
                    log.error("Unexpected optical event type.");
                    break;
            }
        }
    }

    private OpticalConnectivityId setupOpticalConnectivity(ConnectPoint ingress, ConnectPoint egress,
                                                           Bandwidth bandwidth, Duration latency) {
        OpticalConnectivityId opticalConnectId = opticalPathService
                .setupConnectivity(ingress, egress, bandwidth, latency);
        if (opticalConnectId != null) {
            long startTime = System.currentTimeMillis();
            while (((System.currentTimeMillis() - startTime) < OPTICAL_CONNECT_TIMEOUT_MILLIS) &&
                    (opticalConnectStatusMap.get(opticalConnectId) != OpticalPathEvent.Type.PATH_INSTALLED)) {
                // FIXME WTF
            }
        }
        return opticalConnectId;
    }

    private void removeOpticalConnectivity(OpticalConnectivityId opticalConnectId) {
        if (opticalConnectId != null) {
            opticalPathService.removeConnectivity(opticalConnectId);
            long startTime = System.currentTimeMillis();
            while (((System.currentTimeMillis() - startTime) < OPTICAL_CONNECT_TIMEOUT_MILLIS) &&
                    (opticalConnectStatusMap.get(opticalConnectId) != OpticalPathEvent.Type.PATH_REMOVED)) {
                // FIXME WTF
            }
        }
    }

    /**
     * Indicates if the CE app is meant to control a packet-optical topology.
     *
     * @param pktOpticalTopo true if CE app controls a packet-optical topology;
     *                       false otherwise
     */
    public void setPktOpticalTopo(boolean pktOpticalTopo) {
        this.pktOpticalTopo = pktOpticalTopo;
    }

    /**
     * Determines it the CE app is meant to control a packet-optical topology.
     *
     * @return true if CE app is meant to control a packet-optical topology;
     * false otherwise
     */
    public boolean getPktOpticalTopo() {
        return pktOpticalTopo;
    }
}
