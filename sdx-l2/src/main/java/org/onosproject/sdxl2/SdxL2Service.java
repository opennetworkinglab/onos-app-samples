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
     * @param sdxl2 name of SDX-L2
     */
    void deleteSdxL2(String sdxl2);

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
     * @param sdxl2cp SDX-L2 Connection Point object
     */
    void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint sdxl2cp);

    /**
     * Returns all names of Connection Points names in a SDX-L2 or
     * all the names of SDX-L2 Connection Points.
     *
     * @param sdxl2 name of SDX-L2
     * @return a set of SDX-L2 connection points names
     */
    Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2);

    /**
     * Removes a Connection Point from a SDX-L2.
     *
     * @param sdxl2cp name of SDX-L2 Connection Point
     */
    void removeSdxL2ConnectionPoint(String sdxl2cp);

    /**
     * Creates an L2 Virtual Circuit between two SDX-L2 Connection Points.
     *
     * @param sdxl2 name of SDX-L2
     * @param sdxl2cplhs name of SDX-L2 CP, left hand side of the VC
     * @param sdxl2cprhs name of SDX-L2 CP, right hand side of the VC
     */
    void addVC(String sdxl2, String sdxl2cplhs, String sdxl2cprhs);

    /**
     * Deletes a Virtual Circuit between Connection Points in a SDX-L2.
     *
     * @param vc name of SDX-L2 VC
     */
    void removeVC(String vc);

    /**
     * Returns a Connection Point in a SDX-L2.
     *
     * @param sdxl2cp name of SDX-L2 Connection Point
     * @return the relative SdxL2ConnectionPoint object
     */
    SdxL2ConnectionPoint getSdxL2ConnectionPoint(String sdxl2cp);

    /**
     * Returns all the Virtual Circuits in a SDX-L2.
     *
     * @param sdxl2 name of SDX-L2
     * @return set of Virtual Circuits names
     */
    Set<String> getVirtualCircuits(Optional<String> sdxl2);

    /**
     * Returns a Virtual Circuit in a SDX-L2.
     *
     * @param sdxl2vc name of the SDX-L2 VC
     * @return the relative VirtualCircuit object
     */
    VirtualCircuit getVirtualCircuit(String sdxl2vc);

    /**
     * Returns the state of the Intent that has been provided as input.
     *
     * @param intentKey key of the intent
     * @return the last state of the intent
     */
    SdxL2State getIntentState(Key intentKey);

    /**
     * Returns the state of the EdgePort that has been provided as input.
     *
     * @param edgeport the Connection Point representing the edge port
     * @return the last state of the edge port
     */
    SdxL2State getEdgePortState(ConnectPoint edgeport);

    /**
     * Cleans the state of the Application.
     */
    void cleanSdxL2();
}
