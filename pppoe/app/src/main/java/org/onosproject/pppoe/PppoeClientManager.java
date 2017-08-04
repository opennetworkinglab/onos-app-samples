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
import org.onlab.packet.VlanId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.DeviceId;
import org.opencord.cordconfig.access.AccessDeviceData;
import org.opencord.olt.AccessDeviceService;
import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.api.PppoeClientInfo;
import org.onosproject.pppoe.api.PppoeSessionInfo;
import org.onosproject.pppoe.driver.PppoeDeviceConfig;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.pppoe.api.PppoeClientInfo.PppoeAdminState;
import static org.onosproject.pppoe.api.PppoeDeviceInfo.PppoeDeviceType;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * PPPoE Management application to manage information of PPPoE client(s).
 */
public class PppoeClientManager {

    private final Logger log = getLogger(getClass());

    private final DeviceService deviceService;
    private final DriverService driverService;
    private final AccessDeviceService accessDeviceService;
    private final MastershipService mastershipService;

    private static final String ADMIN_STATE = "admin-state";
    private static final PppoeAdminState DEFAULT_ADMIN_STATE = PppoeAdminState.ENABLE;

    private ConcurrentMap<String, PppoeClientInfo> clients = new ConcurrentHashMap<>();

    private boolean ssidFound;

    /**
     * Creates PPPoE Client Manager instance.
     *
     * @param deviceService {@link DeviceService} to be used
     * @param driverService {@link DriverService} to be used
     * @param mastershipService {@link MastershipService} to be used
     * @param accessDeviceService {@link AccessDeviceService} to be used.
     */
    public PppoeClientManager(DeviceService deviceService, DriverService driverService,
                              MastershipService mastershipService, AccessDeviceService accessDeviceService) {
        this.deviceService = deviceService;
        this.driverService = driverService;
        this.mastershipService = mastershipService;
        this.accessDeviceService = accessDeviceService;
        this.ssidFound = false;
    }

    /**
     * Configures/Changes PPPoE client information.
     *
     * @param ssid service specific ID
     * @param paramName parameters name from user
     * @param paramValue parameters value from user
     * @return true or false
     */
    public boolean configClient(String ssid, String paramName, String paramValue) {
        if (paramName.equals(ADMIN_STATE)) {
            try {
                PppoeAdminState state = PppoeAdminState.valueOf(paramValue.toUpperCase());

                PppoeClientInfo clientInfo = clients.get(ssid);
                if (clientInfo == null) {
                    // Client configuration for undiscovered SSID
                    clientInfo = new PppoeClientInfo(state);
                    clientInfo.setServiceSpecificId(ssid);
                    clients.put(ssid, clientInfo);
                    return true;
                }
                clientInfo.setAdminState(state);
                clientInfo.setConfigured(true);
                if (clientInfo.deviceId() == null) {
                    return true;
                }
                return updateClient(clientInfo);
            } catch (Exception e) {
                checkArgument(false, "Invalid parameter value %s", paramValue);
                return false;
            }
        } else {
            PppoeClientInfo clientInfo = clients.get(ssid);
            if ((clientInfo == null) || (clientInfo.deviceId() == null)) {
                log.error("No PPPoE client device for SSID {}", ssid);
                return false;
            }
            clientInfo.setEndSession(true);
            return updateClient(clientInfo);
        }
    }

    /**
     * Retrieves one or more PPPoE client information.
     *
     * @param ssid service specific ID if exists
     * @return colletion of PPPoE client information
     */
    public Collection<PppoeClientInfo> getClients(String ssid) {
        if (ssid == null) {
            return ImmutableList.copyOf(clients.values());
        }

        PppoeClientInfo clientInfo = clients.get(ssid);
        if (clientInfo == null) {
            return Collections.emptyList();
        }

        List<PppoeClientInfo> clientList = new ArrayList<PppoeClientInfo>();
        clientList.add(clientInfo);
        return clientList;
    }

    /**
     * Retrieves one or more PPPoE clients' session information.
     *
     * @return colletion of PPPoE clients' session information
     */
    public Collection<PppoeSessionInfo> getSessions() {
        List<PppoeSessionInfo> sessionList = new ArrayList<PppoeSessionInfo>();

        clients.values().forEach(clientInfo -> {
            PppoeSessionInfo sessionInfo = getSession(clientInfo);
            if (sessionInfo != null) {
                sessionList.add(sessionInfo);
            }
        });
        return sessionList;
    }

    /**
     * Retrieves client information from PPPoE client, searches matching SSID,
     * finds configuration information and updates remote PPPoE client.
     *
     * @param deviceInfo PPPoE device information
     */
    public void addClient(PppoeDeviceInfo deviceInfo) {
        PppoeClientInfo clientInfo = getClient(deviceInfo.deviceId());
        if (clientInfo == null) {
            log.error("Failed to get client info from Device {}", deviceInfo.deviceId());
            return;
        }

        if (!findServiceSpecificId(clientInfo)) {
            log.warn("Could not find SSID for Device {} VLAN {}:{}",
                    deviceInfo.deviceId(), clientInfo.sVlan(), clientInfo.cVlan());
            return;
        }

        String ssid = clientInfo.serviceSpecificId();
        clientInfo.setDeviceId(deviceInfo.deviceId());
        PppoeClientInfo configInfo = clients.get(ssid);
        if (configInfo == null) {
            clientInfo.setAdminState(DEFAULT_ADMIN_STATE);
        } else {
            clients.remove(ssid);
            clientInfo.setAdminState(configInfo.adminState());
            clientInfo.setConfigured(true);
        }
        clients.put(ssid, clientInfo);

        log.info("addClient(): Device {} SSID {} VLAN {}:{} Admin {}",
                clientInfo.deviceId(), clientInfo.serviceSpecificId(),
                clientInfo.sVlan(), clientInfo.cVlan(), clientInfo.adminState());
        updateClient(clientInfo);
    }

