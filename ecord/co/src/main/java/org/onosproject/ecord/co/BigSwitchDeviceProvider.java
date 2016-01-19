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
import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
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
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.osgi.service.component.ComponentContext;

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
import java.util.Dictionary;

import static org.onosproject.ecord.co.BigSwitchManager.REALIZED_BY;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/* To configure the BigSwitchDevice parameters at startup, add configuration to tools/package/config/component-cfg.json
   before using onos-package command.
   Configuration example:
   {
     "org.onosproject.ecord.co.BigSwitchDeviceProvider": {
       "providerScheme": "bigswitch",
       "providerId": "org.onosproject.bigswitch",
       "remoteUri": "local://localhost",
       "metroIp": "localhost"
     }
   }
 */

/**
 * Device provider which exposes a big switch abstraction of the underlying data path.
 */
@Component(immediate = true)
public class BigSwitchDeviceProvider implements DeviceProvider {

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

        registerToDeviceProvider();
        NetworkConfigListener cfglistener = new InternalConfigListener();
        cfgRegistry.addListener(cfglistener);
        cfgRegistry.registerConfigFactory(xcConfigFactory);

        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgRegistry.unregisterConfigFactory(xcConfigFactory);
        cfgService.unregisterProperties(getClass(), false);

        unregisterFromDeviceProvider();
        // Won't hurt but necessary?
        providerService = null;
        providerId = null;
        LOG.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        // Needs re-registration to DeviceProvider
        if (loadRpcConfig(context)) {
            // unregister from DeviceProvider with old parameters
            unregisterFromDeviceProvider();
            // register to DeviceProvider with new parameters
            registerToDeviceProvider();

            LOG.info("Re-registered to DeviceProvider");
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
        RemoteServiceContext remoteServiceContext;
        // TODO Can validation of the URI be done while loading configuration?
        try {
            remoteServiceContext = rpcService.get(URI.create(remoteUri));
        } catch (UnsupportedOperationException e) {
            LOG.error("Unsupported URI: {}", remoteUri);
            return;
        }

        providerId = new ProviderId(schemeProp, idProp);
        // Create big switch device and description
        DeviceId deviceId = DeviceId.deviceId(schemeProp + ':' + clusterService.getLocalNode().ip());
        bigSwitch = new BigSwitch(deviceId, this.id());
        bigSwitchDescription = new DefaultDeviceDescription(bigSwitch.id().uri(),
                bigSwitch.type(), bigSwitch.manufacturer(),
                bigSwitch.hwVersion(), bigSwitch.swVersion(), bigSwitch.serialNumber(), bigSwitch.chassisId());
        providerRegistry = remoteServiceContext.get(DeviceProviderRegistry.class);
        providerService = providerRegistry.register(this);
        // Start big switch service and register device
        providerService.deviceConnected(bigSwitch.id(), bigSwitchDescription);
        providerService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
        advertiseCrossConnectLinksOnAllPorts();
        bigSwitchService.addListener(listener);
    }

    private void unregisterFromDeviceProvider() {
        if (bigSwitch == null) {
            LOG.warn("Invalid unregistration.");
            return;
        }
        providerService.deviceDisconnected(bigSwitch.id());
        providerRegistry.unregister(this);

        bigSwitch = null;
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
        bigSwitchService.getPorts().stream()
            .forEach(BigSwitchDeviceProvider.this::advertiseCrossConnectLinks);
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

    public class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == CrossConnectConfig.class) {
                advertiseCrossConnectLinksOnAllPorts();
            }
        }
    }
}
