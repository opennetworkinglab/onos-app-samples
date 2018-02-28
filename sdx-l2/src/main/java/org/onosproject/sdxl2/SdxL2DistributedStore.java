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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

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

    private Map<String, String> sdxL2VCs;
    private ConsistentMap<String, String> sdxL2vcs;

    /**
     * Error definitions for SDX-L2 instances,CPs and VCs.
     */
    private static final String ERROR_SDX_ADD = "It is not possible to add %s " +
            "because it already exists";
    private static final String ERROR_SDX_REMOVE = "It is not possible to remove %s " +
            "because it does not exist";

    private static final String ERROR_SDX_ADD_CP_NAME = "It is not possible to add %s " +
            "because there is a sdxl2cp with the same name";
    private static final String ERROR_SDX_ADD_CP_VLANS = "It is not possible to add %s " +
            "because there is a conflict with %s on the vlan ids";
    private static final String ERROR_SDX_ADD_CP_EXISTING = "It is not possible to add %s " +
            "because there is a conflict with %s on the connection point";
    private static final String ERROR_SDX_ADD_CP_MISSING = "It is not possible to add %s " +
            "because the relative sdxl2 %s does not exist";
    private static final String ERROR_SDX_GET_CP_MISSING = "It is not possible to retrieve %s " +
            "because it does not exist";
    private static final String ERROR_SDX_GET_CPS_MISSING = "It is not possible to list the sdxl2cps " +
            "because sdxl2 %s does not exist";
    private static final String ERROR_SDX_REMOVE_CP_MISSING = "It is not possible to remove %s " +
            "because it does not exist";

    private static final String ERROR_VC_KEY = "It is not possible to %s vc because " +
            "there is a problem with key %s (wrong format)";
    private static final String ERROR_VC_ADD_OVERLAP = "It is not possible to add vc " +
            "because there is an overlap with %s";
    private static final String ERROR_VC_REMOVE_MISSING = "It is not possible to remove the " +
            "vc because it does not exist";
    private static final String ERROR_VC_MISSING = "Virtual Circuit between %s and %s " +
            "does not exist";

    /**
     * Activates the implementation of the SDX-L2 store.
     */
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

        sdxL2vcs = this.storageService.<String, String>consistentMapBuilder()
                .withSerializer(Serializer.using(custom))
                .withName("vcs")
                .build();
        sdxL2VCs = sdxL2vcs.asJavaMap();

        log.info("Started");
    }

    /**
     * Helper class called to initialise tests.
     */
    public void initForTest() {
        this.sdxL2s = Sets.newHashSet();
        this.sdxL2CPs = new ConcurrentHashMap<>();
        this.sdxL2VCs = new ConcurrentHashMap<>();
    }

    /**
     * Deactivates the implementation of the SDX-L2 store.
     */
    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void putSdxL2(String sdxl2) throws SdxL2Exception {
        boolean inserted = sdxL2s.add(sdxl2);
        if (!inserted) {
            throw new SdxL2Exception(String.format(ERROR_SDX_ADD, sdxl2));
        }
    }

    @Override
    public void removeSdxL2(String sdxl2) throws SdxL2Exception {
        boolean removed = sdxL2s.remove(sdxl2);
        if (!removed) {
            throw new SdxL2Exception(String.format(ERROR_SDX_REMOVE, sdxl2));
        }

        // Removes CPs
        Set<Map.Entry<SdxL2ConnectionPoint, String>> cpsToRemove = sdxL2CPs.entrySet().parallelStream().filter(
                key_value -> {
                    String sdxl2Temp = key_value.getValue();
                    return sdxl2Temp.equals(sdxl2);
                }).collect(Collectors.toSet());
        cpsToRemove.forEach(key_value -> sdxL2CPs.remove(key_value.getKey()));
    }

    @Override
    public Set<String> getSdxL2s() {
        return ImmutableSet.copyOf(sdxL2s);
    }

    @Override
    public void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint connectionPoint) throws SdxL2Exception {
        boolean exist = sdxL2s.contains(sdxl2);
        String errorMissingSdxL2 = String.format(ERROR_SDX_ADD_CP_MISSING, connectionPoint.name(), sdxl2);
        if (!exist) {
            throw new SdxL2Exception(errorMissingSdxL2);
        }

        Set<SdxL2ConnectionPoint> sdxl2cpsTemp = ImmutableSet.copyOf(sdxL2CPs.keySet());
        Set<SdxL2ConnectionPoint> sdxl2cpsTempByName = sdxl2cpsTemp.parallelStream().filter(
                sdxl2cpTemp -> sdxl2cpTemp.name().equals(connectionPoint.name()
                )
        ).collect(Collectors.toSet());

        if (!sdxl2cpsTempByName.isEmpty()) {
            throw new SdxL2Exception(String.format(ERROR_SDX_ADD_CP_NAME, connectionPoint.name()));
        }

        Set<SdxL2ConnectionPoint> sdxl2cpsByCP = sdxl2cpsTemp.parallelStream().filter(
                sdxl2cpTemp -> sdxl2cpTemp.connectPoint().equals(connectionPoint.connectPoint()
                )
        ).collect(Collectors.toSet());

        String tempName;
        List<VlanId> vlans = connectionPoint.vlanIds();
        for (VlanId vlanId : vlans) {
            Set<SdxL2ConnectionPoint> sdxl2cpsByVlan = sdxl2cpsByCP.parallelStream().filter(
                    sdxl2cp_by_vlan -> (
                            sdxl2cp_by_vlan.vlanIds().contains(vlanId) || sdxl2cp_by_vlan.vlanIds().size() == 0
                    )).collect(Collectors.toSet());

            tempName = sdxl2cpsByVlan.iterator().hasNext() ? sdxl2cpsByVlan.iterator().next().name() : null;

            if (!sdxl2cpsByVlan.isEmpty()) {
                throw new SdxL2Exception(String.format(ERROR_SDX_ADD_CP_VLANS, connectionPoint.name(), tempName));
            }

        }

        tempName = sdxl2cpsByCP.iterator().hasNext() ? sdxl2cpsByCP.iterator().next().name() : null;
        if (!sdxl2cpsByCP.isEmpty() && vlans.size() == 0) {
            throw new SdxL2Exception(String.format(ERROR_SDX_ADD_CP_EXISTING, connectionPoint.name(), tempName));
        }

        sdxL2CPs.put(connectionPoint, sdxl2);
    }

    @Override
    public Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2) throws SdxL2Exception {
        if (sdxl2.isPresent()) {
            Set<Map.Entry<SdxL2ConnectionPoint, String>> toGet = sdxL2CPs.entrySet().parallelStream().filter(
                    key_value -> {
                        String sdxl2Temp = key_value.getValue();
                        return sdxl2Temp.equals(sdxl2.get());
                    }).collect(Collectors.toSet());

            Iterator<String> itsdxL2s = sdxL2s.iterator();
            boolean found = false;
            while (itsdxL2s.hasNext()) {
                if (sdxl2.get().equals(itsdxL2s.next())) {
                    found = true;
                }
            }

            if (!found) {
                throw new SdxL2Exception(String.format(ERROR_SDX_GET_CPS_MISSING, sdxl2.get()));
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

    @Override
    public void removeSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception {
        Set<SdxL2ConnectionPoint> sdxl2cpsTemp = ImmutableSet.copyOf(sdxL2CPs.keySet());
        Set<SdxL2ConnectionPoint> sdxl2cpsTempByName = sdxl2cpsTemp.parallelStream().filter(
                sdxl2cpTemp -> sdxl2cpTemp.name().equals(sdxl2cp)
        ).collect(Collectors.toSet());

        if (sdxl2cpsTempByName.size() == 0) {
            throw new SdxL2Exception(String.format(ERROR_SDX_REMOVE_CP_MISSING, sdxl2cp));
        }

        sdxl2cpsTempByName.forEach(sdxl2cpTemp -> sdxL2CPs.remove(sdxl2cpTemp));
    }

    @Override
    public SdxL2ConnectionPoint getSdxL2ConnectionPoint(String sdxl2cp) throws SdxL2Exception {
        SdxL2ConnectionPoint sdxl2cpTemp = ImmutableSet.copyOf(sdxL2CPs.keySet()).parallelStream()
                .filter(sdxl2cp_temp -> sdxl2cp_temp.name().equals(sdxl2cp)).findFirst().orElse(null);

        if (sdxl2cpTemp == null) {
            throw new SdxL2Exception(String.format(ERROR_SDX_GET_CP_MISSING, sdxl2cp));
        }

        return sdxl2cpTemp;
    }

    @Override
    public void addVC(String sdxl2, SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs)
            throws SdxL2Exception {
        Set<String> vcs = ImmutableSet.copyOf(
                sdxL2VCs.keySet().parallelStream().filter((vctemp -> vctemp.contains(sdxl2cplhs.toString())
                        || vctemp.contains(sdxl2cprhs.toString()))).collect(Collectors.toSet()));
        for (String vctemp : vcs) {
            String[] splitted = vctemp.split("~");

            if (splitted.length != 2) {
                throw new SdxL2Exception(String.format(ERROR_VC_KEY, "add", vctemp));
            }

            if (!(!sdxl2cplhs.toString().equals(splitted[0]) &&
                    !sdxl2cplhs.toString().equals(splitted[1]) &&
                    !sdxl2cprhs.toString().equals(splitted[0]) &&
                    !sdxl2cprhs.toString().equals(splitted[1]))) {
                throw new SdxL2Exception(String.format(ERROR_VC_ADD_OVERLAP, vctemp));
            }
        }

        String cps = sdxl2cplhs.toString().compareTo(sdxl2cprhs.toString()) < 0 ?
                format(SdxL2VCManager.SDXL2_CPS_FORMAT, sdxl2cplhs, sdxl2cprhs) :
                format(SdxL2VCManager.SDXL2_CPS_FORMAT, sdxl2cprhs, sdxl2cplhs);
        String name = sdxl2cplhs.name().compareTo(sdxl2cprhs.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, sdxl2, sdxl2cplhs.name(), sdxl2cprhs.name()) :
                format(SdxL2VCManager.NAME_FORMAT, sdxl2, sdxl2cprhs.name(), sdxl2cplhs.name());
        sdxL2VCs.put(cps, name);
    }

    @Override
    public void removeVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs)
            throws SdxL2Exception {
        String cps = sdxl2cplhs.toString().compareTo(sdxl2cprhs.toString()) < 0 ?
                format(SdxL2VCManager.SDXL2_CPS_FORMAT, sdxl2cplhs, sdxl2cprhs) :
                format(SdxL2VCManager.SDXL2_CPS_FORMAT, sdxl2cprhs, sdxl2cplhs);
        String name = sdxL2VCs.remove(cps);
        if (name == null) {
            throw new SdxL2Exception(ERROR_VC_REMOVE_MISSING);
        }
    }

    @Override
    public void removeVC(SdxL2ConnectionPoint cp) throws SdxL2Exception {
        Set<String> vcs = ImmutableSet.copyOf(sdxL2VCs.keySet()
                                                      .parallelStream()
                                                      .filter((vctemp -> vctemp.contains(cp.toString())))
                                                      .collect(Collectors.toSet()));
        for (String vctemp : vcs) {
            String[] splitted = vctemp.split("~");
            if (splitted.length != 2) {
                throw new SdxL2Exception(String.format(ERROR_VC_KEY, "delete", vctemp));
            }
            if (cp.toString().equals(splitted[0]) || cp.toString().equals(splitted[1])) {
                sdxL2VCs.remove(vctemp);
            }
        }
    }

    @Override
    public void removeVCs(String sdxl2) {
        Set<Map.Entry<String, String>> vcsToRemove = sdxL2VCs.entrySet().parallelStream().filter(key_value -> {
            String[] fields = key_value.getValue().split(":");
            return fields.length == 2 && fields[0].equals(sdxl2);
        }).collect(Collectors.toSet());

        vcsToRemove.forEach(key_value -> sdxL2VCs.remove(key_value.getKey()));
    }

    @Override
    public String getVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs)
            throws SdxL2Exception {
        String cps = sdxl2cplhs.toString().compareTo(sdxl2cprhs.toString()) < 0 ?
                format(SdxL2VCManager.SDXL2_CPS_FORMAT, sdxl2cplhs, sdxl2cprhs) :
                format(SdxL2VCManager.SDXL2_CPS_FORMAT, sdxl2cprhs, sdxl2cplhs);

        String encodedvc = ImmutableSet.copyOf(sdxL2VCs.keySet()).parallelStream().filter(
                encoded_cps -> encoded_cps.equals(cps)).findFirst().orElse(null);

        if (encodedvc == null) {
            throw new SdxL2Exception(String.format(ERROR_VC_MISSING,
                                                   sdxl2cplhs.name(), sdxl2cprhs.name()));
        }
        return encodedvc;
    }

    @Override
    public Set<String> getVCs(Optional<String> sdxl2) {
        if (sdxl2.isPresent()) {
            return ImmutableSet.copyOf(sdxL2VCs.values())
                    .parallelStream()
                    .filter(vc -> {
                        String[] parts = vc.split(":");
                        return parts.length == 2 && parts[0].equals(sdxl2.get());
                    }).collect(Collectors.toSet());
        }
        return ImmutableSet.copyOf(sdxL2VCs.values());
    }
}