    /**
     * Removes PPPoE client information if automatically added during
     * device discovery.
     *
     * @param deviceInfo PPPoE device information
     */
    public void removeClient(PppoeDeviceInfo deviceInfo) {
        log.info("removeClient(): Device {}", deviceInfo.deviceId());
        if ((deviceInfo != null) && deviceInfo.type().equals(PppoeDeviceType.CLIENT)) {
            PppoeClientInfo clientInfo = (PppoeClientInfo) deviceInfo;
            if ((clientInfo != null) && (!isNullOrEmpty(clientInfo.serviceSpecificId()))) {
                if (clientInfo.configured()) {
                    log.info("removeClient(): configured SSID {}",
                            clientInfo.serviceSpecificId());
                    // Clear information filled during device discovery
                    clientInfo.clear();
                } else {
                    log.info("removeClient(): auto SSID {} VLAN {}:{}",
                            clientInfo.serviceSpecificId(),
                            clientInfo.sVlan(), clientInfo.cVlan());
                    // Added during device discovery
                    clients.remove(clientInfo.serviceSpecificId());
                }
            }
        }
    }

    /**
     * Searches matching SSID for PPPoE client.
     *
     * @param clientInfo PPPoE client information
     * @return true or false
     */
    private boolean findServiceSpecificId(PppoeClientInfo clientInfo) {
        Map<DeviceId, AccessDeviceData> olts = accessDeviceService.fetchOlts();
        Collection<Map.Entry<ConnectPoint, VlanId>> subscribers = accessDeviceService.getSubscribers();
        setSsidFound(false);

        olts.keySet().forEach(did -> {
            if (!getSsidFound() && (olts.get(did).vlan().toShort() == clientInfo.sVlan())) {
                String oltId = did.toString();
                subscribers.forEach(subscriber -> {
                    ConnectPoint cp = subscriber.getKey();
                    if (!getSsidFound() && oltId.equals(cp.elementId().toString()) &&
                            (subscriber.getValue().toShort() == clientInfo.cVlan())) {
                        deviceService.getPorts(did).forEach(port -> {
                            if (!getSsidFound() && (port.number().toLong() == cp.port().toLong())) {
                                String ssid = port.annotations().value(PORT_NAME);
                                clientInfo.setServiceSpecificId(ssid);
                                clientInfo.setSubscriber(cp.toString());
                                // SSID found
                                log.info("Found SSID: {}", clientInfo.serviceSpecificId());
                                setSsidFound(true);
                            }
                        });
                    }
                });
            }
        });
        return getSsidFound();
    }

    /**
     * Retrieves PPPoE client information from remote device.
     *
     * @param deviceId PPPoE device identifier
     * @return PPPoE client information
     */
    private PppoeClientInfo getClient(DeviceId deviceId) {
        if (deviceId == null) {
            return null;
        }
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.warn("Not master for Device {}", deviceId);
            return null;
        }

        DriverHandler handler = driverService.createHandler(deviceId);
        PppoeDeviceConfig behaviour = handler.behaviour(PppoeDeviceConfig.class);

        PppoeClientInfo clientInfo = behaviour.getClient();
        if (clientInfo == null) {
            log.error("getClient() failed: Device {}", deviceId);
        }
        return clientInfo;
    }

    /**
     * Retrieves PPPoE client session information from remote device.
     *
     * @param clientInfo PPPoE client information
     * @return PPPoE session information
     */
    private PppoeSessionInfo getSession(PppoeClientInfo clientInfo) {
        if (clientInfo.deviceId() == null) {
            return null;
        }
        if (!mastershipService.isLocalMaster(clientInfo.deviceId())) {
            log.warn("Not master for Device {}", clientInfo.deviceId());
            return null;
        }

        DriverHandler handler = driverService.createHandler(clientInfo.deviceId());
        PppoeDeviceConfig behaviour = handler.behaviour(PppoeDeviceConfig.class);

        PppoeSessionInfo sessionInfo = behaviour.readSessionData();
        if (sessionInfo == null) {
            log.error("getSession() failed: Device {}", clientInfo.deviceId());
            return null;
        }

        sessionInfo = new PppoeSessionInfo(sessionInfo, clientInfo.deviceId(), clientInfo.serviceSpecificId());
        log.info("Session {}", sessionInfo);
        return sessionInfo;
    }

    /**
     * Changes PPPoE client information of remote device.
     *
     * @param clientInfo PPPoE client information
     * @return true or false
     */
    private boolean updateClient(PppoeClientInfo clientInfo) {
        if (!mastershipService.isLocalMaster(clientInfo.deviceId())) {
            log.warn("Not master for Device {}", clientInfo.deviceId());
            clientInfo.setEndSession(false);
            return false;
        }

        DriverHandler handler = driverService.createHandler(clientInfo.deviceId());
        PppoeDeviceConfig behaviour = handler.behaviour(PppoeDeviceConfig.class);

        boolean result = behaviour.configClient(clientInfo);
        if (!result) {
            log.error("configClient() failed: Device {}", clientInfo.deviceId());
        }
        return result;
    }

    /**
     * Set state of Searches matching SSID for PPPoE client.
     *
     * @param found state of SSID Searching
     */
    private void setSsidFound(boolean found) {
        ssidFound = found;
    }

    /**
     * Get state of Searches matching SSID for PPPoE client.
     *
     * @return true or false
     */
    private boolean getSsidFound() {
        return ssidFound;
    }

}
