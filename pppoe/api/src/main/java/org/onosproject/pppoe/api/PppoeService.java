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

package org.onosproject.pppoe.api;

import java.util.Collection;


/**
 * Service used to interact with PPPoE devices.
 */
public interface PppoeService {

    /**
     * Retrieves PPPoE server information.
     *
     * @return PPPoE server information
     */
    PppoeServerInfo getPppoeServer();

    /**
     * Retrieves PPPoE server information.
     *
     * @param paramName PPPoE server information paramName.
     * @param paramValue PPPoE server information paramValue.
     * @return true or false
     */
    boolean setPppoeServer(String paramName, String paramValue);

    /**
     * Retrieves all PPPoE client information.
     *
     * @param ssid service specific ID (ONU serial number)
     * @return a list of PPPoE clients' information
     */
    Collection<PppoeClientInfo> getPppoeClients(String ssid);

    /**
     * Requests to change PPPoE client information.
     *
     * @param ssid service specific ID (ONU serial number)
     * @param paramName PPPoE server information paramName.
     * @param paramValue PPPoE server information paramValue.
     * @return true or false
     */
    boolean setPppoeClient(String ssid, String paramName, String paramValue);

    /**
     * Retrieves all PPPoE session information.
     *
     * @return a list of PPPoE sessions' information
     */
    Collection<PppoeSessionInfo> getPppoeSessions();

}
