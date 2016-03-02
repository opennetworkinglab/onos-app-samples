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
import org.onosproject.ecord.metro.api.MetroConnectivityId;
import org.onosproject.ecord.metro.api.MetroPathEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of a Carrier Ethernet Service along with relevant ONOS-related resources.
 */
public class CarrierEthernetService {

    public enum Type {
        POINT_TO_POINT, MULTIPOINT_TO_MULTIPOINT, ROOT_MULTIPOINT
    }

    protected String serviceId;
    protected String serviceCfgId;
    protected Type serviceType;
    protected VlanId vlanId;
    protected boolean isVirtual;
    protected Set<CarrierEthernetUni> uniSet;
    protected Duration latency;
    protected CarrierEthernetServiceMetroConnectivity metroConnectivity;
    protected boolean congruentPaths;

    // Set to true if both directions should use the same path
    private static final boolean CONGRUENT_PATHS = true;

    private static final Duration DEFAULT_LATENCY = Duration.ofMillis(50);

    // Note: serviceId should be provided only when updating an existing service
    public CarrierEthernetService(String serviceId, String serviceCfgId, Type serviceType,
                                  Set<CarrierEthernetUni> uniSet) {
        this.serviceId = serviceId;
        this.serviceCfgId = serviceCfgId;
        this.serviceType = serviceType;
        this.vlanId = null;
        this.uniSet = new HashSet<>();
        this.uniSet.addAll(uniSet);
        this.congruentPaths = CONGRUENT_PATHS;
        this.latency = DEFAULT_LATENCY;
        this.metroConnectivity = new CarrierEthernetServiceMetroConnectivity(null, MetroPathEvent.Type.PATH_REMOVED);
    }

    /**
     * Returns service identifier.
     *
     * @return service identifier
     */
    public String id() {
        return serviceId;
    }

    /**
     * Returns service config identifier.
     *
     * @return service config identifier
     */
    public String cfgId() {
        return serviceCfgId;
    }

    /**
     * Returns type of service.
     *
     * @return type of service
     */
    public Type type() {
        return serviceType;
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
     * Returns set of UNIs.
     *
     * @return set of UNIs
     */
    public Set<CarrierEthernetUni> uniSet() {
        return uniSet;
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
        this.serviceId = serviceId;
    }

    /**
     * Sets service config identifier.
     *
     * @param serviceCfgId service config identifier
     */
    public void setCfgId(String serviceCfgId) {
        this.serviceCfgId = serviceCfgId;
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
    public CarrierEthernetServiceMetroConnectivity metroConnectivity() {
        return this.metroConnectivity;
    }

    /**
     * Sets metro connectivity id.
     *
     * @param id the metro connectivity identifier to set
     */
    public void setMetroConnectivityId(MetroConnectivityId id) {
        this.metroConnectivity.setId(id);
    }

    /**
     * Sets metro connectivity status.
     *
     * @param status the metro connectivity status
     */
    public void setMetroConnectivityStatus(MetroPathEvent.Type status) {
        this.metroConnectivity.setStatus(status);
    }

    public String toString() {

        return toStringHelper(this)
                .add("id", serviceId)
                .add("cfgId", serviceCfgId)
                .add("type", serviceType)
                .add("vlanId", vlanId)
                .add("metroConnectId", (metroConnectivity.id() == null ? "null" : metroConnectivity.id().value()))
                .add("UNIs", uniSet).toString();
    }

    class CarrierEthernetServiceMetroConnectivity {

        // TODO: In the future this may be replaced by a connectivity intent
        // FIXME: Need to keep a set of MetroConnectivityIds

        private MetroConnectivityId id;
        private MetroPathEvent.Type status;

        CarrierEthernetServiceMetroConnectivity(MetroConnectivityId id, MetroPathEvent.Type status) {
            this.id = id;
            this.status = status;
        }

        public MetroConnectivityId id() {
            return this.id;
        }

        public MetroPathEvent.Type status() {
            return this.status;
        }

        public void setId(MetroConnectivityId id) {
            this.id = id;
        }

        public void setStatus(MetroPathEvent.Type status) {
            this.status = status;
        }

    }

}
