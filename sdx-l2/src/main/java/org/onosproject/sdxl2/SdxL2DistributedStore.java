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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.primitives.DefaultDistributedSet;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SDX-L2 Store implementation backed by different distributed primitives.
 */
@Component(immediate = true)
@Service
public class SdxL2DistributedStore implements SdxL2Store {

    private static Logger log = LoggerFactory.getLogger(SdxL2DistributedStore.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    private Set<String> sdxL2s;

    private Map<SdxL2ConnectionPoint, String> sdxL2CPs;
    private ConsistentMap<SdxL2ConnectionPoint, String> sdxL2cps;

    private static String errorAddSdx = "It is not possible to add %s " +
            "because it exists";

    private static String errorRemoveSdx = "It is not possible to remove %s " +
            "because it does not exist";

    private static String errorAddSdxL2CPName = "It is not possible to add %s " +
            "because there is a sdxl2cp with the same name";
    private static String errorAddSdxL2CPVlans = "It is not possible to add %s " +
            "because there is a conflict with %s on the vlan ids";
    private static String errorAddSdxL2CPCP = "It is not possible to add %s " +
            "because there is a conflict with %s on the connection point";
    private static String errorAddSdxL2CPSdx = "It is not possible to add %s " +
            "because the relative sdxl2 does not exist";

    private static String errorGetSdxL2CPs = "It is not possible to list the sdxl2cps" +
            "because sdxl2 %s does not exist";

    private static String errorRemoveSdxL2CP = "It is not possible to remove %s " +
            "because it does not exist";

    public void initForTest() {

        this.sdxL2s = Sets.newHashSet();

        this.sdxL2CPs = new ConcurrentHashMap<SdxL2ConnectionPoint, String>();

    }

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

        sdxL2cps = this.storageService
                .<SdxL2ConnectionPoint, String>consistentMapBuilder()
                .withSerializer(Serializer.using(custom))
                .withName("sdxl2cps")
                .build();
        sdxL2CPs = sdxL2cps.asJavaMap();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * Creates a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     * @throws SdxL2Exception if SDX-L2 exists
     */
    @Override
    public void putSdxL2(String sdxl2) throws SdxL2Exception {
        boolean inserted = sdxL2s.add(sdxl2);
        if (!inserted) {
            throw new SdxL2Exception(String.format(errorAddSdx, sdxl2));
        }
    }

    /**
     * Removes a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     * @throws SdxL2Exception if SDX-L2 does not exist
     */
    @Override
    public void removeSdxL2(String sdxl2) throws SdxL2Exception {
        boolean removed = sdxL2s.remove(sdxl2);
        if (!removed) {
            throw new SdxL2Exception(String.format(errorRemoveSdx, sdxl2));
        }

        Set<Map.Entry<SdxL2ConnectionPoint, String>> toRemove = sdxL2CPs.entrySet().parallelStream().filter(
                key_value -> {
                    String sdxl2Temp = key_value.getValue();
                    return sdxl2Temp.equals(sdxl2) ? true : false;
                }).collect(Collectors.toSet());
        toRemove.forEach(key_value -> sdxL2CPs.remove(key_value.getKey()));
    }

    /**
     * Returns a set of SDX-L2 names.
     *
     * @return a set of SDX-L2 names
     */
    @Override
    public Set<String> getSdxL2s() {
        return ImmutableSet.copyOf(sdxL2s);
    }

    /**
     * Adds an SDX-L2 connection point to an SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     * @param connectionPoint the connection point object
     * @throws SdxL2Exception if it is not possible to add the SDX-L2 connection point
     */
    @Override
    public void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint connectionPoint) throws SdxL2Exception {

        boolean exist = sdxL2s.contains(sdxl2);

        if (!exist) {
            throw new SdxL2Exception(String.format(errorAddSdxL2CPSdx, connectionPoint.name()));
        }

        Set<SdxL2ConnectionPoint> sdxl2cpsTemp = ImmutableSet.copyOf(sdxL2CPs.keySet());
        Set<SdxL2ConnectionPoint> sdxl2cpsTempByName = sdxl2cpsTemp.parallelStream().filter(
                sdxl2cpTemp-> sdxl2cpTemp.name().equals(connectionPoint.name()
                )
        ).collect(Collectors.toSet());

        if (sdxl2cpsTempByName.size() != 0) {
            throw new SdxL2Exception(String.format(errorAddSdxL2CPName, connectionPoint.name()));
        }


        Set<SdxL2ConnectionPoint> sdxl2cpsByCP = sdxl2cpsTemp.parallelStream().filter(
                sdxl2cp_temp-> sdxl2cp_temp.connectPoint().equals(connectionPoint.connectPoint()
                )
        ).collect(Collectors.toSet());

        String tempName;
        List<VlanId> vlans = connectionPoint.vlanIds();
        for (VlanId vlanId : vlans) {
            Set<SdxL2ConnectionPoint> sdxl2cpsByVlan = sdxl2cpsByCP.parallelStream().filter(
                    sdxl2cp_by_vlan -> (
                            sdxl2cp_by_vlan.vlanIds().contains(vlanId) || sdxl2cp_by_vlan.vlanIds().size() == 0
                    )).collect(Collectors.toSet());

            tempName =  sdxl2cpsByVlan.iterator().hasNext() ?  sdxl2cpsByVlan.iterator().next().name() : null;

            if (sdxl2cpsByVlan.size() != 0) {
                throw new SdxL2Exception(String.format(errorAddSdxL2CPVlans, connectionPoint.name(), tempName));
            }

        }

        tempName =  sdxl2cpsByCP.iterator().hasNext() ? sdxl2cpsByCP.iterator().next().name() : null;

        if (sdxl2cpsByCP.size() != 0 && vlans.size() == 0) {
            throw new SdxL2Exception(String.format(errorAddSdxL2CPCP, connectionPoint.name(), tempName));
        }

        sdxL2CPs.put(connectionPoint, sdxl2);

    }

