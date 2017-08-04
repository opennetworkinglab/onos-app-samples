/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the Client.
 * You may obtain a copy of the Client at
 *
 *     http://www.apache.org/clients/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Client is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Client for the specific language governing permissions and
 * limitations under the Client.
 */

package org.onosproject.pppoe.api;

import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;


/**
 * Implementation of PPPoE client information.
 */
public class PppoeClientInfo implements PppoeDeviceInfo {

    private DeviceId deviceId;
    private PppoeAdminState adminState;
    private String serviceSpecificId;
    private short sVlan;
    private short cVlan;
    private boolean endSession;
    private boolean configured;
    private String subscriber;
    private PppoeSessionInfo session;
    private PppoeDeviceType type;

    public static enum PppoeAdminState {
        ENABLE,
        DISABLE
    }

    /**
     * Generates a PPPoE client information instance.
     */
    public PppoeClientInfo() {
        this.type = PppoeDeviceType.CLIENT;
    }

    /**
     * Generates a PPPoE client information instance.
     *
     * @param state administrative state
     * @param sVlan service provider VLAN
     * @param cVlan customer VLAN
     */
    public PppoeClientInfo(PppoeAdminState state, short sVlan, short cVlan) {
        this.type = PppoeDeviceType.CLIENT;
        this.adminState = state;
        this.sVlan = sVlan;
        this.cVlan = cVlan;
    }

    /**
     * Generates a PPPoE client information instance.
     *
     * @param state administrative state
     */
    public PppoeClientInfo(PppoeAdminState state) {
        this.type = PppoeDeviceType.CLIENT;
        this.adminState = state;
        this.configured = true;
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
     * Returns administrative state.
     *
     * @return administrative state
     */
    public PppoeAdminState adminState() {
        return adminState;
    }

    /**
     * Sets administrative state.
     *
     * @param state administrative state
     */
    public void setAdminState(PppoeAdminState state) {
        this.adminState = state;
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
     * Sets service specific ID.
     *
     * @param ssid service specific ID
     */
    public void setServiceSpecificId(String ssid) {
        this.serviceSpecificId = ssid;
    }

    /**
     * Returns service provider VLAN.
     *
     * @return service provider VLAN
     */
    public short sVlan() {
        return sVlan;
    }

    /**
     * Returns customer VLAN.
     *
     * @return customer VLAN
     */
    public short cVlan() {
        return cVlan;
    }

    /**
     * Returns end session flag.
     *
     * @return end session flag
     */
    public boolean endSession() {
        return endSession;
    }

    /**
     * Sets end session flag.
     *
     * @param flag flag to end session
     */
    public void setEndSession(boolean flag) {
        this.endSession = flag;
    }

    /**
     * Returns configured flag.
     *
     * @return configured flag
     */
    public boolean configured() {
        return configured;
    }

    /**
     * Sets configured flag.
     *
     * @param flag flag for configured
     */
    public void setConfigured(boolean flag) {
        this.configured = flag;
    }

    /**
     * Sets associated subscriber information.
     *
     * @param info subscriber information
     */
    public void setSubscriber(String info) {
        this.subscriber = info;
    }

    /**
     * Clears information from remote.
     */
    public void clear() {
        this.deviceId = null;
        this.sVlan = 0;
        this.cVlan = 0;
        this.subscriber = null;
    }

    @Override
    public String toString() {
        if (deviceId == null) {
            return toStringHelper("")
                    .add("ssid", serviceSpecificId)
                    .add("adminState", adminState.name().toLowerCase())
                    .toString();
        } else {
            return toStringHelper("")
                    .add("id", deviceId)
                    .add("ssid", serviceSpecificId)
                    .add("adminState", adminState.name().toLowerCase())
                    .add("sVlanId", sVlan)
                    .add("cVlanId", cVlan)
                    .add("subscriber", subscriber)
                    .toString();
        }
    }

}
