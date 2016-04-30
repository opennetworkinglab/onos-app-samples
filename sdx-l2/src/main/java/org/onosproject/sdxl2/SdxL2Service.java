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

import java.util.Set;

/**
 * Service that allows to create virtual named SDXL2s
 * In which it is possible to provide connectivity
 * (layer 2 Virtual Circuits - VC) between edge ports.
 */
public interface SdxL2Service {

    /**
     * Create a named SDXL2.
     *
     * @param sdxl2 SDXL2 name
     */
    void createSdxL2(String sdxl2);

    /**
     * Delete a named SDXL2.
     *
     * @param sdxl2 SDXL2 name
     */
    void deleteSdxL2(String sdxl2);

    /**
     * Returns a set of SDXL2 names.
     *
     * @return a set of SDXL2 names
     */
    Set<String> getSdxL2s();

}
