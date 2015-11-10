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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.net.URI;

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

    private BigSwitch bigSwitch;
    private DeviceDescription bigSwitchDescription;
    private DeviceProviderRegistry providerRegistry;
    private DeviceProviderService providerService;
    private BigSwitchListener listener = new InternalListener();

    @Activate
    public void activate() {
        // Create big switch device and description
        DeviceId deviceId = DeviceId.deviceId(SCHEME + ':' + clusterService.getLocalNode().ip());
        bigSwitch = new BigSwitch(deviceId, this.id());
        buildDeviceDescription();
        // Register this device provider remotely
        // TODO: make remote configurable
        RemoteServiceContext remoteServiceContext = rpcService.get(URI.create("local://localhost"));
        providerRegistry = remoteServiceContext.get(DeviceProviderRegistry.class);
        providerService = providerRegistry.register(this);
        // Start big switch service and register device
        bigSwitchService.addListener(listener);
        registerDevice();
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        unregisterDevice();
        providerRegistry.unregister(this);
        // Won't hurt but necessary?
        providerService = null;
        LOG.info("Stopped");
    }

    private void registerDevice() {
        providerService.deviceConnected(bigSwitch.id(), bigSwitchDescription);
        providerService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
    }

    private void unregisterDevice() {
        providerService.deviceDisconnected(bigSwitch.id());
    }

    private class InternalListener implements BigSwitchListener {
        @Override
        public void event(BigSwitchEvent event) {
            switch (event.type()) {
                case PORT_ADDED:
                case PORT_REMOVED:
                    providerService.updatePorts(bigSwitch.id(), bigSwitchService.getPorts());
                    break;
                case PORT_UPDATED:
                    providerService.portStatusChanged(bigSwitch.id(), event.subject());
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
}
