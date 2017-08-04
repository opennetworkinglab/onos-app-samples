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

package org.onosproject.sdxl2;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Abstraction of a L2 Virtual Circuit.
 * Note the circuit is expressed as a composition of two SDX-L2 Connection Points.
 */
public class VirtualCircuit {

    private final String name;
    private final SdxL2ConnectionPoint sdxl2cplhs;
    private final SdxL2ConnectionPoint sdxl2cprhs;

    /**
     * Creates a new Virtual Circuit.
     *
     * @param sdxl2cplhs left hand side of the virtual circuit
     * @param sdxl2cprhs right hand side of the virtual circuit
     */
    public VirtualCircuit(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs) {
        this.name = sdxl2cplhs.name() + "-" + sdxl2cprhs.name();
        this.sdxl2cplhs = sdxl2cplhs;
        this.sdxl2cprhs = sdxl2cprhs;
    }

    /**
     * Returns the left hand side of the Virtual Circuit.
     *
     * @return SDX-L2 Connection Point
     */
    public SdxL2ConnectionPoint lhs() {
        return sdxl2cplhs;
    }

    /**
     * Returns the right hand side of the Virtual Circuit.
     *
     * @return SDX-L2 Connection Point
     */
    public SdxL2ConnectionPoint rhs() {
        return sdxl2cprhs;
    }

    @Override
    public int hashCode() {
        return sdxl2cplhs.hashCode() + sdxl2cprhs.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VirtualCircuit) {
            final VirtualCircuit other = (VirtualCircuit) obj;
            return (Objects.equals(this.sdxl2cplhs, other.sdxl2cplhs) &&
                    Objects.equals(this.sdxl2cprhs, other.sdxl2cprhs)) ||
                    (Objects.equals(this.sdxl2cplhs, other.sdxl2cprhs) &&
                            Objects.equals(this.sdxl2cprhs, other.sdxl2cplhs));
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("lhs", sdxl2cplhs)
                .add("rhs", sdxl2cprhs)
                .toString();
    }
}
