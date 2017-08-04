/*
 * Copyright 2015 Open Networking Foundation
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

import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

/**
 * Convenience class for big switch devices.
 */
public class BigSwitch extends DefaultDevice {
    private static final String ONLAB_MANUFACTURER = "ON.Lab";
    private static final String HW_VERSION = "CORD BigSwitch";
    private static final String SW_VERSION = "v1";
    private static final String SERIAL = "v1";
    private static final ChassisId CHASSIS = new ChassisId();

    /**
     * Creates a big switch instance with given device ID and provider ID.
     *
     * @param deviceId the device ID
     * @param providerId the provider ID
     */
    public BigSwitch(DeviceId deviceId, ProviderId providerId) {
        super(providerId, deviceId, Type.VIRTUAL, ONLAB_MANUFACTURER, HW_VERSION, SW_VERSION, SERIAL, CHASSIS);
    }
}
