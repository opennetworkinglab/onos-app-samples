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

import java.util.Optional;
import java.util.Set;

/**
 * Service that allows the creation of L2 Virtual Circuits
 * between edge ports of a given SDN network.
 */
public interface SdxL2VCService {

    /**
     * Creates an L2 Virtual Circuit between two SDX-L2 Connection Points.
     *
     * @param sdxl2 name of SDX-L2
     * @param sdxl2cplhs name of SDX-L2 CP, left hand side of the VC
     * @param sdxl2cprhs name of SDX-L2 CP, right hand side of the VC
     */
    void addVC(String sdxl2, SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs);

    /**
     * Deletes a Virtual Circuit from a SDX-L2.
     *
     * @param sdxl2cplhs name of SDX-L2 CP, left hand side of the VC
     * @param sdxl2cprhs name of SDX-L2 CP, right hand side of the VC
     */
    void removeVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs);

    /**
     * Deletes a Virtual Circuit where a given SDX-L2 CP acts as endpoint.
     *
     * @param cp Connection Point
     */
    void removeVC(SdxL2ConnectionPoint cp);

    /**
     * Removes all Virtual Circuits created in a given SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     */
    void removeVCs(String sdxl2);

    /**
     * Returns Virtual Circuits (either all or those created in a given SDX-L2).
     *
     * @param sdxl2 name of SDX-L2
     * @return the set of virtual circuits name
     */
    Set<String> getVCs(Optional<String> sdxl2);

    /**
     * Returns an encoded Virtual Circuit in a given SDX-L2.
     *
     * @param lhs left hand side of the VC
     * @param rhs right hand side of the VC
     * @return encoded VC
     */
    String getVC(SdxL2ConnectionPoint lhs, SdxL2ConnectionPoint rhs);
}
