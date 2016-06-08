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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.Key;

import java.util.Optional;
import java.util.Set;

/**
 * Service that allows to create virtual named sdx-l2s
 * in which is possible to provide connectivity
 * (layer 2 Virtual Circuits - VC) between edge ports.
 */
public interface SdxL2Service {

    /**
     * Creates a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     */
    void createSdxL2(String sdxl2);

    /**
     * Deletes a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     */
    void deleteSdxL2(String sdxl2);

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
     * @param sdxl2cp SDX-L2 connection point object
     */
    void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint sdxl2cp);

    /**
     * Returns all the SDX-L2 connection points names in a SDX-L2 or all the SDX-L2 connection points names.
     *
     * @param sdxl2 SDX-L2 name
     * @return a set of SDX-L2 connection points names
     */
    Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2);

    /**
     * Removes an SDX-L2 connection point from an SDX-L2.
     *
     * @param sdxl2cp SDX-L2 connection point name
     */
    void removeSdxL2ConnectionPoint(String sdxl2cp);

    /**
     * Returns an SDX-L2 connection point in a SDX-L2.
     *
     * @param sdxl2cp SDX-L2 connection point name
     * @return the relative SdxL2ConnectionPoint object
     */
    SdxL2ConnectionPoint getSdxL2ConnectionPoint(String sdxl2cp);

    /**
     * Returns the state of the Intent that has been provided as input.
     *
     * @param intentKey key of the intent;
     * @return the last state of the intent;
     */
    SdxL2State getIntentState(Key intentKey);

    /**
     * Returns the state of the EdgePort that has been provided as input.
     *
     * @param edgeport the connect point representing the edge port
     * @return the last state of the edgeport;
     */
    SdxL2State getEdgePortState(ConnectPoint edgeport);

    /**
     * Cleans the state of the Application.
     */
    void cleanSdxL2();

}
