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

package org.onosproject.pppoe.api;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import static com.google.common.base.MoreObjects.toStringHelper;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.pppoe.api.PppoeClientInfo.PppoeAdminState.*;

/**
 * Unit tests for PppoeClientInfo class.
 */
public class PppoeClientInfoTest {
    private final short testsVlan = 16;
    private final short testcVlan = 125;
    private final PppoeClientInfo.PppoeAdminState state = ENABLE;
    private final PppoeClientInfo client = new PppoeClientInfo(state, testsVlan, testcVlan);
    private final String ssid = "0123456789abcd";

    /**
     * input DeviceId is null.
     */
    @Test
    public void testtoStringDeviceidNull() throws Exception {
        final String expectStr, outputStr;
        // Set deviceId null.
        client.setDeviceId(null);
        client.setServiceSpecificId(ssid);
        // Create expect string.
        expectStr = toStringHelper("")
                .add("ssid", ssid)
                .add("adminState", state.name().toLowerCase())
                .toString();
        // Call toString().
        outputStr = client.toString();
        assertEquals("unexpected string", expectStr, outputStr);
    }

    /**
     * input DeviceId is not null.
     */
    @Test
    public void testtoStringDeviceidNotNull() throws Exception {
        final String expectStr, outputStr;
        final DeviceId deviceid = deviceId("rest:10.6.1.133:3000");
        // Set deviceId not null.
        client.setDeviceId(deviceid);
        client.setServiceSpecificId(ssid);
        // Create expect string.
        expectStr = toStringHelper("")
                .add("id", deviceid)
                .add("ssid", ssid)
                .add("adminState", state.name().toLowerCase())
                .add("sVlanId", testsVlan)
                .add("cVlanId", testcVlan)
                .add("subscriber", null)
                .toString();
        // Call toString().
        outputStr = client.toString();
        assertEquals("unexpected string", expectStr, outputStr);
    }

}
