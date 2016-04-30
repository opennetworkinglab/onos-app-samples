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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.primitives.DefaultDistributedSet;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * SDXL2 Store implementation backed by different distributed primitives.
 */
@Component(immediate = true)
@Service
public class SdxL2DistributedStore implements SdxL2Store {

    private static Logger log = LoggerFactory.getLogger(SdxL2DistributedStore.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    private DistributedSet<String> sdxL2s;

    private static String errorAddSdx = "It is not possible to add ";
    private static String errorRemoveSdx = "It is not possible to remove ";

    @Activate
    public void activate() {

        KryoNamespace custom = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                .register(new SdxL2ConnectionPointSerializer(), new Class[]{SdxL2ConnectionPoint.class})
                .build();

        sdxL2s = new DefaultDistributedSet<>(this.storageService
                .<String>setBuilder()
                .withSerializer(Serializer.using(custom))
                .withName("sdxl2s")
                .build(), DistributedPrimitive.DEFAULT_OPERTATION_TIMEOUT_MILLIS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

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
