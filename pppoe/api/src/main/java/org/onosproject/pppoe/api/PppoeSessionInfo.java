/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the Session.
 * You may obtain a copy of the Session at
 *
 *     http://www.apache.org/sessions/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Session is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Session for the specific language governing permissions and
 * limitations under the Session.
 */

package org.onosproject.pppoe.api;

import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Implementation of PPPoE session information.
 */
public class PppoeSessionInfo {

    private final DeviceId deviceId;
    private final String serviceSpecificId;
    private final String ip;
    private final long rxPackets;
    private final long txPackets;
    private final long rxBytes;
    private final long txBytes;

    /**
     * Generates a PPPoE session information instance.
     *
     * @param ip IP address of RADIUS session
     * @param rxPackets number of packets received
     * @param txPackets number of packets transmitted
     * @param rxBytes number of bytes received
     * @param txBytes number of bytes transmitted
     */
    public PppoeSessionInfo(String ip, long rxPackets, long txPackets, long rxBytes, long txBytes) {
        checkNotNull(ip);
        checkNotNull(rxPackets);
        checkNotNull(txPackets);
        checkNotNull(rxBytes);
        checkNotNull(txBytes);

        this.ip = ip;
        this.rxPackets = rxPackets;
        this.txPackets = txPackets;
        this.rxBytes = rxBytes;
        this.txBytes = txBytes;
        this.deviceId = null;
        this.serviceSpecificId = null;
    }

    /**
     * Generates a PPPoE session information instance.
     *
     * @param sessionInfo PPPoE session information
     * @param deviceId device identifier
     * @param ssid service specific ID
     */
    public PppoeSessionInfo(PppoeSessionInfo sessionInfo, DeviceId deviceId, String ssid) {
        checkNotNull(sessionInfo);
        checkNotNull(deviceId);
        checkNotNull(ssid);

        this.ip = sessionInfo.ip();
        this.rxPackets = sessionInfo.rxPackets();
        this.txPackets = sessionInfo.txPackets();
        this.rxBytes = sessionInfo.rxBytes();
        this.txBytes = sessionInfo.txBytes();
        this.deviceId = deviceId;
        this.serviceSpecificId = ssid;
    }

    /**
     * Returns device identifier.
     *
     * @return device identifier
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns service specific ID.
     *
     * @return service specific ID
     */
    public String serviceSpecificId() {
        return serviceSpecificId;
    }

    /**
     * Returns assigned IP address.
     *
     * @return assigned IP address
     */
    public String ip() {
        return ip;
    }

    /**
     * Returns number of packets received.
     *
     * @return number of packets received
     */
    public long rxPackets() {
        return rxPackets;
    }

    /**
     * Returns number of packets transmitted.
     *
     * @return number of packets transmitted
     */
    public long txPackets() {
        return txPackets;
    }

    /**
     * Returns number of bytes received.
     *
     * @return number of bytes received
     */
    public long rxBytes() {
        return rxBytes;
    }

    /**
     * Returns number of bytes transmitted.
     *
     * @return number of bytes transmitted
     */
    public long txBytes() {
        return txBytes;
    }

    @Override
    public String toString() {
        return toStringHelper("")
                .add("ssid", serviceSpecificId)
                .add("ip", ip)
                .add("rxPackets", rxPackets)
                .add("txPackets", txPackets)
                .add("rxBytes", rxBytes)
                .add("txBytes", txBytes)
                .toString();
    }

}
