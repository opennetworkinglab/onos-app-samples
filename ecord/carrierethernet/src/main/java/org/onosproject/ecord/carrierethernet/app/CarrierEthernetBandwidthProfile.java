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

import org.onlab.util.Bandwidth;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Bandwidth profile for a CE UNI.
 */
public class CarrierEthernetBandwidthProfile {

    public enum Type {
        INTERFACE, EVC, COS
    }

    protected String id;
    protected String cfgId;
    protected Type type;
    protected Bandwidth cir;
    protected Bandwidth eir;
    protected long cbs;
    protected long ebs;

    public CarrierEthernetBandwidthProfile(String id, String cfgId, Type type,
                                           Bandwidth cir, Bandwidth eir, long cbs, long ebs) {
        this.id = id;
        this.cfgId = cfgId;
        this.type = type;
        this.cir = cir;
        this.eir = eir;
        this.cbs = cbs;
        this.ebs = ebs;
    }

    /**
     * Returns BWP string identifier.
     *
     * @return BWP string identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns BWP string config identifier.
     *
     * @return BWP string config identifier
     */
    public String cfgId() {
        return cfgId;
    }

    /**
     * Returns BWP type (INTERFACE, EVC, COS).
     *
     * @return BWP type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns BWP CIR rate.
     *
     * @return BWP CIR rate
     */
    public Bandwidth cir() {
        return cir;
    }

    /**
     * Returns BWP EIR rate.
     *
     * @return BWP EIR rate
     */
    public Bandwidth eir() {
        return eir;
    }

    /**
     * Returns BWP CBS in Bytes.
     *
     * @return BWP CBS in Bytes
     */
    public long cbs() {
        return cbs;
    }

    /**
     * Returns BWP EBS in Bytes.
     *
     * @return BWP EBS in Bytes
     */
    public long ebs() {
        return ebs;
    }

    /**
     * Sets BWP id.
     *
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets BWP CIR rate.
     *
     * @param cir the CIR to set
     */
    public void setCir(Bandwidth cir) {
        this.cir = cir;
    }

    /**
     * Sets BWP EIR rate.
     *
     * @param eir the EIR to set
     */
    public void setEir(Bandwidth eir) {
        this.eir = eir;
    }

    public String toString() {

        return toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("cir", cir)
                .add("cbs", cbs)
                .add("eir", eir)
                .add("ebs", ebs).toString();
    }

}
