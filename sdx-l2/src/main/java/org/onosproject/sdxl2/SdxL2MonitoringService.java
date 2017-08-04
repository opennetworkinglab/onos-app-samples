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

/**
 * Service that provides the current state of
 * the SDX-L2 related intents and of the
 * client interfaces.
 */
public interface SdxL2MonitoringService {

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
     * @return the last state of the edge port;
     */
    SdxL2State getEdgePortState(ConnectPoint edgeport);

    /**
     * Remove listeners.
     */
    void cleanup();

}
