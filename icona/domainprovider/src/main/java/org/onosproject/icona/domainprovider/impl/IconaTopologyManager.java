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
import org.onosproject.icona.domainmgr.api.DomainId;
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
import org.onosproject.net.HostLocation;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onosproject.icona.domainprovider.impl.IconaRemoteDeviceProvider.PROVIDER_NAME;
import static org.onosproject.icona.domainprovider.impl.config.IconaConfig.DomainConfig;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Port.Type.VIRTUAL;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Component in charge of sending the local topology abstraction to the remote clusters
 * through the southbound listeners. It also listens for relevant local topology events.
 */
@Component(immediate = true)
@Service
public class IconaTopologyManager implements IconaSBListenerService {
    private static final String DEVICE_ID = "deviceId";
    private static final String DOMAIN_ID = "domainId";
    protected static final String INTER_LINK_ID = "interlinkId";

    private static DomainId localDomainId;

    private final Logger log = getLogger(IconaTopologyManager.class);

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
    }

    @Deactivate
    public void deactivate() {
        eventExecutor.shutdown();
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);

        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
    }

    @Override
    public void addListener(IconaSBListener sbListener) {
        sbListeners.add(sbListener);
    }

    @Override
    public void removeListener(IconaSBListener sbListener) {
        sbListeners.remove(sbListener);
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
            switch (event.type()) {
                case HOST_ADDED:
                    // TODO: check config policy and the impact on the local topology exposed to the peers
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

    private void buildFullMesh(DomainConfig domainConfig) {

    }
    // We will add a parameter to this method to process only the subsets of domains
    // whose TopologyConfig.Type is equal to BIG_SWITCH
    private void buildBigSwitch(String bigSwitchPrefixId) {
        Map<DomainId, DomainTopology> topologyMap = Maps.newHashMap();
        domainConfigs.forEach(domainConfig -> {
            log.info("Building big switch topology for domain {}", domainConfig.peerId().id());
            DeviceId bigSwitchId = DeviceId.deviceId(bigSwitchPrefixId + localDomainId);
            AtomicLong portCounter = new AtomicLong(0);

            List<PortDescription> allPorts = new ArrayList<>();

            domainConfig.interLinkConnectPointMap()
                    .forEach((interLinkId, pair) -> {
                        SparseAnnotations annotations = DefaultAnnotations.builder()
                                .set(DEVICE_ID, bigSwitchId.toString())
                                .set(DOMAIN_ID, localDomainId.id())
                                .set(INTER_LINK_ID, interLinkId.id())
                                .build();
                        allPorts.add(
                                new DefaultPortDescription(
                                        PortNumber.portNumber(portCounter.get()),
                                        true,
                                        VIRTUAL,
                                        100,    // port speed is to be configured
                                        annotations));

                        portCounter.getAndIncrement();
                    });

            Set<DomainHostDescription> domainHostDescriptions = new HashSet<>();
            hostService.getHosts().forEach(host ->
                    domainConfig.topologyConfig()
                            .endPointIds().forEach(mac -> {
                        if (host.mac().equals(MacAddress.valueOf(mac))) {
                            DefaultDomainHostDescription domainHost = new DefaultDomainHostDescription(
                                    localDomainId,
                                    MacAddress.valueOf(mac),
                                    VlanId.NONE,
                                    new HostLocation(bigSwitchId,
                                            PortNumber.portNumber(portCounter.get()), 0),
                                    host.ipAddresses(),
                                    new HashSet<Ip4Prefix>()); // TODO: ip4/6prefixes from config
                            domainHostDescriptions.add(domainHost);
                            log.info("host {} added as domainHost for domain {}", host.id(), domainConfig.peerId());
                            portCounter.getAndIncrement();
                        }
                    })
            );

            domainHostDescriptions.forEach(domainHost ->
                    allPorts.add(new DefaultPortDescription(domainHost.location().port(),
                            true, VIRTUAL, 100)));
            DomainDevice bigSwitch =
                    new DefaultDomainDevice(bigSwitchId, localDomainId, allPorts);
            Set<DomainDevice> domainDevices = Sets.newHashSet();
            domainDevices.add(bigSwitch);
            Set<IntraLinkDescription> interLinks = Sets.newHashSet();
            DomainTopology domainTopology = new DefaultDomainTopology(localDomainId, domainDevices,
                    interLinks, domainHostDescriptions);
            topologyMap.put(domainConfig.peerId(), domainTopology);
        });

        log.info("Calling southbound listeners to expose the local topology...");
        // listeners will executes network tasks,
        // load balancing is handled via the leadership service
        sbListeners.forEach(listener ->
                listener.configTopology(topologyMap));
    }

    private void readConfig() {
        log.info("List of domains and inter-link connect points received");

        IconaConfig iconaConfig =
                configRegistry.getConfig(appId, IconaConfig.class);

        localDomainId = iconaConfig.getLocalId();

        domainConfigs.addAll(
                iconaConfig.getPeersConfig());

        // TODO: different topology for different TopologyConfig.Type values
        buildBigSwitch(iconaConfig.getBigSwitchPrefixId());
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