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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.PathService;
import org.slf4j.Logger;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service (value = CarrierEthernetPacketProvisioner.class)
public class CarrierEthernetPacketProvisioner {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CarrierEthernetOpenFlowPacketNodeManager ceOfPktNodeManager;

    @Activate
    protected void activate() {}

    @Deactivate
    protected void deactivate() {

    }

    // TODO: Get LTPs as input
    public boolean setupConnectivity(CarrierEthernetNetworkInterface ni1, CarrierEthernetNetworkInterface ni2, CarrierEthernetVirtualConnection service) {

        // Find the paths for both directions at the same time, so that we can skip the pair if needed
        List<Link> forwardLinks = selectLinkPath(ni1, ni2, service);
        List<Link> backwardLinks = selectLinkPath(ni2, ni1, service);

        // Skip this UNI pair if no feasible path could be found
        if (forwardLinks == null || (!service.congruentPaths() && backwardLinks == null)) {
            log.warn("There are no feasible paths between {} and {}.",
                    ni1.cp().deviceId(), ni2.cp().deviceId());
            return false;
        }

        // Establish connectivity for the packet switches
        // TODO: Send some kind of gRPC message to BigSwitches
        for (int i = 0; i < forwardLinks.size() - 1; i++) {
            // Create flows for the forward direction
            boolean first = isFirst(i);
            boolean last = isLast(forwardLinks, i);
            ConnectPoint ingress = forwardLinks.get(i).dst();
            ConnectPoint egress = forwardLinks.get(i + 1).src();
            //  TODO: Select node manager depending on device protocol
            // Set forwarding only on packet switches
            if (deviceService.getDevice(ingress.deviceId()).type().equals(Device.Type.SWITCH)) {
                ceOfPktNodeManager.setNodeForwarding(service, ni1, ni2, ingress, egress, first, last);
            }

            if (service.congruentPaths()) {
                // Create flows for the forward direction using the reverse path
                ingress = forwardLinks.get(forwardLinks.size() - i - 1).src();
                egress = forwardLinks.get(forwardLinks.size() - i - 2).dst();
                //  TODO: Select node manager depending on device protocol
                if (deviceService.getDevice(ingress.deviceId()).type().equals(Device.Type.SWITCH)) {
                    ceOfPktNodeManager.setNodeForwarding(service, ni2, ni1, ingress, egress, first, last);
                }
            }
        }

        if (!service.congruentPaths()) {
            // Create flows for the backward direction using a path potentially different from the reverse one
            for (int i = 0; i < backwardLinks.size() - 1; i++) {
                boolean first = isFirst(i);
                boolean last = isLast(backwardLinks, i);
                ConnectPoint ingress = backwardLinks.get(i).dst();
                ConnectPoint egress = backwardLinks.get(i + 1).src();
                //  TODO: Select node manager depending on device protocol
                if (deviceService.getDevice(ingress.deviceId()).type().equals(Device.Type.SWITCH)) {
                    ceOfPktNodeManager.setNodeForwarding(service, ni2, ni1, ingress, egress, first, last);
                }
            }
        }

        return true;
    }

    /**
     * Select a feasible link path between two NIs based on the CE service parameters.
     *
     * @param ni1 the first NI
     * @param ni2 the second NI
     * @param service the CE service descriptor
     */
    private List<Link> selectLinkPath(CarrierEthernetNetworkInterface ni1, CarrierEthernetNetworkInterface ni2,
                                      CarrierEthernetVirtualConnection service) {

        /*List<Constraint> constraints = ImmutableList.<Constraint>builder()
                .add(new BandwidthConstraint(uni1.bwp().cir()))
                .add(new LatencyConstraint(service.latency()))
                .build();*/

        Set<Path> paths = pathService.getPaths(ni1.cp().deviceId(), ni2.cp().deviceId());

        Path path = null;

        for (Path p : paths) {
            // TODO: Select path in more sophisticated way and return null if any of the constraints cannot be met
            path = p;
            break;
        }

        if (path == null) {
            return null;
        } else {
            List<Link> links = new ArrayList<>();
            links.add(createEdgeLink(ni1.cp(), true));
            links.addAll(path.links());
            links.add(createEdgeLink(ni2.cp(), false));
            return links;
        }
    }

    private boolean isLast(List<Link> links, int i) {
        return i == links.size() - 2;
    }

    private boolean isFirst(int i) {
        return i == 0;
    }

    public void removeConnectivity(CarrierEthernetVirtualConnection evc) {
        // TODO: Add here the same call for all node manager types
        ceOfPktNodeManager.removeAllForwardingResources(evc);
    }

    /**
     * Applies bandwidth profiles to the UNIs of an EVC.
     *
     * @param evc the EVC representation
     */
    public void applyBandwidthProfiles(CarrierEthernetVirtualConnection evc) {
        //  TODO: Select node manager depending on device protocol
        evc.uniSet().forEach(uni -> ceOfPktNodeManager.applyBandwidthProfileResources(evc, uni));
    }

    /**
     * Removes bandwidth profiles from the UNIs of an ECV.
     *
     * @param evc the EVC representation
     */
    public void removeBandwidthProfiles(CarrierEthernetVirtualConnection evc) {
        //  TODO: Select node manager depending on device protocol
        evc.uniSet().forEach(uni -> ceOfPktNodeManager.removeBandwidthProfileResources(evc.id(), uni));
    }

}
