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

package org.onosproject.pppoe.driver;

import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.api.PppoeClientInfo;
import org.onosproject.pppoe.api.PppoeServerInfo;
import org.onosproject.pppoe.api.PppoeSessionInfo;


/**
 * Behaviour for handling device information retrieval and configuration
 * for PPPoE devices.
 */
public interface PppoeDeviceConfig extends HandlerBehaviour {

    /**
     * Retrieve PPPoE device information.
     *
     * @return device information
     */
    PppoeDeviceInfo getDevice();

    /**
     * Retrieve PPPoE server information of PPPoE device.
     *
     * @return server information
     */
    PppoeServerInfo getServer();

    /**
     * Retrieve PPPoE client information of PPPoE device.
     *
     * @return client information
     */
    PppoeClientInfo getClient();

    /**
     * Retrieve PPPoE session data of PPPoE device.
     *
     * @return session information
     */
    PppoeSessionInfo readSessionData();

    /**
     * Update PPPoE server of PPPoE device.
     *
     * @param serverInfo server information to update
     * @return true - success, false - failure
     */
    boolean configServer(PppoeServerInfo serverInfo);

    /**
     * Update PPPoE client of PPPoE device.
     *
     * @param clientInfo client information to update
     * @return true - success, false - failure
     */
    boolean configClient(PppoeClientInfo clientInfo);

}
