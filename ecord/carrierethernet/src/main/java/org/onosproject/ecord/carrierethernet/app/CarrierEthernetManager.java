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
import org.onlab.packet.VlanId;

import org.onlab.util.Bandwidth;
import org.onosproject.ecord.metro.api.MetroConnectivityId;
import org.onosproject.ecord.metro.api.MetroPathEvent;
import org.onosproject.ecord.metro.api.MetroPathListener;
import org.onosproject.ecord.metro.api.MetroPathService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private MetroPathListener metroEventListener = new MetroEventListener();

    // Keeps track of the next S-VLAN tag the app will try to use
    private static short curVlanId = 0;

    private static final int METRO_CONNECT_TIMEOUT_MILLIS = 7000;

    private static final boolean PACKET_OPTICAL_TOPO = true;

    // TODO: Implement distributed store for CE services
    // The installed CE services
    private final Map<String, CarrierEthernetService> serviceMap = new ConcurrentHashMap<>();

    // TODO: Refactor this part
    private final Map<MetroConnectivityId, MetroPathEvent.Type> metroConnectStatusMap = new ConcurrentHashMap<>();

    // TODO: Implement distributed store for CE UNIs
    // The installed CE UNIs
    private final Map<String, CarrierEthernetUni> uniMap = new ConcurrentHashMap<>();

    /**
     * Activate this component.
     */
    @Activate
    public void activate() {
        metroPathService.addListener(metroEventListener);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    public void deactivate() {
        removeAllServices();
        metroPathService.removeListener(metroEventListener);
    }

    /**
     * Returns the map of installed services.
     *
     * @return map of installed services
     */
    public  Map<String, CarrierEthernetService> serviceMap() {
        return this.serviceMap;
    }

    // TODO: Add method to remove a UNI from an already installed service

    /**
     * Get an installed CE using its id.
     *
     * @return the CE service definition or null if the service doesn't exist
     */
    public CarrierEthernetService getService(String serviceId) {
        return ((serviceMap.get(serviceId) == null) ? null : serviceMap.get(serviceId));
    }

    /**
     * Get the map containing all installed services
     *
     * @return the CE service map
     */
    public Map<String, CarrierEthernetService> getServiceMap() {
        return serviceMap;
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
     * Verify the validity of a CE service definition taking also into account current network status.
     *
     * @param originalService the provided CE service definition
     * @return a valid, potentially modified service description, or null if the service could not be validated
     */
    private CarrierEthernetService validateService(CarrierEthernetService originalService) {

        // Make a copy of the provided service, since it may be modified
        CarrierEthernetService service = originalService;

        // Try to set a unique VLAN id for the service unless the service is being updated
        // TODO: Add different connectivity types
        if (service.vlanId() == null) {
            service.setVlanId(generateVlanId());
            if (service.vlanId() == null) {
                log.error("No available VLAN id found.");
                return null;
            }
        }

        // Verify that CE-VLAN ID is provided to either all UNIs or none and set the virtualService flag accordingly
        boolean isVirtual = false;
        Iterator<CarrierEthernetUni> it = service.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
            if (uni.ceVlanId() == null && isVirtual) {
                log.error("Could not validate the virtual status of the service.");
                return null;
            } else if (uni.ceVlanId() != null){
                isVirtual = true;
            }
        }
        service.setIsVirtual(isVirtual);

        // Set unique id for the service unless the service is being updated
        if (service.id() == null) {
            service.setId(generateServiceId(service));
        }

        Set<CarrierEthernetUni> validatedUniSet = new HashSet<>();

        // Check the UNIs of the service, possibly removing UNIs that are incompatible with existing ones
        it = service.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni uni = it.next();
            // Change the name of the UNI's BWP to the service name if it is an EVC BWP
            if (uni.bwp().type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                uni.bwp().setId(service.id());
            }
            // Check first if UNI already exists by checking against the global UNI Map
            if (uniMap.keySet().contains(uni.id())) {
                CarrierEthernetUni existingUni = uniMap.get(uni.id());
                // Check if the service-specific UNI is compatible with the global one
                if (!(existingUni.validateServiceUni(uni))) {
                    // If service is of ROOT_MULTIPOINT type and we have removed the root, return null
                    if (service.type() == CarrierEthernetService.Type.ROOT_MULTIPOINT &&
                            uni.type() == CarrierEthernetUni.Type.ROOT) {
                        log.error("Root UNI could not be added to %s service.", service.type().name());
                        return null;
                    }
                    log.warn("UNI {} could not be added to service.", uni.id());
                    continue;
                } else {
                    // Add UNI to service description
                    validatedUniSet.add(uni);
                }
            } else {
                // Add UNI to service description
                validatedUniSet.add(uni);
            }
        }

        // Update the service UNI set, based on the validated UNIs
        service.setUniSet(validatedUniSet);

        if (service.type().equals(CarrierEthernetService.Type.ROOT_MULTIPOINT) && (service.uniSet().size() < 2)) {
            log.error("{} service requires at least two UNIs.", service.type().name());
            return null;
        }

        if (service.type().equals(CarrierEthernetService.Type.MULTIPOINT_TO_MULTIPOINT) &&
                (service.uniSet().size() < 3)) {
            log.error("{} service requires more than two UNIs.", service.type().name());
            return null;
        }

        if (service.type().equals(CarrierEthernetService.Type.POINT_TO_POINT) && (service.uniSet().size() != 2)) {
            log.error("{} service requires exactly two UNIs.", service.type().name());
            return null;
        }

        return service;
    }

    /**
     * Establish connectivity according to the service type (E-Line, E-Tree, E-LAN) and the service parameters.
     *
     * @param originalService the CE service definition
     * @return boolean value indicating whether the service could be established even partially
     */
    public boolean establishConnectivity(CarrierEthernetService originalService) {

        // If service already exists, remove it and reestablish with new parameters
        if (originalService.id() != null && serviceMap.containsKey(originalService.id())) {
            return updateService(originalService);
        } else {
            originalService.setId(null);
        }

        CarrierEthernetService service = validateService(originalService);

        boolean outcome = false;

        if (service == null) {
            log.error("Service could not be installed, please check log for details.");
            return outcome;
        }

        // Temporary set for iterating through service UNI pairs
        Set<CarrierEthernetUni> uniSet = service.uniSet();

        // Temporary set for indicating which UNIs were finally included in the service
        Set<CarrierEthernetUni> usedUniSet = new HashSet<>();

        Iterator<CarrierEthernetUni> it1 = uniSet.iterator();
        while (it1.hasNext()) {

            CarrierEthernetUni uni1 = it1.next();

            // Iterate through all the remaining UNIs
            Iterator<CarrierEthernetUni> it2 = uniSet.iterator();
            while (it2.hasNext()) {

                CarrierEthernetUni uni2 = it2.next();

                // Skip equals
                if (uni1.equals(uni2)) {
                    continue;
                }

                // Do not establish connectivity between leaf UNIs (applies to Rooted_Multipoint)
                if (uni1.type() == CarrierEthernetUni.Type.LEAF && uni2.type() == CarrierEthernetUni.Type.LEAF) {
                    continue;
                }

                MetroConnectivityId metroConnectId = null;

                if (PACKET_OPTICAL_TOPO) {
                    metroConnectId = setupMetroConnectivity(uni1.cp(), uni2.cp(), uni1.bwp().cir(), service.latency());

                    if (metroConnectId == null ||
                            metroConnectStatusMap.get(metroConnectId) != MetroPathEvent.Type.PATH_INSTALLED) {
                        log.error("Could not establish metro connectivity between {} and {}" +
                                        " (metro id and status: {}, {})", uni1.cp(), uni2.cp(), metroConnectId,
                                (metroConnectId == null ? "null" : metroConnectStatusMap.get(metroConnectId)));
                        //continue;
                    }

                    if (metroConnectId != null) {
                        service.setMetroConnectivityId(metroConnectId);
                        service.setMetroConnectivityStatus(metroConnectStatusMap.get(metroConnectId));
                    }

                    log.info("Metro connectivity id and status for CE service {}: {}, {}", service.id(),
                            service.metroConnectivity().id(), service.metroConnectivity().status());

                    // FIXME: Temporary hack for ONS: Get vlanId from metro app
                    if (metroConnectId != null) {
                        Optional<VlanId> vlanId = metroPathService.getVlanId(metroConnectId);
                        if (vlanId.isPresent()) {
                            service.setVlanId(vlanId.get());
                        }
                    }
                }

                if (!cePktProvisioner.setupConnectivity(uni1, uni2, service)) {
                    log.warn("Could not set up packet connectivity between {} and {}", uni1, uni2);
                    removeMetroConnectivity(metroConnectId);
                    continue;
                }

                // Indicate that connection for at least one UNI pair has been established
                outcome = true;

                // Add UNIs to the set of UNIs used by the service
                usedUniSet.add(uni1);
                usedUniSet.add(uni2);
            }
            // Remove UNI from temporary set so that each pair is visited only once
            it1.remove();
        }

        // Update the service UNI set, based on the UNIs actually used
        service.setUniSet(usedUniSet);

        // If no pair was connected, do not register the service
        if (outcome) {
            serviceMap.put(service.id(), service);
            cePktProvisioner.applyBandwidthProfiles(service);
            // Apply the BWPs of the service UNI to the global UNIs, creating them if needed
            applyBandwidthProfiles(service.uniSet());
        }

        return outcome;
    }

    /**
     * Reestablish connectivity for an existing service.
     *
     * @param originalService the updated CE service definition
     * @return boolean value indicating whether the service could be established even partially
     */
    public boolean updateService(CarrierEthernetService originalService) {
        // Just checking again
        if (serviceMap.containsKey(originalService.id())) {
            log.info("Updating existing service {}", originalService.id());
            // Keep the VLAN ID of the original service
            originalService.setVlanId(serviceMap.get(originalService.id()).vlanId());
            removeService(originalService.id());
        }
        return establishConnectivity(originalService);
    }

    /**
     * Applies bandwidth profiles to the UNIs of a service and adds them if needed to the global UNI map.
     *
     * @param  uniSet set of UNIs that are included in the service
     */
    private void applyBandwidthProfiles(Set<CarrierEthernetUni> uniSet) {

        uniSet.forEach(uni -> {
            if (!(uniMap.keySet().contains(uni.id()))) {
                // Just add the UNI as it appears at the service
                uniMap.put(uni.id(), uni);
            } else {
                // Add UNI resources (BWP, CE-VLAN ID) to existing global UNI
                CarrierEthernetUni newUni = uniMap.get(uni.id());
                newUni.addServiceUni(uni);
                // Update config identifier
                newUni.setCfgId(uni.cfgId());
                uniMap.put(uni.id(), newUni);
            }
        });
    }

    /**
     * Removes bandwidth profiles from the UNIs of a service and removes them if needed from the global UNI map.
     *
     * @param serviceId the CE service id
     */
    private void removeBandwidthProfiles(String serviceId) {

        serviceMap.get(serviceId).uniSet().forEach(uni -> {
            // TODO: Check if the bandwidth profile really needs to be removed (e.g. may be CoS)
            cePktProvisioner.removeBandwidthProfiles(serviceMap.get(serviceId));

            // Remove UNI resources (BWP, CE-VLAN ID) from global UNI
            CarrierEthernetUni newUni = uniMap.get(uni.id());
            newUni.removeServiceUni(uni);
            uniMap.put(uni.id(), newUni);
        });
    }

    /**
     * Removes all resources associated with the application.
     *
     * This will be called either from the deactivate method or as a response to a CLI command.
     * */
    public void removeAllServices() {
        serviceMap.keySet().forEach(serviceId -> {
            CarrierEthernetService service = serviceMap.get(serviceId);
            cePktProvisioner.removeConnectivity(service);
            cePktProvisioner.removeBandwidthProfiles(service);
            removeMetroConnectivity(service.metroConnectivity().id());
            removeBandwidthProfiles(serviceId);
        });
        serviceMap.clear();
    }

    /**
     * Removes all resources associated with a specific CE service.
     *
     * @param serviceId the CE service id
     * */
    public void removeService(String serviceId) {
        if (serviceMap.containsKey(serviceId)) {
            CarrierEthernetService service = serviceMap.get(serviceId);
            cePktProvisioner.removeConnectivity(service);
            cePktProvisioner.removeBandwidthProfiles(service);
            removeMetroConnectivity(service.metroConnectivity().id());
            removeBandwidthProfiles(serviceId);
            serviceMap.remove(serviceId);
        }
    }

    /**
     * Generates a unique vlanId in the context of the CE app.
     *
     * @return the generated vlanId or null if none found
     * */
    public VlanId generateVlanId() {

        Set<VlanId> vlanIdSet = new HashSet<>();
        VlanId vlanId = null;

        serviceMap.values().forEach(service -> vlanIdSet.add(service.vlanId));

        // If all vlanIds are being used return null, else try to find the next available one
        if (vlanIdSet.size() <  VlanId.MAX_VLAN - 1) {
            do {
                curVlanId = nextValidShort(curVlanId);
                vlanId = VlanId.vlanId(curVlanId);
            }
            while (vlanIdSet.contains(vlanId));
        }

        return vlanId;
    }

    private short nextValidShort(short i) {

        if (i >= VlanId.MAX_VLAN || i <= 0) {
            return 1;
        } else {
            return (short) (i + 1);
        }
    }

    /**
     * Generates a unique id in the context of the CE app.
     *
     * @return the generated vlanId or null if none found
     * */
    public String generateServiceId(CarrierEthernetService service) {

        // TODO: Add different connectivity types

        String tmpType;

        if (service.type().equals(CarrierEthernetService.Type.POINT_TO_POINT)) {
            tmpType = "Line";
        } else if (service.type().equals(CarrierEthernetService.Type.MULTIPOINT_TO_MULTIPOINT)) {
            tmpType = "LAN";
        } else {
            tmpType = "Tree";
        }

        String serviceId = "E" + (service.isVirtual() ? "V" : "") + "P-" + tmpType + "-" +
                service.vlanId().toString();

        return serviceId;
    }

    /**
     * Adds all potential UNIs to the global UNI map if they are not already there.
     *
     * */
    // TODO: Modify to return set of UNIs so that method can be reused in Uni Completer
    public void addGlobalUnis() {

        CarrierEthernetUni uni;
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
                        // Add UNI only if it's not already there
                        if (!uniMap.containsKey(uni.id())) {
                            uniMap.put(uni.id(), uni);
                        }
                    }
                }
            }
        }
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
            while (((System.currentTimeMillis() - startTime) < METRO_CONNECT_TIMEOUT_MILLIS) &&
                    (metroConnectStatusMap.get(metroConnectId) != MetroPathEvent.Type.PATH_INSTALLED)) {
            }
        }
        return metroConnectId;
    }

    private void removeMetroConnectivity(MetroConnectivityId metroConnectId) {
        if (metroConnectId != null) {
            metroPathService.removeConnectivity(metroConnectId);
            long startTime = System.currentTimeMillis();
            while (((System.currentTimeMillis() - startTime) < METRO_CONNECT_TIMEOUT_MILLIS) &&
                    (metroConnectStatusMap.get(metroConnectId) != MetroPathEvent.Type.PATH_REMOVED)) {
            }
        }
    }

}
