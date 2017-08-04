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

package org.onosproject.pppoe;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.MastershipRole;
import org.opencord.olt.AccessDeviceService;
import org.onosproject.pppoe.api.PppoeService;
import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.api.PppoeClientInfo;
import org.onosproject.pppoe.api.PppoeServerInfo;
import org.onosproject.pppoe.api.PppoeSessionInfo;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onosproject.pppoe.api.PppoeDeviceInfo.PppoeDeviceType;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * PPPoE Management application.
 */
@Component(immediate = true)
@Service
public class PppoeManager implements PppoeService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AccessDeviceService accessDeviceService;

    private static final String DEFAULT_RADIUS_SERVER_IP = "192.168.122.150";
    @Property(name = RADIUS_IP_PROPERTY_NAME, value = DEFAULT_RADIUS_SERVER_IP,
            label = "RADIUS server IP address")
    protected String radiusServer = DEFAULT_RADIUS_SERVER_IP;

    private static final String DEFAULT_RADIUS_SHARED_KEY = "default";
    @Property(name = RADIUS_KEY_PROPERTY_NAME, value = DEFAULT_RADIUS_SHARED_KEY,
            label = "RADIUS shared secret key")
    protected String radiusKey = DEFAULT_RADIUS_SHARED_KEY;

    private static final long DEFAULT_POLL_DELAY_SECONDS = 1;

    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 20;
    @Property(name = POLL_INTERVAL_PROPERTY_NAME, intValue = DEFAULT_POLL_INTERVAL_SECONDS,
            label = "Frequency (in seconds) for polling client devices")
    protected int pollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;

    private static final String RADIUS_IP_PROPERTY_NAME = "radiusIp";
    private static final String RADIUS_KEY_PROPERTY_NAME = "radiusKey";
    private static final String POLL_INTERVAL_PROPERTY_NAME = "pollInterval";
    private static final String PPPOE_APPLICATION_NAME = "org.onosproject.pppoe";
    private static final String PPPOE_DRIVER_NAME = "rest-pppoe";
    private static final String DEVICE_PROTOCOL_REST = "REST";

    private ScheduledExecutorService pollExecutor = newSingleThreadScheduledExecutor();

    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private PppoeDeviceManager deviceManager;
    private PppoeClientManager clientManager;
    private PppoeServerManager serverManager;

    // Devices available & active upon discovery
    private ConcurrentMap<DeviceId, PppoeDeviceInfo> devices = new ConcurrentHashMap<>();


    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        readComponentConfiguration(context);

        deviceManager = new PppoeDeviceManager(driverService);
        serverManager = new PppoeServerManager(driverService, radiusServer, radiusKey);
        clientManager = new PppoeClientManager(deviceService, driverService,
                                mastershipService, accessDeviceService);

        ScheduledFuture<?> pollTask =
                pollExecutor.scheduleAtFixedRate(new InternalPollTask(),
                DEFAULT_POLL_DELAY_SECONDS, pollIntervalSeconds, TimeUnit.SECONDS);

        coreService.registerApplication(PPPOE_APPLICATION_NAME);
        deviceService.addListener(deviceListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        cfgService.unregisterProperties(getClass(), false);
        pollExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public PppoeServerInfo getPppoeServer() {
        return serverManager.getServer();
    }

    @Override
    public boolean setPppoeServer(String paramName, String paramValue) {
        return serverManager.configServer(paramName, paramValue);
    }

    @Override
    public Collection<PppoeClientInfo> getPppoeClients(String ssid) {
        return clientManager.getClients(ssid);
    }

    @Override
    public boolean setPppoeClient(String ssid, String paramName, String paramValue) {
        return clientManager.configClient(ssid, paramName, paramValue);
    }

    @Override
    public Collection<PppoeSessionInfo> getPppoeSessions() {
        return clientManager.getSessions();
    }

    /**
     * Extracts properties from the component configuration context.
     * (on activation only)
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        if (context == null) {
            log.info("No component configuration");
            // All configurable parameters use default values
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        String propertyName;
        String strValue;
        int intValue;

        propertyName = RADIUS_IP_PROPERTY_NAME;
        strValue = Tools.get(properties, propertyName);
        if (!isNullOrEmpty(strValue)) {
            radiusServer = strValue;
        }
        log.info("Settings: {} = {}", propertyName, radiusServer);

        propertyName = RADIUS_KEY_PROPERTY_NAME;
        strValue = Tools.get(properties, propertyName);
        if (!isNullOrEmpty(strValue)) {
            radiusKey = strValue;
        }
        log.info("Settings: {} = {}", propertyName, radiusKey);

        propertyName = POLL_INTERVAL_PROPERTY_NAME;
        strValue = Tools.get(properties, propertyName);
        intValue = isNullOrEmpty(strValue) ?
                pollIntervalSeconds : Integer.parseInt(strValue.trim());
        pollIntervalSeconds = intValue;
        log.info("Settings: {} = {}", propertyName, pollIntervalSeconds);
    }

    /**
     * Handle PPPOE device that became available.
     *
     * @param deviceId device identifier
     */
    private void handleDeviceAvailable(DeviceId deviceId) {
        checkNotNull(deviceId);
        pollDevice(deviceId, true);
    }

    /**
     * Poll PPPOE device to retrieve information and update.
     *
     * @param deviceId device identifier
     */
    private synchronized void pollDevice(DeviceId deviceId, boolean event) {
        PppoeDeviceInfo deviceInfo = deviceManager.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            // Mark the device available
            devices.put(deviceId, deviceInfo);
            log.info("pollDevice(): Device {} Type {}",
                    deviceId, deviceInfo.type());
            if (deviceInfo.type() == PppoeDeviceType.SERVER) {
                serverManager.addServer(deviceInfo);
            } else {
                clientManager.addClient(deviceInfo);
            }
        }
    }

    /**
     * Remove unavailable PPPoE device.
     *
     * @param deviceId PPPoE device identifier
     */
    private void removeDevice(DeviceId deviceId) {
        synchronized (devices) {
            PppoeDeviceInfo deviceInfo = devices.get(deviceId);
            if ((deviceInfo != null) && (deviceInfo.type().equals(PppoeDeviceType.CLIENT))) {
                clientManager.removeClient(deviceInfo);
            }
            devices.remove(deviceId);
        }
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            Driver driver = driverService.getDriver(deviceId);

            if (!driver.name().equals(PPPOE_DRIVER_NAME)) {
                log.debug("event(): Ignore for other driver: device {} {}", driver.name(), deviceId);
                return;
            }

            Device device = deviceService.getDevice(deviceId);
            if (device == null) {
                log.warn("event(): Unknown device {}", deviceId);
                return;
            }
            if (device.annotations() == null) {
                log.warn("event(): No annotations {}", deviceId);
                return;
            }
            String protocol = device.annotations().value(AnnotationKeys.PROTOCOL);
            if ((protocol == null) || (!protocol.equals(DEVICE_PROTOCOL_REST))) {
                // Ignore non-REST device.
                return;
            }

            log.info("event(): {} {} {} {}", deviceId, event.type(),
                deviceService.isAvailable(deviceId), deviceService.getRole(deviceId));
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if ((deviceService.isAvailable(deviceId))
                            && (deviceService.getRole(deviceId).equals(MastershipRole.MASTER))) {
                        handleDeviceAvailable(deviceId);
                    } else {
                        // Mark device unavailable
                        removeDevice(deviceId);
                        log.info("event(): Device removed: {}", deviceId);
                    }
                    break;
                default:
                    log.warn("event(): Unsupported event {} {}", deviceId, event.type());
                    break;
            }
        }
    }

    /**
     * Implements PPPoE client device poll task to find SSID based
     * on retrieved information from client.
     */
    private final class InternalPollTask implements Runnable {

        @Override
        public void run() {
            List<PppoeDeviceInfo> deviceInfoList = ImmutableList.copyOf(devices.values());
            for (PppoeDeviceInfo deviceInfo : deviceInfoList) {
                log.debug("run(): {}", deviceInfo.deviceId());
                if (deviceInfo.type().equals(PppoeDeviceType.CLIENT)) {
                    PppoeClientInfo clientInfo = (PppoeClientInfo) deviceInfo;
                    if ((clientInfo == null) || (isNullOrEmpty(clientInfo.serviceSpecificId()))) {
                        // Retry to collect client info
                        pollDevice(deviceInfo.deviceId(), false);
                    }
                }
            }
        }
    }

}
