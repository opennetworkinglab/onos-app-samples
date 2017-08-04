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

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;
import java.time.Duration;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a Carrier Ethernet EVC.
 */
public final class CarrierEthernetVirtualConnection extends CarrierEthernetConnection {

    private Short shortId;
    private Set<CarrierEthernetUni> uniSet;
    private boolean isVirtual;
    private Integer maxNumUni;
    private Set<CarrierEthernetForwardingConstruct> fcSet;

    // Maximum possible number of UNIs for non-Point-to-Point EVCs
    public static final Integer MAX_NUM_UNI = 1000;

    // TODO: Remove id from constructor - currently used only when updating EVC
    private CarrierEthernetVirtualConnection(String id, String cfgId, Type type,
                                            Integer maxNumUni,
                                            Set<CarrierEthernetUni> uniSet,
                                            Duration maxLatency) {
        super(id, cfgId, type, maxLatency);
        this.maxNumUni = maxNumUni != null ? maxNumUni :
                type.equals(Type.POINT_TO_POINT) ? 2 : MAX_NUM_UNI;
        this.uniSet = new HashSet<>(uniSet);
        this.fcSet = new HashSet<>();
        this.shortId = null;
    }

    /**
     * Returns numerical identifier.
     *
     * @return numerical identifier
     */
    public Short shortId() {
        return shortId;
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
    public Integer maxNumUni() {
        return maxNumUni;
    }

    /**
     * Returns set of UNIs.
     *
     * @return set of UNIs
     */
    public Set<CarrierEthernetUni> uniSet() {
        return ImmutableSet.copyOf(uniSet);
    }

    /**
     * Returns the set of FCs associated with the EVC.
     *
     * @return set of FCs associated with the EVC
     */
    public Set<CarrierEthernetForwardingConstruct> fcSet() {
        return ImmutableSet.copyOf(fcSet);
    }

    /**
     * Set numerical identifier.
     *
     * @param shortId the numerical identifier to set
     */
    public void setShortId(Short shortId) {
        this.shortId = shortId;
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
     * Sets the set of FCs.
     *
     * @param fcSet the set of UNIs to be set
     */
    public void setFcSet(Set<CarrierEthernetForwardingConstruct> fcSet) {
        this.fcSet = fcSet;
    }

    /**
     * Sets the Virtual status of the service.
     *
     * @param isVirtual boolean value with the status to set
     */
    public void setIsVirtual(boolean isVirtual) {
        this.isVirtual = isVirtual;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("cfgId", cfgId)
                .add("type", type)
                .add("state", state)
                .add("UNIs", uniSet)
                .add("FCs", fcSet).toString();
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
     * Builder of CarrierEthernetVirtualConnection entities.
     */
    public static final class Builder {

        private String id;
        private String cfgId;
        private Type type;
        private Duration maxLatency;
        private Set<CarrierEthernetUni> uniSet;
        private Integer maxNumUni;

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
         * Sets the uniSet of this builder.
         *
         * @param uniSet the builder uniSet to set
         * @return this builder instance
         */
        public Builder uniSet(Set<CarrierEthernetUni> uniSet) {
            this.uniSet = uniSet;
            return this;
        }

        /**
         * Sets the maxNumUni of this builder.
         *
         * @param maxNumUni the builder maxNumUni to set
         * @return this builder instance
         */
        public Builder maxNumUni(Integer maxNumUni) {
            this.maxNumUni = maxNumUni;
            return this;
        }

        /**
         * Builds a new CarrierEthernetVirtualConnection instance.
         * based on this builder's parameters
         *
         * @return a new CarrierEthernetVirtualConnection instance
         */
        public CarrierEthernetVirtualConnection build() {
            checkNotNull(type, "EVC must have a type");
            checkArgument(uniSet != null && uniSet.size() > 1,
                          "EVC must include at least two UNIs");
            return new CarrierEthernetVirtualConnection(id, cfgId, type, maxNumUni, uniSet, maxLatency);
        }
    }
}
