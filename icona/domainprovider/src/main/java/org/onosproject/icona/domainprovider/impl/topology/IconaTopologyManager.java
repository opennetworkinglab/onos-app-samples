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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.icona.domainmgr.api.DomainConfigService;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainmgr.api.DomainService;
import org.onosproject.icona.domainprovider.api.DomainTopology;
import org.onosproject.icona.domainprovider.api.DefaultDomainTopology;
import org.onosproject.icona.domainprovider.api.IconaSBListener;
import org.onosproject.icona.domainprovider.api.IconaSBListenerService;
import org.onosproject.icona.domainprovider.api.device.DefaultDomainDevice;
import org.onosproject.icona.domainprovider.api.device.DomainDevice;
import org.onosproject.icona.domainprovider.api.host.DefaultDomainHostDescription;
import org.onosproject.icona.domainprovider.api.host.DomainHostDescription;
import org.onosproject.icona.domainprovider.api.link.IntraLinkDescription;
import org.onosproject.icona.domainprovider.impl.config.IconaConfig;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onosproject.icona.domainprovider.impl.topology.IconaRemoteDeviceProvider.PROVIDER_NAME;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.icona.domainprovider.impl.config.IconaConfig.DomainConfig;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Port.Type.VIRTUAL;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 *
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true)
@Service
public class IconaTopologyManager implements IconaSBListenerService {
    protected static final String DEVICE_ID = "deviceId";
    protected static final String DOMAIN_ID = "domainId";
    protected static final String INTER_LINK_ID = "interlinkId";

    private final Logger log = getLogger(getClass());
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DomainService domainService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DomainConfigService domainConfigService;


    private final ConfigFactory configFactory =
            new ConfigFactory(APP_SUBJECT_FACTORY, IconaConfig.class, "icona") {
                @Override
                public IconaConfig createConfig() {
                    return new IconaConfig();
                }
            };

    private final ExecutorService eventExecutor =
            newFixedThreadPool(3, groupedThreads("onos/icona-sb-manager", "event-handler-%d"));

    private Set<DomainConfig> domainConfigs = Sets.newConcurrentHashSet();
    private Set<IconaSBListener> sbListeners = new CopyOnWriteArraySet<>();

    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final LocalDeviceListener deviceListener = new LocalDeviceListener();
    private final LocalHostListener hostListener = new LocalHostListener();
    private final LocalLinkListener linkListener = new LocalLinkListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(PROVIDER_NAME);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);

        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventExecutor.shutdown();

        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);

        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        log.info("Stopped");
    }

    @Override
    public void addListener(IconaSBListener sbListener) {
        log.debug("listener added");
        sbListeners.add(sbListener);
    }

    @Override
    public void removeListener(IconaSBListener sbListener) {
        sbListeners.remove(sbListener);
    }

    private void buildBigSwitch(String bigSwitchPrefixId, int portSpeed) {
        Map<DomainId, DomainTopology> topologyMap = Maps.newHashMap();

        domainConfigs.forEach(domainConfig -> {
            log.info("Building big switch topology for domain {}", domainConfig.domainId().id());
            DomainId localDomainId = domainConfigService.localDomainId();
            DeviceId bigSwitchId = DeviceId.deviceId(bigSwitchPrefixId + localDomainId + "-" +
                    domainConfig.domainId().id());
            AtomicLong portCounter = new AtomicLong(0);
            List<PortDescription> allPorts = new ArrayList<>();
            Map<PortNumber, ConnectPoint> virtualPortToPortMapping = new HashMap<>();

            domainConfigService.interlinkConnectPointMap().forEach((interlinkId, pair) -> {
                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(DEVICE_ID, bigSwitchId.toString())
                        .set(DOMAIN_ID, localDomainId.id())
                        .set(INTER_LINK_ID, interlinkId.id())
                        .build();

                allPorts.add(new DefaultPortDescription(
                        PortNumber.portNumber(portCounter.get()),
                        true,
                        VIRTUAL,
                        portSpeed,    // port speed is to be configured
                        annotations));

                virtualPortToPortMapping.put(PortNumber.portNumber(portCounter.get()),
                        pair.getRight());

                portCounter.getAndIncrement();
            });

            final Set<DomainHostDescription> domainHostDescriptions = new HashSet<>();
            hostService.getHosts().forEach(host ->
                    domainConfig.topologyConfig().endPointIds().forEach(mac -> {

                        if (host.mac().equals(MacAddress.valueOf(mac))) {

                            PortNumber virtualPort = PortNumber.portNumber(portCounter.get());

                            DefaultDomainHostDescription domainHost = new DefaultDomainHostDescription(
                                    localDomainId,
                                    MacAddress.valueOf(mac),
                                    VlanId.NONE,
                                    new HostLocation(bigSwitchId,
                                            virtualPort, 0),
                                    host.ipAddresses(),
                                    new HashSet<Ip4Prefix>()); // TODO: ip4/6prefixes from config

                            virtualPortToPortMapping.put(virtualPort,
                                    new ConnectPoint(host.location().deviceId(), host.location().port()));

                            domainHostDescriptions.add(domainHost);

                            portCounter.getAndIncrement();

                            log.info("host {} added", host.id());
                        }
                    })
            );
            domainService.setVirtualPortToPortMapping(bigSwitchId, virtualPortToPortMapping);

            domainHostDescriptions.forEach(domainHost ->
                    allPorts.add(new DefaultPortDescription(domainHost.location().port(),
                            true, VIRTUAL, portSpeed)));
            final DomainDevice bigSwitch =
                    new DefaultDomainDevice(bigSwitchId, localDomainId, allPorts);
            final Set<DomainDevice> domainDevices = Sets.newHashSet();
            domainDevices.add(bigSwitch);
            final Set<IntraLinkDescription> interLinks = Sets.newHashSet();
            final DomainTopology domainTopology = new DefaultDomainTopology(localDomainId, domainDevices,
                    interLinks, domainHostDescriptions);
            topologyMap.put(domainConfig.domainId(), domainTopology);
        });

        log.debug("Calling southbound listeners to expose the local topology");
        sbListeners.forEach(listener ->
                listener.configTopology(topologyMap));
    }

    private class LocalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case DEVICE_REMOVED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case DEVICE_SUSPENDED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case PORT_REMOVED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case PORT_ADDED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                default:
            }
        }
    }

    private class LocalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            switch (event.type()) {
                case HOST_ADDED:
                    /// TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case HOST_REMOVED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                default:
                    break;
            }
        }
    }

    private class LocalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            switch (event.type()) {
                case LINK_ADDED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case LINK_REMOVED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                    break;
                case LINK_UPDATED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
                default:
            }
        }
    }

    private void readConfig() {
        log.info("Config received");

        IconaConfig iconaConfig = configRegistry.getConfig(appId, IconaConfig.class);
        domainConfigs.addAll(
                iconaConfig.getPeersConfig());

        // TODO: different topology for different TopologyConfig.Type values
        buildBigSwitch(iconaConfig.getBigSwitchPrefixId(), iconaConfig.portSpeed());
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
                    eventExecutor.execute(IconaTopologyManager.this::readConfig);
                    break;
                case CONFIG_UPDATED:
                    log.info("Network configuration updated");
                    eventExecutor.execute(IconaTopologyManager.this::readConfig);
                    break;
                default:
                    break;
            }
        }
    }
}