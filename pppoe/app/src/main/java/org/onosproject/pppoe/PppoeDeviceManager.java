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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.DeviceId;
import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.driver.PppoeDeviceConfig;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * PPPoE Management application to manage information of PPPoE device(s).
 */
public class PppoeDeviceManager {

    private final Logger log = getLogger(getClass());
    private final DriverService driverService;


    /**
     * Creates PPPoE Device Manager instance.
     *
     * @param driverService {@link DriverService} to be used
     */
    public PppoeDeviceManager(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * Retrieves basic PPPoE device information from remote.
     *
     * @param deviceId device identifier
     * @return PPPoE device information
     */
    public PppoeDeviceInfo getDeviceInfo(DeviceId deviceId) {
        DriverService driverService = DefaultServiceDirectory.getService(DriverService.class);

        DriverHandler handler = driverService.createHandler(deviceId);
        PppoeDeviceConfig behaviour = handler.behaviour(PppoeDeviceConfig.class);

        PppoeDeviceInfo deviceInfo = behaviour.getDevice();
        if (deviceInfo == null) {
            log.warn("getDevice() failed for {}", deviceId);
        }
        return deviceInfo;
    }

}
