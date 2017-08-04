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

import org.junit.Before;
import org.junit.Test;


import org.onosproject.net.driver.DriverService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.mastership.MastershipService;
import org.opencord.olt.AccessDeviceService;

import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.api.PppoeClientInfo;
import org.onosproject.pppoe.api.PppoeServerInfo;
import org.onosproject.pppoe.api.PppoeSessionInfo;

import java.util.Collection;

import static org.junit.Assert.*;
import static org.onosproject.pppoe.api.PppoeDeviceInfo.PppoeDeviceType.*;
import static org.onosproject.pppoe.api.PppoeClientInfo.PppoeAdminState;

/**
 * Unit tests for PppoeClientManager class.
 */
public class PppoeClientManagerTest {
    private DriverService driverService;
    private DeviceService deviceService;
    private MastershipService mastershipService;
    private AccessDeviceService accessDeviceService;
    private PppoeClientManager clientManager;
    private String ssid;

    private static final String RADIUSSERVER = "192.168.122.150";
    private static final String RADIUSKEY = "default";

    @Before
    public void setUp() throws Exception {
        clientManager = new PppoeClientManager(deviceService, driverService, mastershipService, accessDeviceService);
        ssid = "0123456789abcd";
    }

    /**
     * Config client when session is not established.
     */
    @Test
    public void testconfigClientSessionNotEstablish() throws Exception {
        assertTrue("Incorrect response", clientManager.configClient(ssid, "admin-state", "enable"));
        assertFalse("Incorrect response", clientManager.configClient(ssid, "end-session", ""));
    }

    /**
     * Get client info when client rest server is not exist.
     */
    @Test
    public void testgetClientsRestServerNotExist() throws Exception {
        assertEquals("Incorrect response", 0, clientManager.getClients(null).size());
        assertEquals("Incorrect response", 0, clientManager.getClients(ssid).size());
    }

    /**
     * Get session info, session info is empty.
     */
    @Test
    public void testgetSessionsSsidNull() throws Exception {
        Collection<PppoeSessionInfo> sessionList;
        sessionList = clientManager.getSessions();
        assertEquals("Incorrect response", 0, sessionList.size());
    }

    /**
     * Add client when device type is client and client info is null.
     */
    @Test
    public void testaddClientDeviceTypeClient() throws Exception {
        PppoeDeviceInfo pppoeDeviceInfo = new PppoeClientInfo(PppoeAdminState.ENABLE);
        try {
            clientManager.addClient(pppoeDeviceInfo);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Remove client when device type is client and client info is null.
     */
    @Test
    public void testremoveClientDeviceTypeClient() throws Exception {
        PppoeDeviceInfo pppoeDeviceInfo = new PppoeClientInfo(PppoeAdminState.ENABLE);
        try {
            clientManager.removeClient(pppoeDeviceInfo);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Remove client when device type is not client and client info is null.
     */
    @Test
    public void testremoveClientDeviceTypeNotClient() throws Exception {

        PppoeDeviceInfo pppoeDeviceInfo = new PppoeServerInfo(RADIUSSERVER, RADIUSKEY);
        try {
            clientManager.removeClient(pppoeDeviceInfo);
        } catch (Exception e) {
            fail();
        }
    }
}

