/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.icona.domainprovider.impl.topology;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.icona.domainmgr.api.DomainConfigService;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainprovider.api.device.DomainDevice;
import org.onosproject.icona.domainprovider.api.device.IconaSBDeviceService;
import org.onosproject.icona.domainprovider.api.link.DefaultInterLinkDescription;
import org.onosproject.icona.domainprovider.api.link.IconaSBLinkService;
import org.onosproject.icona.domainprovider.api.link.InterLinkDescription;
import org.onosproject.icona.domainmgr.api.LinkId;
import org.onosproject.icona.domainprovider.impl.config.IconaConfig;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Link;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.onosproject.icona.domainprovider.impl.config.IconaConfig.DriverConfig;
import static org.onosproject.icona.domainprovider.impl.topology.IconaTopologyManager.DOMAIN_ID;
import static org.onosproject.icona.domainprovider.impl.topology.IconaTopologyManager.INTER_LINK_ID;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.DRIVER;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Exposes remote domain devices to the core.
 */
@Component(immediate = true)
@Service(value = IconaSBDeviceService.class)
public class IconaRemoteDeviceProvider implements DeviceProvider, IconaSBDeviceService {

    private final Logger log = getLogger(getClass());
    public static final String PROVIDER_NAME = "org.onosproject.icona.domainprovider";
    public static final ProviderId PROVIDER_ID = new ProviderId("domain", PROVIDER_NAME);
    private static final String UNKNOWN = "unknown";
    private static final String NO_LLDP = "no-lldp";

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IconaSBLinkService iconaSBLinkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DomainConfigService domainConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipAdminService mastershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    protected DeviceProviderService deviceProviderService;

    private DriverConfig driverConfig;

    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final ConfigFactory configFactory =
            new ConfigFactory(APP_SUBJECT_FACTORY, IconaConfig.class, "icona") {
                @Override
                public IconaConfig createConfig() {
                    return new IconaConfig();
                }
            };

    private final ExecutorService eventExecutor =
            newFixedThreadPool(3, groupedThreads("onos/icona-sb-manager", "event-handler-%d"));

    @Activate
    public void activate() {
        appId = coreService.registerApplication(PROVIDER_NAME);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);
        deviceProviderService = deviceProviderRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        deviceProviderRegistry.unregister(this);
        log.info("Stopped");
    }

    /**
     * Notify the core system that a new domain device is on.
     * @param deviceId remote device identifier
     */
    private void advertiseDevice(DeviceId deviceId, DomainId domainId) {
        ChassisId chassisId = new ChassisId();
        log.info("advertiseDevice");

        // disable lldp for this virtual device and annotate it with the proper driver
        String driverKey = driverConfig.manufacturer() + "-" + driverConfig.hwVersion() + "-" +
                driverConfig.swVersion();
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(NO_LLDP, "any")
                .set(DOMAIN_ID, domainId.id())
                .set(DRIVER, driverKey)
                .build();

        DeviceDescription deviceDescription = new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.SWITCH,
                driverConfig.manufacturer(), driverConfig.hwVersion(),
                driverConfig.swVersion(), UNKNOWN,
                chassisId,
                annotations);
        deviceProviderService.deviceConnected(deviceId, deviceDescription);
        mastershipAdminService.setRole(clusterService.getLocalNode().id(), deviceId,
                MastershipRole.MASTER);
    }

    /**
     * Notify the core system of all ports of a device.
     * @param deviceId device identifier
     * @param portDescriptions description of ports
     */
    private void advertiseDevicePorts(DeviceId deviceId, List<PortDescription> portDescriptions) {
        deviceProviderService.updatePorts(deviceId, portDescriptions);
    }

    /**
     * Creates two directed links for each inter-link port found among the list
     * of all the domain device ports, assuming all interlinks are bidirectional.
     * One connect-point is taken from configuration, the other from the port description
     * @param domainId domain identifier
     * @param deviceId port identifier
     * @param port interlink port description
     */
    private void advertiseInterlinks(DomainId domainId, DeviceId deviceId, PortDescription port) {
        LinkId interLinkId = LinkId.linkId(port.annotations().value(INTER_LINK_ID));
        Pair<Link.Type, ConnectPoint> interlinkConf =
                domainConfigService.interlinkConnectPointMap()
                        .get(interLinkId);
        if (interlinkConf != null) {

            ConnectPoint localCp = interlinkConf.getRight();
            ConnectPoint remoteCp = new ConnectPoint(deviceId, port.portNumber());
            Link.Type linkType = interlinkConf.getLeft();
            DomainId localDomainId = domainConfigService.localDomainId();
            // currently we handle interlinks as being bidirectional...
            InterLinkDescription interLinkDescription1 = new DefaultInterLinkDescription(localCp,
                    remoteCp, linkType, Pair.of(localDomainId, domainId), interLinkId);
            InterLinkDescription interLinkDescription2 = new DefaultInterLinkDescription(remoteCp,
                    localCp, linkType, Pair.of(domainId, localDomainId), interLinkId);
            iconaSBLinkService.addInterLink(interLinkDescription1);
            iconaSBLinkService.addInterLink(interLinkDescription2);
            log.info("Interlink {} detected", interLinkId.id());
        } else {
            log.info("No local connect point for interlink: " + interLinkId);
        }
    }

    private void disconnectDevice(DeviceId deviceId) {
        deviceProviderService.deviceDisconnected(deviceId);
    }

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    // DeviceProvider
    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        // TODO
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        // TODO
    }

    // IconaSBDeviceService
    @Override
    public void connectRemoteDevice(DomainDevice domainDevice) {
        DomainId domainId = domainDevice.domainId();
        DeviceId deviceId = domainDevice.deviceId();
        advertiseDevice(deviceId, domainId);
        advertiseDevicePorts(deviceId, domainDevice.ports());
        domainDevice.ports().forEach(port -> {
            if (port.annotations().keys().contains(INTER_LINK_ID)) {
                advertiseInterlinks(domainId, deviceId, port);
            }
        });
    }

    @Override
    public void addRemotePort(DomainId domainId, DeviceId deviceId, PortDescription newPort) {

    }

    @Override
    public void updateRemotePortState(DomainId domainId, DeviceId deviceId, PortDescription portDescription) {
        // TODO
    }

    @Override
    public void disconnectRemoteDevice(DomainId domainId, DeviceId deviceId) {
        disconnectDevice(deviceId);
    }

    @Override
    public void removeRemotePort(DomainId domainId, DeviceId deviceId, PortNumber portNumber) {
        // TODO
    }

    private void readConfig() {
        IconaConfig iconaConfig =
                configRegistry.getConfig(appId, IconaConfig.class);
        driverConfig = iconaConfig.getDriverConfig();
    }

    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(IconaConfig.class)) {
                return;
            }
            switch (event.type()) {
                case CONFIG_ADDED:
                    log.info("Network configuration added");
                    eventExecutor.execute(IconaRemoteDeviceProvider.this::readConfig);
                    break;
                case CONFIG_UPDATED:
                    log.info("Network configuration updated");
                    eventExecutor.execute(IconaRemoteDeviceProvider.this::readConfig);
                    break;
                default:
                    break;
            }
        }
    }
}