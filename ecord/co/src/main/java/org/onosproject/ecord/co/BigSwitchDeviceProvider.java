/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.ecord.co;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONOSLLDP;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceDirectory;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.ProbedLinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.ecord.co.BigSwitchManager.REALIZED_BY;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

/* To configure the BigSwitchDevice parameters at startup, add configuration to tools/package/config/component-cfg.json
   before using onos-package command.
   Configuration example:
   {
     "org.onosproject.ecord.co.BigSwitchDeviceProvider": {
       "providerScheme": "bigswitch",
       "providerId": "org.onosproject.bigswitch",
       "remoteUri": "grpc://192.168.64.100:11984",
       "metroIp": "192.168.64.100"
     }
   }
   Note that you need the port number (11984) and the metroIp and the remoteUri
   values should point to the same IP/host.
 */

/**
 * Device provider which exposes a big switch abstraction of the underlying data path.
 */
@Component(immediate = true)
public class BigSwitchDeviceProvider implements DeviceProvider, ProbedLinkProvider {

    private static final Logger LOG = getLogger(BigSwitchDeviceProvider.class);

    private static final String PROP_SCHEME = "providerScheme";
    private static final String DEFAULT_SCHEME = "bigswitch";
    private static final String PROP_ID = "providerId";
    private static final String DEFAULT_ID = "org.onosproject.bigswitch";
    private static final String PROP_REMOTE_URI = "remoteUri";
    private static final String DEFAULT_REMOTE_URI = "local://localhost";
    private static final String PROP_METRO_IP = "metroIp";
    private static final String DEFAULT_METRO_IP = "localhost";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BigSwitchService bigSwitchService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RemoteServiceDirectory rpcService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService metadataService;

    private RemoteServiceContext remoteServiceContext;
    private BigSwitch bigSwitch;
    private DeviceDescription bigSwitchDescription;
    private DeviceProviderRegistry deviceProviderRegistry;
    private DeviceProviderService deviceProviderService;
    private LinkProviderRegistry linkProviderRegistry;
    private LinkProviderService linkProviderService;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    private BigSwitchListener listener = new InternalListener();

    private PacketProcessor packetProcessor = new InternalPacketProcessor();

    private final Ethernet ethPacket = new Ethernet();

