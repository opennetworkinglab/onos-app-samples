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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceDirectory;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.onosproject.ecord.co.BigSwitchManager.REALIZED_BY;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Device provider which exposes a big switch abstraction of the underlying data path.
 */
@Component(immediate = true)
public class BigSwitchDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger LOG = getLogger(BigSwitchDeviceProvider.class);

    private static final String SCHEME = "bigswitch";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BigSwitchService bigSwitchService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RemoteServiceDirectory rpcService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgRegistry;

    private String dpidScheme = SCHEME;

    private String rpcScheme = "grpc";

    private int rpcPort = 11984;
    // Metro ONOS IP
    private String metroIp = "172.16.218.128";

    private BigSwitch bigSwitch;
    private DeviceDescription bigSwitchDescription;
    private DeviceProviderRegistry providerRegistry;
    private DeviceProviderService providerService;
    private BigSwitchListener listener = new InternalListener();

    private final ConfigFactory<ConnectPoint, CrossConnectConfig> xcConfigFactory
        = new ConfigFactory<ConnectPoint, CrossConnectConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                                                    CrossConnectConfig.class,
                                                    "cross-connect") {
            @Override
            public CrossConnectConfig createConfig() {
                return new CrossConnectConfig();
            }
        };

    @Activate
    public void activate() {
        // Create big switch device and description
        DeviceId deviceId = DeviceId.deviceId(dpidScheme + ':' + clusterService.getLocalNode().ip());
        bigSwitch = new BigSwitch(deviceId, this.id());
        buildDeviceDescription();
        // Register this device provider remotely
        // TODO: make remote configurable
        RemoteServiceContext remoteServiceContext
            = rpcService.get(URI.create(rpcScheme + "://" + metroIp + ":" + rpcPort));
        providerRegistry = remoteServiceContext.get(DeviceProviderRegistry.class);
        providerService = providerRegistry.register(this);

        NetworkConfigListener cfglistener = new InternalConfigListener();
        cfgRegistry.addListener(cfglistener);
        cfgRegistry.registerConfigFactory(xcConfigFactory);

        // Start big switch service and register device
        bigSwitchService.addListener(listener);
        registerDevice();

        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        unregisterDevice();
        cfgRegistry.unregisterConfigFactory(xcConfigFactory);
        providerRegistry.unregister(this);
        // Won't hurt but necessary?
        providerService = null;
        LOG.info("Stopped");
    }

    private void registerDevice() {
        providerService.deviceConnected(bigSwitch.id(), bigSwitchDescription);
        providerService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
        bigSwitchService.getPorts().stream()
            .forEach(this::advertiseCrossConnectLinks);
    }

    private void unregisterDevice() {
        providerService.deviceDisconnected(bigSwitch.id());
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
            .put(BasicLinkConfig.TYPE, "DIRECT");
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


        if (response.getStatusInfo() != Response.Status.OK) {
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

    private class InternalListener implements BigSwitchListener {
        @Override
        public void event(BigSwitchEvent event) {
            switch (event.type()) {
                case PORT_ADDED:
                case PORT_REMOVED:
                    providerService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
                    // if the subject's underlying port was a cross connect port,
                    // advertise cross-connect link to Metro-ONOS view
                    advertiseCrossConnectLinks(event.subject());
                    break;

                case PORT_UPDATED:
                    providerService.portStatusChanged(bigSwitch.id(), event.subject());
                    // if the subject's underlying port was a cross connect port,
                    // advertise cross-connect link to Metro-ONOS view
                    advertiseCrossConnectLinks(event.subject());
                    break;
                default:
                    break;
            }
        }
    }

    private void buildDeviceDescription() {
        bigSwitchDescription = new DefaultDeviceDescription(bigSwitch.id().uri(),
                bigSwitch.type(), bigSwitch.manufacturer(),
                bigSwitch.hwVersion(), bigSwitch.swVersion(), bigSwitch.serialNumber(), bigSwitch.chassisId());
    }

    /**
     * A big switch device provider implementation.
     */
    public BigSwitchDeviceProvider() {
        super(new ProviderId(SCHEME, "org.onosproject.bigswitch"));
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

    public class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == CrossConnectConfig.class) {
                bigSwitchService.getPorts().stream()
                    .forEach(BigSwitchDeviceProvider.this::advertiseCrossConnectLinks);
            }
        }
    }
}
