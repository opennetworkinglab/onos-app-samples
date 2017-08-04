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
 * Storage service for SDX-L2 application.
 */
public interface SdxL2Store {

    /**
     * Creates a named SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     * @throws SdxL2Exception if SDX-L2 is not added
     */
    void putSdxL2(String sdxl2) throws SdxL2Exception;

    /**
     * Removes a named SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     * @throws SdxL2Exception if SDX-L2 is not removed
     */
    void removeSdxL2(String sdxl2) throws SdxL2Exception;

    /**
     * Returns a set of SDX-L2 names.
     *
     * @return set of SDX-L2 names
     */
    Set<String> getSdxL2s();

    /**
     * Adds a Connection Point to a SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     * @param connectionPoint SDX-L2 cCnnection Point object
     * @throws SdxL2Exception if SDX-L2 is not added
     */
    void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint connectionPoint) throws SdxL2Exception;

    /**
     * Returns all the SDX-L2 connection points names or the SDX-L2 connection points name
     * that are related to a SDX-L2.
     *
     * @param sdxl2 name of the SDX-L2 (optional)
     * @return a set of SDX-L2 connection points names, the result depends on the input parameter;
     * @throws SdxL2Exception if SDX-L2 cannot be found
     */
    Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2) throws SdxL2Exception;

    /**
     * Removes a named Connection Point in a SDX-L2.
     *
     * @param sdxl2cp name of Connection Point
     * @throws SdxL2Exception if SDX-L2 Connection Point does not exist
     */
    void removeSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception;

    /**
     * Returns a Connection Point in a SDX-L2.
     *
     * @param sdxl2cp name of Connection Point
     * @return relative SDX-L2 Connection Point
     * @throws SdxL2Exception if SDX-L2 CP cannot be found
     */
    SdxL2ConnectionPoint getSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception;

    /**
     * Creates a Virtual Circuit between two SDX-L2 Connection Points.
     *
     * @param sdxl2 name of SDX-L2
     * @param sdxl2cplhs name of SDX-L2 CP, left hand side of the VC
     * @param sdxl2cprhs name of SDX-L2 CP, right hand side of the VC
     * @throws SdxL2Exception if SDX-L2 VC cannot be added
     */
    void addVC(String sdxl2, SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs)
            throws SdxL2Exception;

    /**
     * Deletes a Virtual Circuit between Connection Points in a SDX-L2.
     *
     * @param sdxl2cplhs name of SDX-L2 CP, left hand side of the VC
     * @param sdxl2cprhs ame of SDX-L2 CP, right hand side of the VC
     * @throws SdxL2Exception if no name is provided for VC
     */
    void removeVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs)
            throws SdxL2Exception;

    /**
     * Deletes a Virtual Circuit where a given SDX-L2 CP acts as endpoint.
     *
     * @param cp Connection Point
     * @throws SdxL2Exception if appropriate VC identifier is not provided
     */
    void removeVC(SdxL2ConnectionPoint cp) throws SdxL2Exception;

    /**
     * Removes all Virtual Circuits created in a given SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     */
    void removeVCs(String sdxl2);

    /**
     * Returns an encoded Virtual Circuit in a SDX-L2.
     *
     * @param sdxl2cplhs name of SDX-L2 CP, left hand side of the VC
     * @param sdxl2cprhs ame of SDX-L2 CP, right hand side of the VC
     * @return the encoded Virtual Circuit
     * @throws SdxL2Exception if no matching VC exists
     */
    String getVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs)
            throws SdxL2Exception;

    /**
     * Returns a) all Virtual Circuits, or b) Virtual Circuits in a given SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     * @return set of names for Virtual Circuits
     */
    Set<String> getVCs(Optional<String> sdxl2);
}
