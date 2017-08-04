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

import org.onosproject.net.DeviceId;


/**
 * Implementation of PPPoE device information.
 */
public interface PppoeDeviceInfo {

    static enum PppoeDeviceType {
        SERVER,
        CLIENT,
        UNKNOWN
    }

    /**
     * Returns device type.
     *
     * @return device type
     */
    public PppoeDeviceType type();

    /**
     * Returns device identifier.
     *
     * @return device identifier
     */
    public DeviceId deviceId();

    /**
     * Sets device identifier.
     *
     * @param deviceId device identifier
     */
    public void setDeviceId(DeviceId deviceId);

}
