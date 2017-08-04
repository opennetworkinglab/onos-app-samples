/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the Server.
 * You may obtain a copy of the Server at
 *
 *     http://www.apache.org/servers/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Server is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Server for the specific language governing permissions and
 * limitations under the Server.
 */

package org.onosproject.pppoe.api;

import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Implementation of PPPoE server information.
 */
public class PppoeServerInfo implements PppoeDeviceInfo {

    private DeviceId deviceId;
    private String radiusIp;
    private String radiusKey;
    private PppoeDeviceType type;

    /**
     * Generates a PPPoE server information instance.
     */
    public PppoeServerInfo() {
        this.type = PppoeDeviceType.SERVER;
    }

    /**
     * Generates a PPPoE server information instance.
     *
     * @param ip IP address of RADIUS server
     * @param key shared secret key of RADIUS server
     */
    public PppoeServerInfo(String ip, String key) {
        this.type = PppoeDeviceType.SERVER;
        checkNotNull(ip);
        checkNotNull(key);

        this.radiusIp = ip;
        this.radiusKey = key;
    }

    @Override
    public PppoeDeviceType type() {
        return type;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns IP address of RADIUS server.
     *
     * @return IP address of RADIUS server
     */
    public String radiusIp() {
        return radiusIp;
    }

    /**
     * Sets IP address of RADIUS server.
     *
     * @param ip IP address of RADIUS server
     */
    public void setRadiusIp(String ip) {
        this.radiusIp = ip;
    }

    /**
     * Returns shared secret key for RADIUS server.
     *
     * @return shared secret key for RADIUS server
     */
    public String radiusKey() {
        return radiusKey;
    }

    /**
     * Sets shared secret key for RADIUS server.
     *
     * @param key shared secret key for RADIUS server
     */
    public void setRadiusKey(String key) {
        this.radiusKey = key;
    }

    @Override
    public String toString() {
        return toStringHelper("")
                .add("radiusIp", radiusIp)
                .add("radiusKey", radiusKey)
                .toString();
    }

}