    private final ConfigFactory<ConnectPoint, CrossConnectConfig> xcConfigFactory
        = new ConfigFactory<ConnectPoint, CrossConnectConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                                                    CrossConnectConfig.class,
                                                    "cross-connect") {
            @Override
            public CrossConnectConfig createConfig() {
                return new CrossConnectConfig();
            }
        };

    @Property(name = PROP_SCHEME, value = DEFAULT_SCHEME,
            label = "Provider scheme used to register a big switch device")
    private String schemeProp = DEFAULT_SCHEME;

    @Property(name = PROP_ID, value = DEFAULT_ID,
            label = "Provider ID used to register a big switch device")
    private String idProp = DEFAULT_ID;

    @Property(name = PROP_REMOTE_URI, value = DEFAULT_REMOTE_URI,
            label = "URI of remote host to connect via RPC service")
    private String remoteUri = DEFAULT_REMOTE_URI;

    @Property(name = PROP_METRO_IP, value = DEFAULT_METRO_IP,
            label = "IP address or hostname of metro ONOS instance to make REST calls")
    private String metroIp = DEFAULT_METRO_IP;

    private ProviderId providerId;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        loadRpcConfig(context);
        loadRestConfig(context);

        // setup service to, and register with, providers
        try {
            remoteServiceContext = rpcService.get(URI.create(remoteUri));
        } catch (UnsupportedOperationException e) {
            LOG.warn("Unsupported URI: {}", remoteUri);
        }
        providerId = new ProviderId(schemeProp, idProp);
        registerToDeviceProvider();
        executor = newSingleThreadScheduledExecutor(groupedThreads("onos/bigswitch", "discovery-%d"));
        prepareProbe();
        registerToLinkServices();

        // start listening to config changes
        NetworkConfigListener cfglistener = new InternalConfigListener();
        cfgRegistry.addListener(cfglistener);
        cfgRegistry.registerConfigFactory(xcConfigFactory);
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(packetProcessor);

        cfgRegistry.unregisterConfigFactory(xcConfigFactory);
        cfgService.unregisterProperties(getClass(), false);
        unregisterFromLinkServices();
        executor.shutdownNow();
        unregisterFromDeviceProvider();
        // Won't hurt but necessary?
        deviceProviderService = null;
        providerId = null;
        LOG.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        // Needs re-registration to DeviceProvider
        if (loadRpcConfig(context)) {
            // unregister from Device and Link Providers with old parameters
            unregisterFromLinkServices();
            unregisterFromDeviceProvider();
            // register to Device and Link Providers with new parameters
            try {
                remoteServiceContext = rpcService.get(URI.create(remoteUri));
                registerToDeviceProvider();
                registerToLinkServices();
            } catch (UnsupportedOperationException e) {
                LOG.warn("Unsupported URI: {}", remoteUri);
            }
            LOG.info("Re-registered with Device and Link Providers");
        }

        // Needs to advertise cross-connect links
        if (loadRestConfig(context)) {
            advertiseCrossConnectLinksOnAllPorts();
        }
    }

    // Loads RPC-related configuration values from ComponentContext and returns true if there are any changes.
    private boolean loadRpcConfig(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        boolean changed = false;
        // TODO When configuration value is set to null or empty, actual value will differ from configuration.
        //      Any means to sync those values?
        String s = Tools.get(properties, PROP_SCHEME);
        String newValue = Strings.isNullOrEmpty(s) ? DEFAULT_SCHEME : s;
        if (!schemeProp.equals(newValue)) {
            schemeProp = newValue;
            changed = true;
        }
        s = Tools.get(properties, PROP_ID);
        newValue = Strings.isNullOrEmpty(s) ? DEFAULT_ID : s;
        if (!idProp.equals(newValue)) {
            idProp = newValue;
            changed = true;
        }
        s = Tools.get(properties, PROP_REMOTE_URI);
        newValue = Strings.isNullOrEmpty(s) ? DEFAULT_REMOTE_URI : s;
        if (!remoteUri.equals(newValue)) {
            remoteUri = newValue;
            changed = true;
        }

        return changed;
    }

    // Loads REST-related configuration values from ComponentContext and returns true if there are any changes.
    private boolean loadRestConfig(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        boolean changed = false;
        String s = Tools.get(properties, PROP_METRO_IP);
        String newValue = Strings.isNullOrEmpty(s) ? DEFAULT_METRO_IP : s;
        if (!metroIp.equals(newValue)) {
            metroIp = newValue;
            changed = true;
        }

        return changed;
    }

    private void registerToDeviceProvider() {
        // Create big switch device and description
        DeviceId deviceId = DeviceId.deviceId(schemeProp + ':' + clusterService.getLocalNode().ip());
        bigSwitch = new BigSwitch(deviceId, this.id());
        bigSwitchDescription = new DefaultDeviceDescription(bigSwitch.id().uri(),
                bigSwitch.type(), bigSwitch.manufacturer(),
                bigSwitch.hwVersion(), bigSwitch.swVersion(), bigSwitch.serialNumber(), bigSwitch.chassisId());
        deviceProviderRegistry = remoteServiceContext.get(DeviceProviderRegistry.class);
        deviceProviderService = deviceProviderRegistry.register(this);
        // Start big switch service and register device
        deviceProviderService.deviceConnected(bigSwitch.id(), bigSwitchDescription);
        deviceProviderService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
        advertiseCrossConnectLinksOnAllPorts();
        bigSwitchService.addListener(listener);
    }

    private void registerToLinkServices() {
        // Start link discovery -related functions
        linkProviderRegistry = remoteServiceContext.get(LinkProviderRegistry.class);
        linkProviderService = linkProviderRegistry.register(this);

        future = executor.scheduleAtFixedRate(new DiscoveryTask(), 3, 3, TimeUnit.SECONDS);

        // maybe also want a way to say 'get me next usable priority of class X'
        packetService.addProcessor(packetProcessor, PacketProcessor.advisor(2));
    }

    private void unregisterFromDeviceProvider() {
        if (bigSwitch == null) {
            LOG.warn("Invalid unregistration.");
            return;
        }
        deviceProviderService.deviceDisconnected(bigSwitch.id());
        deviceProviderRegistry.unregister(this);
        bigSwitch = null;
    }

    private void unregisterFromLinkServices() {
        future.cancel(true);
        packetService.removeProcessor(packetProcessor);
        linkProviderRegistry.unregister(this);
    }

    private ConnectPoint toConnectPoint(String strCp) {
        String[] split = strCp.split("/");
        if (split.length != 2) {
            LOG.warn("Unexpected annotation %s:%s", REALIZED_BY, strCp);
            return null;
        }
        DeviceId did = DeviceId.deviceId(split[0]);
        PortNumber num = PortNumber.fromString(split[1]);
        return new ConnectPoint(did, num);
    }

    private Optional<Pair<ConnectPoint, ConnectPoint>> crossConnectLink(PortDescription bigPort) {
        String sPhyCp = bigPort.annotations().value(REALIZED_BY);
        if (sPhyCp == null) {
            return Optional.empty();
        }

        ConnectPoint phyCp = toConnectPoint(sPhyCp);
        if (phyCp != null) {
            CrossConnectConfig config = cfgRegistry.getConfig(phyCp, CrossConnectConfig.class);
            if (config != null) {
                return config.remote()
                    .map(remCp -> Pair.of(new ConnectPoint(bigSwitch.id(), bigPort.portNumber()), remCp));
            }
        }
        return Optional.empty();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ObjectNode crossConnectLinksJson(Pair<ConnectPoint, ConnectPoint> link) {
        ObjectNode linksCfg = MAPPER.createObjectNode();

        ObjectNode basic = MAPPER.createObjectNode();
        basic.putObject("basic")
            .put(BasicLinkConfig.IS_DURABLE, true)
            .put(BasicLinkConfig.TYPE, "OPTICAL");
        linksCfg.set(String.format("%s/%s-%s/%s",
                                   link.getLeft().deviceId(), link.getLeft().port(),
                                   link.getRight().deviceId(), link.getRight().port()),
                     basic);
        linksCfg.set(String.format("%s/%s-%s/%s",
                                   link.getRight().deviceId(), link.getRight().port(),
                                   link.getLeft().deviceId(), link.getLeft().port()),
                     basic);
        return linksCfg;
    }

    private boolean postNetworkConfig(String subject, ObjectNode cfg) {
        // TODO Slice out REST Client code as library?
        Client client = ClientBuilder.newClient();

        client.property(ClientProperties.FOLLOW_REDIRECTS, true);

        // Trying to do JSON processing using Jackson triggered OSGi nightmare
        //client.register(JacksonFeature.class);

        final Map<String, String> env = System.getenv();
        // TODO Where should we get the user/password from?
        String user = env.getOrDefault("ONOS_WEB_USER", "onos");
        String pass = env.getOrDefault("ONOS_WEB_PASS", "rocks");
        HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(user, pass);
        client.register(auth);

        // TODO configurable base path
        WebTarget target = client.target("http://" + metroIp + ":8181/onos/v1/")
                                    .path("network/configuration/")
                                    .path(subject);

        Response response = target.request(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(cfg.toString(), MediaType.APPLICATION_JSON));


        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            LOG.error("POST failed {}\n{}", response, cfg.toString());
            return false;
        }
        return true;
    }

    private void advertiseCrossConnectLinks(PortDescription port) {
        crossConnectLink(port).ifPresent(xcLink -> {
            LOG.debug("CrossConnect {} is {}!",
                     xcLink,
                     port.isEnabled() ? "up" : "down");
            // TODO check port status and add/remove cross connect Link
            postNetworkConfig("links", crossConnectLinksJson(xcLink));
        });
    }

    private void advertiseCrossConnectLinksOnAllPorts() {
        bigSwitchService.getPorts()
            .forEach(BigSwitchDeviceProvider.this::advertiseCrossConnectLinks);
    }

    private void prepareProbe() {
        ethPacket.setEtherType(Ethernet.TYPE_LLDP)
                 .setDestinationMACAddress(ONOSLLDP.LLDP_NICIRA)
                 .setPad(true);
    }

    private String buildMac() {
        return ProbedLinkProvider.fingerprintMac(metadataService.getClusterMetadata());
    }

    private class InternalListener implements BigSwitchListener {
        @Override
        public void event(BigSwitchEvent event) {
            switch (event.type()) {
                case PORT_ADDED:
                case PORT_REMOVED:
                    deviceProviderService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
                    // if the subject's underlying port was a cross connect port,
                    // advertise cross-connect link to Metro-ONOS view
                    advertiseCrossConnectLinks(event.subject());
                    break;

                case PORT_UPDATED:
                    deviceProviderService.portStatusChanged(bigSwitch.id(), event.subject());
                    // if the subject's underlying port was a cross connect port,
                    // advertise cross-connect link to Metro-ONOS view
                    advertiseCrossConnectLinks(event.subject());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * A big switch device provider implementation.
     */
    public BigSwitchDeviceProvider() {
    }

    @Override
    public ProviderId id() {
        return providerId;
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean b) {
    }

    public class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == CrossConnectConfig.class) {
                advertiseCrossConnectLinksOnAllPorts();
            }
        }
    }

    /*
     * Emits link probes tagged with the big switch's scheme from available
     * virtual ports. This is run every three seconds.
     */
    private class DiscoveryTask implements Runnable {

        @Override
        public void run() {
            bigSwitchService.getPorts().forEach(p -> {
                // ID of big switch contains schema, so we're good
                ONOSLLDP lldp = ONOSLLDP.onosLLDP(bigSwitch.id().toString(),
                                                           bigSwitch.chassisId(),
                                                           (int) p.portNumber().toLong());
                ethPacket.setSourceMACAddress(buildMac()).setPayload(lldp);

                // recover physical connect point
                ConnectPoint real = ConnectPoint.deviceConnectPoint(
                        p.annotations().value(BigSwitchManager.REALIZED_BY));
                LOG.debug("sending probe for {}/{} through {}", bigSwitch.id(), p.portNumber(), real.toString());
                packetService.emit(new DefaultOutboundPacket(real.deviceId(),
                                                            builder().setOutput(real.port()).build(),
                                                            ByteBuffer.wrap(ethPacket.serialize())));
            });
        }

    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            Ethernet eth = context.inPacket().parsed();
            if (eth == null) {
                return;
            }
            ONOSLLDP probe = ONOSLLDP.parseONOSLLDP(eth);
            if (probe == null) {
                return;
            }
            if (isValidProbe(eth.getSourceMAC().toString(), probe)) {
                /*
                 * build and pass a linkDesription to the metro domain, which
                 * will map out a virtual link between the two big switches.
                 */
                PortNumber srcPort = PortNumber.portNumber(probe.getPort());
                DeviceId srcDev = DeviceId.deviceId(probe.getDeviceString());
                ConnectPoint src = new ConnectPoint(srcDev, srcPort);
                // receiver-side: some assembly required.
                PortNumber dstPort = bigSwitchService.getPort(context.inPacket().receivedFrom());
                if (dstPort == null) {
                    return;
                }
                ConnectPoint dst = new ConnectPoint(bigSwitch.id(), dstPort);

                LOG.debug("recvd link: {}->{}", src, dst);
                linkProviderService.linkDetected(new DefaultLinkDescription(src, dst, Link.Type.VIRTUAL));
            }
        }

        /*
         * true for probes from other controller clusters, that are from big switches.
         */
        private boolean isValidProbe(String mac, ONOSLLDP probe) {
            // don't consider ourselves valid if we're using DEFAULT_MAC
            String ourMac = buildMac();
            if (mac.equalsIgnoreCase(ourMac) || ProbedLinkProvider.defaultMac().equalsIgnoreCase(ourMac)) {
                return false;
            }
            // TODO Come up with more rubust way to identify bigswitch probe?
            return probe.getDeviceString().contains("bigswitch");
        }
    }
}
