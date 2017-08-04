/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ovsdbrest;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultPatchDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKey;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import static org.onosproject.ovsdbrest.OvsdbNodeConfig.OvsdbNode;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.ovsdbrest.OvsdbRestException.BridgeAlreadyExistsException;
import static org.onosproject.ovsdbrest.OvsdbRestException.BridgeNotFoundException;
import static org.onosproject.ovsdbrest.OvsdbRestException.OvsdbDeviceException;

/**
 * Bridge and port controller.
 */
@Component(immediate = true)
@Service
public class OvsdbBridgeManager implements OvsdbBridgeService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private static final int DPID_BEGIN = 4;
    private static final int OFPORT = 6653;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    private Set<OvsdbNode> ovsdbNodes;

    // {bridgeName: datapathId} structure to manage the creation/deletion of bridges
    private Map<String, DeviceId> bridgeIds = Maps.newConcurrentMap();

    private Map<OvsdbNode, Set<DeviceId>> ovsdbNodeDevIdsSetMap = Maps.newConcurrentMap();

    private final ExecutorService eventExecutor =
            newSingleThreadExecutor(groupedThreads("onos/ovsdb-rest-ctl", "event-handler", log));
    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final AtomicLong datapathId = new AtomicLong(DPID_BEGIN);


    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, OvsdbNodeConfig.class, "ovsdbrest") {
                @Override
                public OvsdbNodeConfig createConfig() {
                    return new OvsdbNodeConfig();
                }
            };

    @Activate
    protected void activate() {
        appId = coreService.getAppId("org.onosproject.ovsdbrest");
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createBridge(IpAddress ovsdbAddress, String bridgeName)
            throws OvsdbDeviceException, BridgeAlreadyExistsException {

        OvsdbNode ovsdbNode;
        log.debug("Creating bridge {} at {}", bridgeName, ovsdbAddress);
        try {
            //  gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.info(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        // construct a unique dev id'
        DeviceId dpid = getNextUniqueDatapathId(datapathId);


        if (isBridgeCreated(bridgeName)) {
            log.warn("A bridge with this name already exists, aborting.");
            throw new BridgeAlreadyExistsException();
        }
        List<ControllerInfo> controllers = new ArrayList<>();
        Sets.newHashSet(clusterService.getNodes()).forEach(controller -> {
            ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "tcp");
            controllers.add(ctrlInfo);
            log.info("controller {}:{} added", ctrlInfo.ip().toString(), ctrlInfo.port());
        });
        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            if (device == null) {
                log.warn("Ovsdb device not found, aborting.");
                throw new OvsdbDeviceException("Ovsdb device not found");
            }
            if (device.is(BridgeConfig.class)) {
                BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
                BridgeDescription bridgeDescription = DefaultBridgeDescription.builder()
                        .name(bridgeName)
                        .datapathId(dpid.toString())
                        .controllers(controllers)
                        .build();
                bridgeConfig.addBridge(bridgeDescription);
                bridgeIds.put(bridgeName, bridgeDescription.deviceId().get());
                log.info("Correctly created bridge {} at {}", bridgeName, ovsdbAddress);
            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id());
                throw new OvsdbDeviceException(
                        "The bridging behaviour is not supported in device " + device.id()
                );
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create integration bridge on {}", ovsdbNode.ovsdbIp());
            throw new OvsdbDeviceException("Error with ovsdb device: item not found");
        }
    }

    @Override
    public void deleteBridge(IpAddress ovsdbAddress, String bridgeName)
            throws OvsdbDeviceException, BridgeNotFoundException {

        OvsdbNode ovsdbNode;
        log.debug("Deleting bridge {} at {}", bridgeName, ovsdbAddress);

        try {
            // gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.warn(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        DeviceId deviceId = bridgeIds.get(bridgeName);
        if (deviceId == null) {
            log.warn("No bridge with this name, aborting.");
            throw new BridgeNotFoundException();
        }

        log.debug("Device id is: " + deviceId.toString());

        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            if (device == null) {
                log.warn("Ovsdb device not found, aborting.");
                throw new OvsdbDeviceException("Ovsdb device not found");
            }
            if (device.is(BridgeConfig.class)) {

                // unregister bridge from its controllers
                deviceId = DeviceId.deviceId(deviceId.uri());
                DriverHandler h = driverService.createHandler(deviceId);
                ControllerConfig controllerConfig = h.behaviour(ControllerConfig.class);
                controllerConfig.setControllers(new ArrayList<>());

                // remove bridge from ovsdb
                BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
                bridgeConfig.deleteBridge(BridgeName.bridgeName(bridgeName));
                bridgeIds.remove(bridgeName);

                // remove bridge from onos devices
                adminService.removeDevice(deviceId);

                log.info("Correctly deleted bridge {} at {}", bridgeName, ovsdbAddress);
            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id());
                throw new OvsdbDeviceException(
                        "The bridging behaviour is not supported in device " + device.id()
                );
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to delete bridge on {}", ovsdbNode.ovsdbIp());
            throw new OvsdbDeviceException("Error with ovsdb device: item not found");
        }
    }

    @Override
    public void addPort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbDeviceException, BridgeNotFoundException {

        OvsdbNode ovsdbNode;
        log.debug("Adding port {} to bridge {} at {}", portName, bridgeName, ovsdbAddress);

        try {
            // gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.warn(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            log.debug("OvsdbNode.ovsdbId = " + ovsdbNode.ovsdbId());
            if (device == null) {
                log.warn("Ovsdb device not found, aborting.");
                throw new OvsdbDeviceException("Ovsdb device not found");
            }
            if (device.is(BridgeConfig.class)) {
                // add port to bridge through ovsdb
                BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
                bridgeConfig.addPort(BridgeName.bridgeName(bridgeName), portName);
                log.info("Correctly added port {} to bridge {} at {}", portName, bridgeName, ovsdbAddress);
            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id());
                throw new OvsdbDeviceException(
                        "The bridging behaviour is not supported in device " + device.id()
                );
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to delete bridge on {}", ovsdbNode.ovsdbIp());
            throw new OvsdbDeviceException("Error with ovsdb device: item not found");
        }
    }

    @Override
    public void removePort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbDeviceException, BridgeNotFoundException {

        OvsdbNode ovsdbNode;
        log.debug("Deleting port {} to bridge {} at {}", portName, bridgeName, ovsdbAddress);

        try {
            // gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();

        } catch (NoSuchElementException nsee) {
            log.warn(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            if (device == null) {
                log.warn("Ovsdb device not found, aborting.");
                throw new OvsdbDeviceException("Ovsdb device not found");
            }
            if (device.is(BridgeConfig.class)) {

                // delete port from bridge through ovsdb
                BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
                bridgeConfig.deletePort(BridgeName.bridgeName(bridgeName), portName);

                log.info("Correctly deleted port {} from bridge {} at {}", portName, bridgeName, ovsdbAddress);

            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id());
                throw new OvsdbDeviceException(
                        "The bridging behaviour is not supported in device " + device.id()
                );
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to delete bridge on {}", ovsdbNode.ovsdbIp());
            throw new OvsdbDeviceException("Error with ovsdb device: item not found");
        }
    }

    @Override
    public void createPatchPeerPort(IpAddress ovsdbAddress, String bridgeName, String portName, String patchPeer)
            throws OvsdbDeviceException {

        OvsdbNode ovsdbNode;
        log.debug("Setting port {} as peer of port {}", portName, patchPeer);

        try {
            // gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.warn(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
        log.debug("OvsdbNode.ovsdbId = " + ovsdbNode.ovsdbId());
        if (device == null) {
            log.warn("Ovsdb device not found, aborting.");
            throw new OvsdbDeviceException("Ovsdb device not found");
        }

        if (device.is(InterfaceConfig.class)) {
            InterfaceConfig interfaceConfig = device.as(InterfaceConfig.class);

            // prepare patch
            PatchDescription.Builder builder = DefaultPatchDescription.builder();
            PatchDescription patchDescription = builder
                    .deviceId(bridgeName)
                    .ifaceName(portName)
                    .peer(patchPeer)
                    .build();
            // add patch to port through ovsdb
            interfaceConfig.addPatchMode(portName, patchDescription);
            log.info("Correctly created port {} on device {} as peer of port {}", portName, bridgeName, patchPeer);
        } else {
            log.warn("The interface behaviour is not supported in device {}", device.id());
            throw new OvsdbDeviceException(
                    "The interface behaviour is not supported in device " + device.id()
            );
        }
    }

    @Override
    public void createGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName, IpAddress localIp,
                                IpAddress remoteIp, String key)
            throws OvsdbDeviceException, BridgeNotFoundException {

        OvsdbNode ovsdbNode;
        log.debug("Setting up tunnel GRE from {} to {} with key {}",
                localIp, remoteIp, key);

        try {
            // gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.warn(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            log.debug("OvsdbNode.ovsdbId = " + ovsdbNode.ovsdbId());
            if (device == null) {
                log.warn("Ovsdb device not found, aborting.");
                throw new OvsdbDeviceException("Ovsdb device not found");
            }

            if (device.is(InterfaceConfig.class)) {
                InterfaceConfig interfaceConfig = device.as(InterfaceConfig.class);

                // prepare tunnel
                TunnelDescription tunnelDescription = DefaultTunnelDescription.builder()
                        .deviceId(bridgeName)
                        .ifaceName(portName)
                        .type(TunnelDescription.Type.GRE)
                        .local(TunnelEndPoints.ipTunnelEndpoint(localIp))
                        .remote(TunnelEndPoints.ipTunnelEndpoint(remoteIp))
                        .key(new TunnelKey<>(key))
                        .build();
                // create tunnel to port through ovsdb
                interfaceConfig.addTunnelMode(portName, tunnelDescription);
                log.info("Correctly added tunnel GRE from {} to {} with key {}",
                        localIp, remoteIp, key);
            } else {
                log.warn("The interface behaviour is not supported in device {}", device.id());
                throw new OvsdbDeviceException(
                        "The interface behaviour is not supported in device " + device.id()
                );
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to delete bridge on {}", ovsdbNode.ovsdbIp());
            throw new OvsdbDeviceException("Error with ovsdb device: item not found");
        }
    }

    @Override
    public void deleteGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbDeviceException {

        OvsdbNode ovsdbNode;
        log.debug("Deleting tunnel GRE from interface {}",
                portName);

        try {
            // gets the target ovsdb node
            ovsdbNode = ovsdbNodes.stream().filter(node -> node.ovsdbIp().equals(ovsdbAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.warn(nsee.getMessage());
            throw new OvsdbDeviceException(nsee.getMessage());
        }

        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            if (device == null) {
                log.warn("Ovsdb device not found, aborting.");
                throw new OvsdbDeviceException("Ovsdb device not found");
            }

            if (device.is(InterfaceConfig.class)) {
                InterfaceConfig interfaceConfig = device.as(InterfaceConfig.class);
                // remove tunnel through ovsdb
                interfaceConfig.removeTunnelMode(portName);
                log.info("Correctly deleted tunnel GRE from interface {}", portName);
            } else {
                log.warn("The interface behaviour is not supported in device {}", device.id());
                throw new OvsdbDeviceException(
                        "The interface behaviour is not supported in device " + device.id()
                );
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to delete bridge on {}", ovsdbNode.ovsdbIp());
            throw new OvsdbDeviceException("Error with ovsdb device: item not found");
        }

    }

    /**
     * Performs the connection to ovsdb.
     *
     * @param node the ovsdb node, with IP address and port
     */
    private void connectOvsdb(OvsdbNode node) {
        if (!isOvsdbConnected(node)) {
            log.info("connecting ovsdb at {}:{}", node.ovsdbIp(), node.ovsdbPort());
            controller.connect(node.ovsdbIp(), node.ovsdbPort());
        }
    }

    /**
     * Gets an available datapath id for the new bridge.
     *
     * @param datapathId the integer used to generate ids
     * @return the datapath id
     */
    private DeviceId getNextUniqueDatapathId(AtomicLong datapathId) {
        DeviceId dpid;
        do {
            String stringId = String.format("%16X", datapathId.getAndIncrement()).replace(' ', '0');
            log.info("String id is: " + stringId);
            dpid = DeviceId.deviceId(stringId);
        } while (deviceService.getDevice(dpid) != null);
        return dpid;
    }

    /**
     * Checks if the bridge exists and is available.
     *
     * @return true if the bridge is available, false otherwise
     */
    private boolean isBridgeCreated(String bridgeName) {
        DeviceId deviceId = bridgeIds.get(bridgeName);
        return (deviceId != null
                && deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId));
    }

    /**
     * Returns connection state of OVSDB server for a given node.
     *
     * @return true if it is connected, false otherwise
     */
    private boolean isOvsdbConnected(OvsdbNode node) {

        OvsdbClientService ovsdbClient = getOvsdbClient(node);
        return deviceService.isAvailable(node.ovsdbId()) &&
                ovsdbClient != null && ovsdbClient.isConnected();
    }

    /**
     * Returns OVSDB client for a given node.
     *
     * @return OVSDB client, or null if it fails to get OVSDB client
     */
    private OvsdbClientService getOvsdbClient(OvsdbNode node) {

        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
                new OvsdbNodeId(node.ovsdbIp(), node.ovsdbPort().toInt()));
        if (ovsdbClient == null) {
            log.trace("Couldn't find OVSDB client for {}", node.ovsdbId().toString());
        }
        return ovsdbClient;
    }

    /**
     * Returns an ovsdb node associated with a given OVSDB device.
     *
     * @param ovsdbId OVSDB device id
     * @return cordvtn node, null if it fails to find the node
     */
    private OvsdbNode nodeByOvsdbId(DeviceId ovsdbId) {
        return ovsdbNodes.stream()
                .filter(node -> node.ovsdbId().equals(ovsdbId))
                .findFirst().orElse(null);
    }

    /**
     * Returns ovsdb node associated with a given integration bridge.
     *
     * @param bridgeId device id of the bridge
     * @return ovsdb node, null if it fails to find the node
     */
    private OvsdbNode nodeByBridgeId(DeviceId bridgeId) {
        final  Set<OvsdbNode> nodes = new HashSet<>();
        ovsdbNodeDevIdsSetMap.forEach((node, set) -> {
            if (set.contains(bridgeId)) {
                 nodes.add(node);
            }
        });
        Optional<OvsdbNode> opt = nodes.stream().findAny();
        if (opt.isPresent()) {
            return opt.get();
        } else {
            return null;
        }
    }

    private void readConfiguration() {
        OvsdbNodeConfig config = configRegistry.getConfig(appId, OvsdbNodeConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }
        ovsdbNodes = config.getNodes();
        ovsdbNodes.forEach(this::connectOvsdb);
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(OvsdbNodeConfig.class)) {
                return;
            }
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    eventExecutor.execute(OvsdbBridgeManager.this::readConfiguration);
                    break;
                default:
                    break;
            }
        }
    }
}