    /**
     * Returns all the SDX-L2 connection points names or the SDX-L2 2connection points names
     * that are related to an SDX-L2.
     *
     * @param sdxl2 name (optional) of the SDX-L2
     * @return a set of SDX-L2 connection points names, the result depends on the input parameter;
     * @throws SdxL2Exception if SDX-L2 is present but it does not exist
     */
    @Override
    public Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2) throws SdxL2Exception {

        if (sdxl2.isPresent()) {

            Set<Map.Entry<SdxL2ConnectionPoint, String>> toGet = sdxL2CPs.entrySet().parallelStream().filter(
                    key_value -> {
                        String sdxl2Temp = key_value.getValue();
                        return sdxl2Temp.equals(sdxl2.get()) ? true : false;
                    }).collect(Collectors.toSet());

            Iterator<String> itsdxL2s = sdxL2s.iterator();
            boolean found = false;
            while (itsdxL2s.hasNext()) {
                if (sdxl2.get().equals(itsdxL2s.next())) {
                    found = true;
                }
            }

            if (!found) {
                throw new SdxL2Exception(String.format(errorGetSdxL2CPs, sdxl2.get()));
            }

            Set<String> cpsTemp = Sets.newHashSet();
            for (Map.Entry<SdxL2ConnectionPoint, String> cp : toGet) {
                cpsTemp.add(cp.getKey().name());
            }

            return cpsTemp;
        }

        return ImmutableSet.copyOf(sdxL2CPs.keySet()).parallelStream().map(
                SdxL2ConnectionPoint::name).collect(Collectors.toSet());

    }

    /**
     * Removes a named SDX-L2 connection point in an SDX-L2.
     *
     * @param sdxl2cp the connection point name
     * @throws SdxL2Exception if SDX-L2 connection point does not exist
     */
    @Override
    public void removeSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception {

        Set<SdxL2ConnectionPoint> sdxl2cpsTemp = ImmutableSet.copyOf(sdxL2CPs.keySet());
        Set<SdxL2ConnectionPoint> sdxl2cpsTempByName = sdxl2cpsTemp.parallelStream().filter(
                sdxl2cpTemp -> sdxl2cpTemp.name().equals(sdxl2cp
                )
        ).collect(Collectors.toSet());

        if (sdxl2cpsTempByName.size() == 0) {
            throw new SdxL2Exception(String.format(errorRemoveSdxL2CP, sdxl2cp));
        }

        for (SdxL2ConnectionPoint sdxl2cpTemp : sdxl2cpsTempByName) {
            sdxL2CPs.remove(sdxl2cpTemp);
        }

    }

}
