/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.icona.domainprovider.impl;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainprovider.api.device.DomainDevice;
import org.onosproject.icona.domainprovider.api.device.IconaSBDeviceService;
import org.onosproject.icona.domainprovider.api.link.DefaultInterLinkDescription;
import org.onosproject.icona.domainprovider.api.link.IconaSBLinkService;
import org.onosproject.icona.domainprovider.api.link.InterLinkDescription;
import org.onosproject.icona.domainprovider.api.link.LinkId;
import org.onosproject.icona.domainprovider.impl.config.IconaConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Link;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigListener;
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

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.onosproject.icona.domainprovider.impl.IconaTopologyManager.INTER_LINK_ID;
import static org.onosproject.icona.domainprovider.impl.config.IconaConfig.DomainConfig;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Exposes remote domain devices to the core.
 */
@Component(immediate = true)
@Service(IconaSBDeviceService.class)
public class IconaRemoteDeviceProvider implements DeviceProvider, IconaSBDeviceService {

    private final Logger log = getLogger(getClass());
    protected static final String PROVIDER_NAME = "org.onosproject.icona.domainprovider";
    protected static final ProviderId PROVIDER_ID = new ProviderId("domain", PROVIDER_NAME);
    private static final String UNKNOWN = "unknown";

    private ApplicationId appId;
    private DomainId localDomainId;

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

    protected DeviceProviderService deviceProviderService;

    private Set<DomainConfig> domainConfigs = Sets.newConcurrentHashSet();

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

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication(PROVIDER_NAME);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);
        deviceProviderService = deviceProviderRegistry.register(this);
    }

    @Deactivate
    public void deactivate() {
        //TODO: disconnect devices
        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        deviceProviderRegistry.unregister(this);
    }

    /**
     * Notify the core system that a new domain device is on.
     *
     * @param deviceId remote device identifier
     */
    private void advertiseDevice(DeviceId deviceId, DomainId domainId) {
        ChassisId chassisId = new ChassisId();

        // disable lldp for this virtual device
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set("no-lldp", "lldp is disabled for a domain device")
                .set("domainId", domainId.toString())
                .build();

        // TODO: give meaningful device info from the remote cluster
        DeviceDescription deviceDescription = new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.SWITCH,
                UNKNOWN, UNKNOWN,
                UNKNOWN, UNKNOWN,
                chassisId,
                annotations);
        deviceProviderService.deviceConnected(deviceId, deviceDescription);
    }

    /**
     * Notifies the core system of all ports of a device.
     *
     * @param deviceId device identifier
     * @param portDescriptions description of ports
     */
    private void advertiseDevicePorts(DeviceId deviceId, List<PortDescription> portDescriptions) {
        // ports are properly annotated in the southbound bundles
        deviceProviderService.updatePorts(deviceId, portDescriptions);
    }

    /**
     * Creates two directed links for each inter-link port found among the list
     * of all the domain device ports, assuming all interlinks are bidirectional.
     * One connect-point is taken from configuration, the other from the port description.
     *
     * @param domainId domain identifier
     * @param deviceId port identifier
     * @param port interlink port description
     */
    private void advertiseInterlinks(DomainId domainId, DeviceId deviceId, PortDescription port) {
        LinkId interLinkId = LinkId.linkId(port.annotations().value(INTER_LINK_ID));
        Optional<DomainConfig> optional = domainConfigs.stream()
                .filter(config -> !config.peerId().equals(domainId))
                .findFirst();
        if (optional.isPresent()) {
            Pair<Link.Type, ConnectPoint> interlinkConf =
                    optional.get()
                            .interLinkConnectPointMap()
                            .get(interLinkId);
            ConnectPoint localCp = interlinkConf.getRight();
            ConnectPoint remoteCp = new ConnectPoint(deviceId, port.portNumber());
            Link.Type linkType = interlinkConf.getLeft();
            // currently we handle interlinks as being bidirectional...
            SparseAnnotations annotations = annotateInterLink(localDomainId.id(), domainId.id());
            InterLinkDescription interLinkDescription1 = new DefaultInterLinkDescription(localCp,
                    remoteCp, linkType, Pair.of(localDomainId, domainId), interLinkId, annotations);
            SparseAnnotations annotations1 = annotateInterLink(domainId.id(), localDomainId.id());
            InterLinkDescription interLinkDescription2 = new DefaultInterLinkDescription(remoteCp,
                    localCp, linkType, Pair.of(domainId, localDomainId), interLinkId, annotations1);
            iconaSBLinkService.addInterLink(domainId, interLinkDescription1);
            iconaSBLinkService.addInterLink(domainId, interLinkDescription2);
        } else {
            log.info("No local connect point for interlink: " + interLinkId);
        }
    }
    private SparseAnnotations annotateInterLink(String srcDomain, String dstDomain) {
        return DefaultAnnotations.builder()
                .set("srcDomain", srcDomain)
                .set("dstDomain", dstDomain)
                .build();
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
    public void addRemotePort(DomainId domainId, DeviceId deviceId, PortDescription portDescription) {
        // TODO
    }

    @Override
    public void updateRemotePortState(DomainId domainId, DeviceId deviceId, PortDescription portDescription) {
        // TODO
    }

    @Override
    public void disconnectRemoteDevice(DomainId domainId, DeviceId deviceId) {
        // TODO
    }

    @Override
    public void removeRemotePort(DomainId domainId, DeviceId deviceId, PortNumber portNumber) {
        // TODO
    }

    private void readConfig() {
        IconaConfig iconaConfig =
                configRegistry.getConfig(appId, IconaConfig.class);
        localDomainId = iconaConfig.getLocalId();
        domainConfigs.addAll(
                iconaConfig.getPeersConfig());
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