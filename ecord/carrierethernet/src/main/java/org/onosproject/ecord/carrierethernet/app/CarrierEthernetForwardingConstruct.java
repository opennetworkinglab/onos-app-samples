/*
 * Copyright 2016 Open Networking Laboratory
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

import org.onosproject.newoptical.api.OpticalPathEvent;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of a CE Forwarding Construct.
 */
public class CarrierEthernetForwardingConstruct {

    private final Logger log = getLogger(getClass());

    public enum State {

        ACTIVE("Active"),
        INACTIVE("Inactive");

        private String value;

        State(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static State fromString(String value) {
            if (value != null) {
                for (State b : State.values()) {
                    if (value.equals(b.value)) {
                        return b;
                    }
                }
            }
            throw new IllegalArgumentException("State " + value + " is not valid");
        }
    }

    public enum ActiveState {

        FULL("Full"),
        PARTIAL("Partial");

        private String value;

        ActiveState(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    protected String fcId;
    protected String fcCfgId;
    protected String evcId;
    protected CarrierEthernetVirtualConnection.Type evcType;
    protected Set<CarrierEthernetLogicalTerminationPoint> ltpSet;
    protected CarrierEthernetForwardingConstruct.State state;
    protected CarrierEthernetForwardingConstruct.ActiveState activeState;
    protected Duration latency;
    protected CarrierEthernetMetroConnectivity metroConnectivity;
    protected boolean congruentPaths;

    // FIXME: Find a better way
    protected CarrierEthernetVirtualConnection evcLite;

    // Set to true if both directions should use the same path
    private static final boolean CONGRUENT_PATHS = true;

    private static final Duration DEFAULT_LATENCY = Duration.ofMillis(50);

    // TODO: Maybe fcCfgId and evcId are not needed?
    // Note: fcId should be provided only when updating an existing FC
    public CarrierEthernetForwardingConstruct(String fcId, String fcCfgId,
                                              String evcId, CarrierEthernetVirtualConnection.Type evcType,
                                              Set<CarrierEthernetLogicalTerminationPoint> ltpSet) {
        this.fcId = fcId;
        this.fcCfgId = (fcCfgId == null? fcId : fcCfgId);
        this.evcId = evcId;
        this.evcType = evcType;
        this.state = State.INACTIVE;
        this.ltpSet = new HashSet<>(ltpSet);
        this.congruentPaths = CONGRUENT_PATHS;
        this.latency = DEFAULT_LATENCY;
        this.metroConnectivity = new CarrierEthernetMetroConnectivity(null, OpticalPathEvent.Type.PATH_REMOVED);

        // FIXME: This is (probably) just a temporary solution
        // Create a lightweight EVC out of the FC which can be used with existing methods
        Set<CarrierEthernetNetworkInterface> niSet = new HashSet<>();
        ltpSet.forEach(ltp -> {
            if (ltp.ni().type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                niSet.add(ltp.ni());
            }
        });
        this.evcLite = new CarrierEthernetVirtualConnection(fcId, fcCfgId, evcType, null, niSet);
    }

    // TODO: Create constructor using the End-to-End service and a set of LTPs

    public String toString() {

        return toStringHelper(this)
                .add("id", fcId)
                .add("cfgId", fcCfgId)
                .add("evcId", evcId)
                .add("evcType", evcType)
                .add("metroConnectId", (metroConnectivity.id() == null ? "null" : metroConnectivity.id().id()))
                .add("LTPs", ltpSet).toString();
    }

    /**
     * Returns the id of the FC.
     *
     * @return id of the FC
     */
    public String id() {
        return fcId;
    }

    /**
     * Returns the set of LTPs associated with the FC.
     *
     * @return set of LTPs associated with the FC
     */
    public Set<CarrierEthernetLogicalTerminationPoint> ltpSet() {
        return ltpSet;
    }

    /**
     * Returns the type of the EVC associated with the FC.
     *
     * @return type of associated EVC
     */
    public CarrierEthernetVirtualConnection.Type evcType() {
        return evcType;
    }

    /**
     * Returns connectivity state of the FC.
     *
     * @return connectivity state
     */
    public State state() {
        return state;
    }

    /**
     * Returns active connectivity state of the FC.
     *
     * @return active connectivity state
     */
    public ActiveState activeState() {
        return activeState;
    }

    /**
     * Returns the "EVC" associated with FC.
     *
     * @return the "EVC" associated with FC
     */
    public CarrierEthernetVirtualConnection evcLite() { return evcLite; }

    /**
     * Sets the id of the FC.
     *
     * @param id the id to set to the FC
     */
    public void setId(String id) {
        this.fcId = id;
    }

    /**
     * Sets the set of LTPs.
     *
     * @param ltpSet the set of LTPs to be set
     */
    public void setLtpSet(Set<CarrierEthernetLogicalTerminationPoint> ltpSet) {
        this.ltpSet = ltpSet;
    }

    /**
     * Sets the connectivity state of the FC.
     *
     * @param state the connectivity state to set
     */
    public void setState(State state) { this.state = state; }

    /**
     * Sets the active connectivity state of the FC.
     *
     * @param activeState the active connectivity state to set
     */
    public void setActiveState(ActiveState activeState) { this.activeState = activeState; }

}
