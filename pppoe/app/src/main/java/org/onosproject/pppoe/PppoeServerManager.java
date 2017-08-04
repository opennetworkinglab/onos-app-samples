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

import com.google.common.net.InetAddresses;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.api.PppoeServerInfo;
import org.onosproject.pppoe.driver.PppoeDeviceConfig;
import org.slf4j.Logger;

import java.net.InetAddress;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;


/**
 * PPPoE Management application to manage information of PPPoE server.
 */
public class PppoeServerManager {

    private final Logger log = getLogger(getClass());

    private final DriverService driverService;

    private static final String RADIUS_IP = "radius-ip";

    private PppoeServerInfo serverInfo;


    /**
     * Creates PPPoE Server Manager instance.
     *
     * @param driverService {@link DriverService} to be used
     * @param radiusIp radius server IP address
     * @param radiusKey radius server shared secret key
     */
    public PppoeServerManager(DriverService driverService, String radiusIp, String radiusKey) {
        this.driverService = driverService;
        serverInfo = new PppoeServerInfo(radiusIp, radiusKey);
    }

    /**
     * Retrieves PPPoE server information.
     *
     * @return PPPoE server information
     */
    public PppoeServerInfo getServer() {
        return serverInfo;
    }

    /**
     * Configures/Changes PPPoE server information.
     *
     * @param paramName parameters name to be changed
     * @param paramValue parameters value to be changed
     * @return true or false
     */
    public boolean configServer(String paramName, String paramValue) {
        if (paramName.equals(RADIUS_IP)) {
            try {
                InetAddress inetAddress = InetAddresses.forString(paramValue);
            } catch (IllegalArgumentException e) {
                checkArgument(false, "Invalid IP address string %s", paramValue);
            }
            serverInfo.setRadiusIp(paramValue);
        } else {
            serverInfo.setRadiusKey(paramValue);
        }
        if (serverInfo.deviceId() != null) {
            return updateServer(serverInfo);
        } else {
            return true;
        }
    }

    /**
     * Add PPPoE server device.
     *
     * @param deviceInfo PPPoE device information
     * @return true or false
     */
    public boolean addServer(PppoeDeviceInfo deviceInfo) {
        serverInfo.setDeviceId(deviceInfo.deviceId());
        return updateServer(serverInfo);
    }

    /**
     * Changes PPPoE server information of remote device.
     *
     * @param serverInfo PPPoE server information
     * @return true or false
     */
    private boolean updateServer(PppoeServerInfo serverInfo) {
        DriverHandler handler = driverService.createHandler(serverInfo.deviceId());
        PppoeDeviceConfig behaviour = handler.behaviour(PppoeDeviceConfig.class);

        boolean result = behaviour.configServer(serverInfo);
        if (!result) {
            log.error("configServer() failed: Device {}", serverInfo.deviceId());
        }
        return result;
    }

}
