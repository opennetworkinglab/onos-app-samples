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

import com.google.common.base.Objects;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceService;
import org.onlab.osgi.DefaultServiceDirectory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a Carrier Ethernet Network Interface (UNI, INNI or ENNI).
 */
public abstract class CarrierEthernetNetworkInterface<E extends CarrierEthernetNetworkInterface> {

    protected DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);

    public enum Scope {
        GLOBAL, SERVICE
    }

    public enum Type {
        UNI, INNI, ENNI, GENERIC
    }

    protected ConnectPoint connectPoint;
    protected String id;
    protected String cfgId;
    protected Bandwidth capacity;
    protected Bandwidth usedCapacity;
    protected Scope scope;
    protected AtomicInteger refCount;
    protected Type type;


    public CarrierEthernetNetworkInterface(ConnectPoint connectPoint, Type type, String cfgId) {
        checkNotNull(connectPoint);
        checkNotNull(type);
        this.type = type;
        this.connectPoint = connectPoint;
        this.id = this.connectPoint.deviceId().toString() + "/" + this.connectPoint.port().toString();
        this.cfgId = (cfgId == null ? this.id : cfgId);
        this.capacity = Bandwidth.mbps(deviceService.getPort(connectPoint.deviceId(), connectPoint.port())
                .portSpeed());
        this.usedCapacity = Bandwidth.mbps((double) 0);
        this.scope = null;
        this.refCount = new AtomicInteger();
    }

    /**
     * Returns associated connect point.
     *
     * @return associated connect point
     */
    public ConnectPoint cp() {
        return connectPoint;
    }

    /**
     * Returns NI string identifier.
     *
     * @return NI string identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns NI string config identifier.
     *
     * @return NI string config identifier
     */
    public String cfgId() {
        return cfgId;
    }

    /**
     * Returns NI capacity.
     *
     * @return NI capacity
     */
    public Bandwidth capacity() {
        return capacity;
    }

    /**
     * Returns NI used capacity.
     *
     * @return NI used capacity
     */
    public Bandwidth usedCapacity() {
        return usedCapacity;
    }

    /**
     * Returns the NI S-TAG.
     *
     * @return NI S-TAG
     */
    public abstract VlanId sVlanId();

    /**
     * Returns the NI CE-VLAN ID.
     *
     * @return NI CE-VLAN ID
     */
    public abstract VlanId ceVlanId();

    /**
     * Returns the scope of the NI (GLOBAL or SERVICE).
     *
     * @return NI scope
     */
    public Scope scope() {
        return scope;
    }

    /**
     * Returns the type of the NI (UNI, INNI or ENNI).
     *
     * @return NI scope
     */
    public Type type() {
        return type;
    }

    /**
     * Returns counter with the number of references (from EVCs/FCs) to the particular NI.
     *
     * @return number of references counter
     */
    public AtomicInteger refCount() {
        return refCount;
    }

    /**
     * Sets NI string identifier.
     *
     * @param id the UNI string identifier to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets NI string config identifier.
     *
     * @param cfgId the UNI string config identifier to set
     */
    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
    }

    /**
     * Sets NI capacity.
     *
     * @param capacity the capacity to set
     */
    public void setCapacity(Bandwidth capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the NI role, depending on the NI.
     *
     * @param <T> the NI role
     * @return the NI role
     */
    public abstract <T> T role();

    /**
     * Adds the resources associated with an EVC- or FC-specific NI to a global NI.
     *
     * @param ni the EVC- or FC-specific NI to be added
     */
    public abstract void addEcNi(E ni);

    /**
     * Removes the resources associated with an EVC- or FC-specific NI from a global NI.
     *
     * @param ni the EVC- or FC-specific NI to be removed
     */
    public abstract void removeEcNi(E ni);

    /**
     * Validates whether an EVC- or FC-specific NI is compatible with the corresponding global NI.
     *
     * @param ni the EVC- or FC-specific NI
     * @return boolean value indicating whether the NIs are compatible
     */
    public abstract boolean validateEcNi(E ni);

    @Override
    public String toString() {

        return toStringHelper(this)
                .add("id", id)
                .add("cfgId", cfgId)
                .add("capacity", capacity)
                .add("usedCapacity", usedCapacity).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CarrierEthernetNetworkInterface that = (CarrierEthernetNetworkInterface) o;
        return Objects.equal(connectPoint, that.connectPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(connectPoint);
    }

}
