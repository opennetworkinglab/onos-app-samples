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
 * Storage service of sdxl2 application.
 */
public interface SdxL2Store {

    /**
     * Create a named sdx-l2.
     *
     * @param sdxl2 sdx-l2 name
     * @throws SdxL2Exception if sdxl2 exists
     */
    void putSdxL2(String sdxl2) throws SdxL2Exception;

    /**
     * Remove a named sdx-l2.
     *
     * @param sdxl2 sdx-l2 name
     * @throws SdxL2Exception if sdxl2 does not exist
     */
    void removeSdxL2(String sdxl2) throws SdxL2Exception;

    /**
     * Returns a set of sdxl2 names.
     *
     * @return a set of sdxl2 names
     */
    Set<String> getSdxL2s();

}
