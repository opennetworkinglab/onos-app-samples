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
package org.onosproject.ecord.carrierethernet.app;

import java.time.Duration;

/**
 * Representation of an abstract CE connection (meant to be extended by the EVC and FC classes).
 */
public abstract class CarrierEthernetConnection {

    public enum Type {
        POINT_TO_POINT,
        MULTIPOINT_TO_MULTIPOINT,
        ROOT_MULTIPOINT
    }

    public enum State {
        ACTIVE,
        INACTIVE,
        PARTIAL
    }

    // TODO: Use the ONOS built-in identifier??
    protected String id;
    protected String cfgId;
    protected Type type;
    protected State state;
    protected Duration maxLatency;

    private static final Duration DEFAULT_MAX_LATENCY = Duration.ofMillis(50);

    // Note: id should be provided only when updating an existing connection
    public CarrierEthernetConnection(String id, String cfgId, Type type, Duration maxLatency) {
        this.id = id;
        this.cfgId = cfgId;
        this.type = type;
        this.state = State.INACTIVE;
        this.maxLatency = maxLatency == null ? DEFAULT_MAX_LATENCY : maxLatency;
    }

    /**
     * Returns connection identifier.
     *
     * @return connection identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns connection config identifier.
     *
     * @return connection config identifier
     */
    public String cfgId() {
        return cfgId;
    }

    /**
     * Returns type of connection.
     *
     * @return type of connection
     */
    public Type type() {
        return type;
    }

    /**
     * Returns connectivity state of the connection.
     *
     * @return connectivity state
     */
    public State state() {
        return state;
    }

    /**
     * Indicates whether the connection is active or partially active.
     *
     * @return connectivity state
     */
    public boolean isActive() {
        return !state.equals(State.INACTIVE);
    }

    /**
     * Returns maximum latency constraint.
     *
     * @return maximum latency constraint
     */
    public Duration maxLatency() {
        return maxLatency;
    }

    /**
     * Sets connection identifier.
     *
     * @param id the connection identifier to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets connection config identifier.
     *
     * @param cfgId connection config identifier
     */
    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
    }

    /**
     * Sets the connectivity state of the connection.
     *
     * @param state the connectivity state to set
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Sets the type of connection.
     *
     * @param type type of connection to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Sets maximum latency constraint.
     *
     * @param maxLatency the maximum latency constraint to set
     */
    public void setMaxLatency(Duration maxLatency) {
        this.maxLatency = maxLatency;
    }
}
