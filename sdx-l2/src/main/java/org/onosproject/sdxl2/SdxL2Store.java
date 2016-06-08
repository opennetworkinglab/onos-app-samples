/*
 * Copyright 2016-present Open Networking Laboratory
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
     * @param sdxl2 SDX-L2 name
     * @throws SdxL2Exception if SDX-L2 exists
     */
    void putSdxL2(String sdxl2) throws SdxL2Exception;

    /**
     * Removes a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     * @throws SdxL2Exception if SDX-L2 does not exist
     */
    void removeSdxL2(String sdxl2) throws SdxL2Exception;

    /**
     * Returns a set of SDX-L2 names.
     *
     * @return a set of SDX-L2 names
     */
    Set<String> getSdxL2s();

    /**
     * Adds an SDX-L2 connection point to an SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     * @param connectionPoint the SDX-L2 connection point object
     * @throws SdxL2Exception if it is not possible to add the SDX-L2 connection point
     */
    void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint connectionPoint) throws SdxL2Exception;

    /**
     * Returns all the SDX-L2 connection points names or the SDX-L2 connection points name
     * that are related to an SDX-L2.
     *
     * @param sdxl2 name (optional) of the SDX-L2
     * @return a set of SDX-L2 connection points names, the result depends on the input parameter;
     * @throws SdxL2Exception if SDX-L2 is present but it does not exist
     */
    Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2) throws SdxL2Exception;


    /**
     * Removes a named SDX-L2 connection point in an SDX-L2.
     *
     * @param sdxl2cp the connection point name
     * @throws SdxL2Exception if the connection point does not exist
     */
    void removeSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception;

    /**
     * Returns an SDX-L2 connection point in a SDX-L2.
     *
     * @param sdxl2cp the connection point name
     * @return the relative SDXL2ConnectionPoint object
     * @throws SdxL2Exception if SDX-L2 connection point does not exist
     */
    SdxL2ConnectionPoint getSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception;

}
