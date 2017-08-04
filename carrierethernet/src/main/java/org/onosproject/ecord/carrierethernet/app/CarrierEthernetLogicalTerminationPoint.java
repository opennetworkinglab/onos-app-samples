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

import org.onosproject.net.ConnectPoint;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a CE Logical Termination Point.
 */
public class CarrierEthernetLogicalTerminationPoint {

    public enum Role {

        WORKING("Working"),
        PROTECTION("Protection"),
        PROTECTED("Protected"),
        SYMMETRIC("Symmetric"),
        HUB("Hub"),
        SPOKE("Spoke"),
        LEAF("Leaf"),
        // FIXME: Remove that after hackathon?
        TRUNK("Trunk"),
        ROOT("Root");

        private String value;

        Role(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    protected String ltpId;
    protected String ltpCfgId;
    protected Role role;
    // A global LTP will point to the corresponding global NI and a service LTP to the corresponding service NI
    protected CarrierEthernetNetworkInterface ni;

    public CarrierEthernetLogicalTerminationPoint(String ltpCfgId, CarrierEthernetNetworkInterface ni) {
        checkNotNull(ni);
        this.ni = ni;
        // NOTE: Role is expected to be null for global LTPs/NIs
        if (ni.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            this.role = (ni.role() == null ? null : Role.valueOf(((CarrierEthernetUni) ni).role().name()));
        } else if (ni.type().equals(CarrierEthernetNetworkInterface.Type.INNI)) {
            this.role = (ni.role() == null ? null : Role.valueOf(((CarrierEthernetInni) ni).role().name()));
        } else if (ni.type().equals(CarrierEthernetNetworkInterface.Type.ENNI)) {
            this.role = (ni.role() == null ? null : Role.valueOf(((CarrierEthernetEnni) ni).role().name()));
        }
        this.ltpId = this.cp().deviceId().toString() + "/" + this.cp().port().toString();
        this.ltpCfgId = (ltpCfgId == null ? this.ltpId : ltpCfgId);
    }

    public CarrierEthernetLogicalTerminationPoint(ConnectPoint cp, String ltpCfgId,
                                                  CarrierEthernetNetworkInterface.Type niType,
                                                  CarrierEthernetLogicalTerminationPoint.Role role) {
        this.ltpId = cp.deviceId().toString() + "/" + cp.port().toString();
        this.ltpCfgId = (ltpCfgId == null ? this.ltpId : ltpCfgId);
        this.role = role;
        // NOTE: Role is expected to be null for global LTPs/NIs
        // FIXME: Provide appropriate mapping between LTP and NI roles (e.g. ROOT -> HUB, LEAF -> SPOKE)
        if (niType.equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            CarrierEthernetUni.Role uniRole = (role == null) ? null : CarrierEthernetUni.Role.valueOf(role.name());
            this.ni = CarrierEthernetUni.builder()
                    .cp(cp)
                    .cfgId(ltpId)
                    .role(uniRole)
                    .build();
        } else if (niType.equals(CarrierEthernetNetworkInterface.Type.INNI))  {
            CarrierEthernetInni.Role inniRole = (role == null) ? null : CarrierEthernetInni.Role.valueOf(role.name());
            this.ni = CarrierEthernetInni.builder()
                    .cp(cp)
                    .cfgId(ltpId)
                    .role(inniRole)
                    .build();
        } else if (niType.equals(CarrierEthernetNetworkInterface.Type.ENNI)) {
            CarrierEthernetEnni.Role enniRole = (role == null) ? null : CarrierEthernetEnni.Role.valueOf(role.name());
            this.ni = CarrierEthernetEnni.builder()
                    .cp(cp)
                    .cfgId(ltpId)
                    .role(enniRole)
                    .build();
        }
    }

    /**
     * Returns associated connect point.
     *
     * @return associated connect point
     */
    public ConnectPoint cp() {
        return ni.cp();
    }

    /**
     * Returns LTP string identifier.
     *
     * @return LTP string identifier
     */
    public String id() {
        return ltpId;
    }

    /**
     * Returns LTP string config identifier.
     *
     * @return LTP string config identifier
     */
    public String cfgId() {
        return ltpCfgId;
    }

    /**
     * Returns LTP role - applicable only to service-specific LTPs.
     *
     * @return LTP role
     */
    public Role role() {
        return role;
    }

    /**
     * Returns LTP type.
     *
     * @return LTP type
     */
    public CarrierEthernetNetworkInterface.Type type() {
        return ni.type();
    }

    /**
     * Returns the NI associated with the LTP or null of there is none.
     *
     * @return NI associated with LTP
     */
    public CarrierEthernetNetworkInterface ni() {
        return ni;
    }

    /**
     * Returns the scope of the LTP (always matches that of the associated NI).
     *
     * @return LTP scope
     */
    public CarrierEthernetNetworkInterface.Scope scope() {
        return this.ni().scope();
    }

    /**
     * Returns counter with the number of references (from EVCs/FCs) to the associated NI.
     *
     * @return number of references counter
     */
    public AtomicInteger refCount() {
        return ni().refCount();
    }

    /**
     * Sets the NI associated with the LTP.
     *
     * @param ni the NI to set
     */
    public void setNi(CarrierEthernetNetworkInterface ni) {
        this.ni = ni;
    }

    /**
     * Sets LTP role - applicable only to EVC- or FC-specific LTPs.
     *
     * @param role the LTP role to set
     */
    public void setRole(Role role) {
        this.role = role;
    }

    public String toString() {

        return toStringHelper(this)
                .add("id", ltpId)
                .add("cfgId", ltpCfgId)
                .add("role", role)
                .add("ni", ni).toString();
    }

}
