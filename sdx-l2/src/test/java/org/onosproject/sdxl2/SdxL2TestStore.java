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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Stub class that emulates the behaviours of the SdxL2Store.
 */
public final class SdxL2TestStore implements SdxL2Store {

    private Set<String> sdxL2s = Sets.newHashSet();

    private static String errorAddSdx = "It is not possible to add ";
    private static String errorRemoveSdx = "It is not possible to remove ";

    /**
     * Create a named sdx-l2.
     *
     * @param sdxl2 sdx-l2 name
     * @throws SdxL2Exception if sdxl2 exists
     */
    @Override
    public void putSdxL2(String sdxl2) throws SdxL2Exception {
        boolean inserted = sdxL2s.add(sdxl2);
        if (!inserted) {
            throw new SdxL2Exception(errorAddSdx + sdxl2);
        }
    }

    /**
     * Remove a named sdx-l2.
     *
     * @param sdxl2 sdx-l2 name
     * @throws SdxL2Exception if sdxl2 does not exist
     */
    @Override
    public void removeSdxL2(String sdxl2) throws SdxL2Exception {
        boolean removed = sdxL2s.remove(sdxl2);
        if (!removed) {
            throw new SdxL2Exception(errorRemoveSdx + sdxl2);
        }
    }

    /**
     * Returns a set of sdxl2 names.
     *
     * @return a set of sdxl2 names
     */
    @Override
    public Set<String> getSdxL2s() {
        return ImmutableSet.copyOf(sdxL2s);
    }

}
