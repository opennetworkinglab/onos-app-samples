/*
 * Copyright 2016 Open Networking Foundation
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

import org.onlab.packet.VlanId;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a CE Forwarding Construct.
 */
public final class CarrierEthernetForwardingConstruct extends CarrierEthernetConnection {

    private Set<CarrierEthernetLogicalTerminationPoint> ltpSet;
    private VlanId vlanId;
    private CarrierEthernetMetroConnectivity metroConnectivity;
    private boolean congruentPaths = true;
    protected AtomicInteger refCount;

    // TODO: Remove id from constructor - currently used only when updating FC
    // TODO: Add congruentPaths flag to constructor and Builder
    private CarrierEthernetForwardingConstruct(String id, String cfgId, Type type,
                                              Set<CarrierEthernetLogicalTerminationPoint> ltpSet,
                                              Duration maxLatency) {
        super(id, cfgId, type, maxLatency);
        this.ltpSet = new HashSet<>(ltpSet);
        this.vlanId = null;
        this.metroConnectivity = new CarrierEthernetMetroConnectivity(null, OpticalPathEvent.Type.PATH_REMOVED);
        this.refCount = new AtomicInteger();
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
     * Gets metro connectivity id.
     *
     * @return the metro connectivity of the service
     */
    public CarrierEthernetMetroConnectivity metroConnectivity() {
        return this.metroConnectivity;
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
     * Returns the set of UNIs associated with the FC.
     *
     * @return set of UNIs associated with the FC
     */
    public Set<CarrierEthernetUni> uniSet() {
        // FIXME: Find a more efficient way to get the FC UNIs
        return ltpSet
                .stream()
                .filter(ltp -> ltp.type().equals(CarrierEthernetNetworkInterface.Type.UNI))
                .map(ltp -> (CarrierEthernetUni) ltp.ni()).collect(Collectors.toSet());
    }

    /**
     * Returns true if FC requires that both directions should use the same path.
     *
     * @return true if both directions should use the same path
     */
    public boolean congruentPaths() {
        return congruentPaths;
    }

    /**
     * Returns counter with the number of references (from EVCs) to the particular FC.
     *
     * @return number of references counter
     */
    public AtomicInteger refCount() {
        return refCount;
    }

    /**
     * Sets the vlanId to be used by the FC.
     *
     * @param vlanId the vlanId to set
     */
    public void setVlanId(VlanId vlanId) {
        this.vlanId = vlanId;
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
                .add("id", id)
                .add("cfgId", cfgId)
                .add("type", type)
                .add("vlanId", vlanId)
                .add("metroConnectId", (metroConnectivity.id() == null ? "null" : metroConnectivity.id().id()))
                .add("refCount", refCount)
                .add("LTPs", ltpSet).toString();
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of CarrierEthernetForwardingConstruct entities.
     */
    public static final class Builder {

        private String id;
        private String cfgId;
        private Type type;
        private Duration maxLatency;
        private Set<CarrierEthernetLogicalTerminationPoint> ltpSet;

        /**
         * Sets the id of this builder.
         *
         * @param id the builder id to set
         * @return this builder instance
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the cfgId of this builder.
         *
         * @param cfgId the builder cfgId to set
         * @return this builder instance
         */
        public Builder cfgId(String cfgId) {
            this.cfgId = cfgId;
            return this;
        }

        /**
         * Sets the type of this builder.
         *
         * @param type the builder type to set
         * @return this builder instance
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the maxLatency of this builder.
         *
         * @param maxLatency the builder maxLatency to set
         * @return this builder instance
         */
        public Builder maxLatency(Duration maxLatency) {
            this.maxLatency = maxLatency;
            return this;
        }

        /**
         * Sets the ltpSet of this builder.
         *
         * @param ltpSet the builder ltpSet to set
         * @return this builder instance
         */
        public Builder ltpSet(Set<CarrierEthernetLogicalTerminationPoint> ltpSet) {
            this.ltpSet = ltpSet;
            return this;
        }

        /**
         * Builds a new CarrierEthernetForwardingConstruct instance.
         * based on this builder's parameters
         *
         * @return a new CarrierEthernetForwardingConstruct instance
         */
        public CarrierEthernetForwardingConstruct build() {
            checkNotNull(type, "FC must have a type");
            checkArgument(ltpSet != null && ltpSet.size() > 1,
                          "FC must include at least two LTPs");

            return new CarrierEthernetForwardingConstruct(id, cfgId, type, ltpSet, maxLatency);
        }
    }
}
