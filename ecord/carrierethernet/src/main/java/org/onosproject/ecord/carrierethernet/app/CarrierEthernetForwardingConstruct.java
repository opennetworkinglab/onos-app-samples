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

import org.onlab.packet.VlanId;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of a CE Forwarding Construct.
 */
public class CarrierEthernetForwardingConstruct extends CarrierEthernetConnection {

    private Set<CarrierEthernetLogicalTerminationPoint> ltpSet;
    private VlanId vlanId;
    private CarrierEthernetMetroConnectivity metroConnectivity;
    private boolean congruentPaths;
    protected AtomicInteger refCount;

    // Set to true if both directions should use the same path
    private static final boolean CONGRUENT_PATHS = true;

    // Note: fcId should be provided only when updating an existing FC
    public CarrierEthernetForwardingConstruct(String id, String cfgId, Type type,
                                              Set<CarrierEthernetLogicalTerminationPoint> ltpSet,
                                              Duration maxLatency) {
        super(id, cfgId, type, maxLatency);
        this.ltpSet = new HashSet<>(ltpSet);
        this.vlanId = null;
        this.metroConnectivity = new CarrierEthernetMetroConnectivity(null, OpticalPathEvent.Type.PATH_REMOVED);
        this.congruentPaths = CONGRUENT_PATHS;
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
     * Returns true if FC requires congruent paths.
     *
     * @return true if congruent paths required
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
}
