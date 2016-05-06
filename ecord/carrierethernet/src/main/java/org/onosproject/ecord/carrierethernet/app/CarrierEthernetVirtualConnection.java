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

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.VlanId;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of a Carrier Ethernet Service along with relevant ONOS-related resources.
 */
// FIXME: Consider renaming, since it actually represents a service rather than an EVC.
// FIXME: Term "Service" though might be confusing in the ONOS context.
public class CarrierEthernetVirtualConnection {

    public enum Type {

        POINT_TO_POINT("Point_To_Point"),
        MULTIPOINT_TO_MULTIPOINT("Multipoint_To_Multipoint"),
        ROOT_MULTIPOINT("Root_Multipoint");

        private String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static Type fromString(String value) {
            if (value != null) {
                for (Type b : Type.values()) {
                    if (value.equals(b.value)) {
                        return b;
                    }
                }
            }
            throw new IllegalArgumentException("Type " + value + " is not valid");
        }
    }

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

    // FIXME: single vlanId is a hack for ONS2016.  CE service must store vlanId for each CO.
    protected String evcId;
    protected String evcCfgId;
    protected Type evcType;
    protected State evcState;
    protected ActiveState evcActiveState;
    protected VlanId vlanId;
    protected boolean isVirtual;
    protected Integer maxNumUni;
    protected Set<CarrierEthernetUni> uniSet;
    protected Duration latency;
    protected CarrierEthernetMetroConnectivity metroConnectivity;
    protected boolean congruentPaths;

    // Set to true if both directions should use the same path
    private static final boolean CONGRUENT_PATHS = true;

    private static final Duration DEFAULT_LATENCY = Duration.ofMillis(50);

    // Maximum possible number of UNIs for non-Point-to-Point EVCs
    public static final Integer MAX_NUM_UNI = 1000;

    // Note: evcId should be provided only when updating an existing service
    public CarrierEthernetVirtualConnection(String evcId, String evcCfgId, Type evcType, Integer maxNumUni,
                                            Set<CarrierEthernetUni> uniSet) {
        this.evcId = evcId;
        this.evcCfgId = evcCfgId;
        this.evcType = evcType;
        this.evcState = State.INACTIVE;
        this.evcActiveState = null;
        this.maxNumUni = (maxNumUni != null ? maxNumUni : (evcType.equals(Type.POINT_TO_POINT) ? 2 : MAX_NUM_UNI));
        this.vlanId = null;
        this.uniSet = new HashSet<>(uniSet);
        this.congruentPaths = CONGRUENT_PATHS;
        this.latency = DEFAULT_LATENCY;
        this.metroConnectivity = new CarrierEthernetMetroConnectivity(null, OpticalPathEvent.Type.PATH_REMOVED);
    }

    /**
     * Returns service identifier.
     *
     * @return service identifier
     */
    public String id() {
        return evcId;
    }

    /**
     * Returns service config identifier.
     *
     * @return service config identifier
     */
    public String cfgId() {
        return evcCfgId;
    }

    /**
     * Returns type of service.
     *
     * @return type of service
     */
    public Type type() {
        return evcType;
    }

    /**
     * Returns connectivity state of the EVC.
     *
     * @return connectivity state
     */
    public State state() {
        return evcState;
    }

    /**
     * Returns active connectivity state of the EVC.
     *
     * @return active connectivity state
     */
    public ActiveState activeState() {
        return evcActiveState;
    }

    /**
     * Returns Vlan id.
     *
     * @return Vlan id
     */
    public VlanId vlanId() {
        return vlanId;
    }

    /**
     * Returns the Virtual status of the service (i.e. if all UNIs have CE-VLAN ids).
     *
     * @return true if service is virtual, false otherwise
     */
    public boolean isVirtual() {
        return isVirtual;
    }

    /**
     * Returns the maximum number of UNIs in the EVC.
     *
     * @return true the maximum number of UNIs in the EVC
     */
    public Integer maxNumUni() { return maxNumUni; }

    /**
     * Returns set of UNIs.
     *
     * @return set of UNIs
     */
    public Set<CarrierEthernetUni> uniSet() {
        return ImmutableSet.copyOf(uniSet);
    }

    /**
     * Returns latency constraint.
     *
     * @return latency constraint
     */
    public Duration latency() {
        return latency;
    }

    /**
     * Returns true if service requires congruent paths.
     *
     * @return true if congruent paths required
     */
    public boolean congruentPaths() {
        return congruentPaths;
    }

    /**
     * Sets service identifier.
     *
     * @param serviceId the service identifier to set
     */
    public void setId(String serviceId) {
        this.evcId = serviceId;
    }

    /**
     * Sets service config identifier.
     *
     * @param serviceCfgId service config identifier
     */
    public void setCfgId(String serviceCfgId) {
        this.evcCfgId = serviceCfgId;
    }

    /**
     * Sets the set of UNIs.
     *
     * @param uniSet the set of UNIs to be set
     */
    public void setUniSet(Set<CarrierEthernetUni> uniSet) {
        this.uniSet = uniSet;
    }

    /**
     * Sets the connectivity state of the EVC.
     *
     * @param evcState the connectivity state to set
     */
    public void setState(State evcState) { this.evcState = evcState; }

    /**
     * Sets the active connectivity state of the EVC.
     *
     * @param evcActiveState the active connectivity state to set
     */
    public void setActiveState(ActiveState evcActiveState) { this.evcActiveState = evcActiveState; }

    /**
     * Sets the value of the congruent paths parameter.
     *
     * @param congruentPaths the congruent paths parameter value to set
     */
    public void setCongruentPaths(boolean congruentPaths) {
        this.congruentPaths = congruentPaths;
    }

    /**
     * Sets the vlanId to be used by the service.
     *
     * @param vlanId the vlanId to set
     */
    public void setVlanId(VlanId vlanId) {
        this.vlanId = vlanId;
    }

    /**
     * Sets the Virtual status of the service.
     *
     * @param isVirtual boolean value with the status to set
     */
    public void setIsVirtual(boolean isVirtual) {
        this.isVirtual = isVirtual;
    }

    /**
     * Gets metro connectivity id.
     *
     * @return the metro connectivity of the service
     */
    public CarrierEthernetMetroConnectivity metroConnectivity() {
        return this.metroConnectivity;
    }

    /**
     * Sets metro connectivity id.
     *
     * @param id the metro connectivity identifier to set
     */
    public void setMetroConnectivityId(OpticalConnectivityId id) {
        this.metroConnectivity.setId(id);
    }

    /**
     * Sets metro connectivity status.
     *
     * @param status the metro connectivity status
     */
    public void setMetroConnectivityStatus(OpticalPathEvent.Type status) {
        this.metroConnectivity.setStatus(status);
    }

    public String toString() {

        return toStringHelper(this)
                .add("id", evcId)
                .add("cfgId", evcCfgId)
                .add("type", evcType)
                .add("vlanId", vlanId)
                .add("metroConnectId", (metroConnectivity.id() == null ? "null" : metroConnectivity.id().id()))
                .add("UNIs", uniSet).toString();
    }
}
