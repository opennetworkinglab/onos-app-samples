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

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Manages Virtual Circuits using MAC addresses.
 */
public class SdxL2MacVCManager extends SdxL2VCManager {

    private static final int PRIORITY_OFFSET = 2000;
    private static Logger log = LoggerFactory.getLogger(SdxL2MacVCManager.class);

    private static String errorMacNull = "VC cannot be %s: the mac address of %s is null";
    private static String errorMacEqual = "VC cannot be %s: same mac addresses on both sides";

    /**
     * Creates a SDX-L2 MAC-based VC Manager.
     *
     * @param sdxl2id application ID
     * @param store reference to the SDX-L2 store
     * @param intentService reference to the Intent service
     */
    public SdxL2MacVCManager(ApplicationId sdxl2id,
                             SdxL2Store store,
                             IntentService intentService) {
        super(sdxl2id, store, intentService);
        log.info("Started");
    }

    @Override
    public Collection<Intent> buildIntents(String sdxl2, SdxL2ConnectionPoint ingress, SdxL2ConnectionPoint egress) {
        List<Intent> intents = null;
        TrafficSelector selector;
        TrafficTreatment treatment;
        Key key;

        if (ingress.vlanIds().size() == egress.vlanIds().size()) {
            intents = new ArrayList<>();
            if (ingress.vlanIds().size() == 0) {
                selector = buildSelector(ingress.macAddress(),
                                         egress.macAddress(),
                                         null,
                                         null);
                treatment = DefaultTrafficTreatment.emptyTreatment();
                key = generateIntentKey(sdxl2, ingress, egress, "1");

                intents.add(PointToPointIntent.builder()
                                    .appId(appId)
                                    .key(key)
                                    .selector(selector)
                                    .treatment(treatment)

                                    .ingressPoint(ingress.connectPoint())
                                    .egressPoint(egress.connectPoint())
                                    .priority(PRIORITY_OFFSET)
                                    .build());
            } else {
                Iterator<VlanId> ingressTags = ingress.vlanIds().iterator();
                Iterator<VlanId> egressTags = egress.vlanIds().iterator();
                int index = 1;
                while (ingressTags.hasNext()) {
                    selector = buildSelector(ingress.macAddress(),
                                             egress.macAddress(),
                                             null,
                                             ingressTags.next());
                    treatment = buildTreatment(egressTags.next(),
                                               null,
                                               false);
                    key = generateIntentKey(sdxl2, ingress, egress, String.valueOf(index));

                    intents.add(PointToPointIntent.builder()
                                        .appId(appId)
                                        .key(key)
                                        .selector(selector)
                                        .treatment(treatment)
                                        .ingressPoint(ingress.connectPoint())
                                        .egressPoint(egress.connectPoint())
                                        .priority(PRIORITY_OFFSET)
                                        .build());
                    index += 1;
                }
            }
            return intents;
        }

        if (ingress.vlanIds().size() == 1 && egress.vlanIds().size() == 0) {
            Iterator<VlanId> ingressTags = ingress.vlanIds().iterator();
            intents = new ArrayList<>();
            selector = buildSelector(ingress.macAddress(),
                                     egress.macAddress(),
                                     null,
                                     ingressTags.next());
            treatment = buildTreatment(null,
                                       null,
                                       true);
            key = generateIntentKey(sdxl2, ingress, egress, "1");

            intents.add(PointToPointIntent.builder()
                                .appId(appId)
                                .key(key)
                                .selector(selector)
                                .treatment(treatment)
                                .ingressPoint(ingress.connectPoint())
                                .egressPoint(egress.connectPoint())
                                .priority(PRIORITY_OFFSET)
                                .build());
            return intents;
        }

        if (ingress.vlanIds().size() == 0 && egress.vlanIds().size() == 1) {
            Iterator<VlanId> egressTags = egress.vlanIds().iterator();
            intents = new ArrayList<>();
            selector = buildSelector(ingress.macAddress(),
                                     egress.macAddress(),
                                     null,
                                     null);
            treatment = buildTreatment(null,
                                       egressTags.next(),
                                       false);
            key = generateIntentKey(sdxl2, ingress, egress, "1");

            intents.add(PointToPointIntent.builder()
                                .appId(appId)
                                .key(key)
                                .selector(selector)
                                .treatment(treatment)
                                .ingressPoint(ingress.connectPoint())
                                .egressPoint(egress.connectPoint())
                                .priority(PRIORITY_OFFSET)
                                .build());
            return intents;
        }
        log.warn(String.format(ERROR_CREATE_INTENTS, ingress.name(), egress.name()));
        return intents;
    }

    @Override
    public void addVC(String sdxl2, SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs) {
        String action = "created";
        checkState(!(isNullMac(sdxl2cplhs.macAddress()) && isNullMac(sdxl2cprhs.macAddress())),
                   errorMacNull, action, sdxl2cplhs.name() + " and " + sdxl2cprhs.name());
        checkState(!isNullMac(sdxl2cprhs.macAddress()), errorMacNull, action, sdxl2cplhs.name());
        checkState(!isNullMac(sdxl2cprhs.macAddress()), errorMacNull, action, sdxl2cprhs.name());
        checkState(!sdxl2cplhs.macAddress().equals(sdxl2cprhs.macAddress()), errorMacEqual, action);
        super.addVC(sdxl2, sdxl2cplhs, sdxl2cprhs);
    }

    @Override
    public void removeVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs) {
        String action = "deleted";
        checkState(!(isNullMac(sdxl2cplhs.macAddress()) && isNullMac(sdxl2cprhs.macAddress())),
                   errorMacNull, action, sdxl2cplhs.name() + " and " + sdxl2cprhs.name());
        checkState(!sdxl2cplhs.macAddress().equals(MacAddress.ZERO), errorMacNull, action, sdxl2cplhs.name());
        checkState(!sdxl2cprhs.macAddress().equals(MacAddress.ZERO), errorMacNull, action, sdxl2cprhs.name());
        checkState(!sdxl2cplhs.macAddress().equals(sdxl2cprhs.macAddress()), errorMacEqual, action);
        super.removeVC(sdxl2cplhs, sdxl2cprhs);
    }

    @Override
    public void removeVC(SdxL2ConnectionPoint cp) {
        checkState(!cp.macAddress().equals(MacAddress.ZERO), errorMacNull, "deleted", cp.name());
        super.removeVC(cp);
    }

    /**
     * Determines whether a MAC address is empty or not.
     *
     * @param mac MacAddress object
     * @return boolean indicating whether MAC is empty (true) or not
     */
    private boolean isNullMac(MacAddress mac) {
        return mac.equals(MacAddress.ZERO);
    }

    /**
     * Returns the traffic selector, used in the definition of the intents.
     *
     * @param ingressMac Input MAC address
     * @param egressMac Output MAC address
     * @param etherType name of the Ethernet type used (e.g. of SDX-L2)
     * @param ingressTag VLAN id used at the ingress
     * @return TrafficSelector object
     */
    protected TrafficSelector buildSelector(MacAddress ingressMac,
                                          MacAddress egressMac,
                                          Short etherType,
                                          VlanId ingressTag) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthSrc(ingressMac);
        selectorBuilder.matchEthDst(egressMac);
        if (etherType != null) {
            selectorBuilder.matchEthType(etherType);
        }
        if (ingressTag != null) {
            selectorBuilder.matchVlanId(ingressTag);
        }
        return selectorBuilder.build();
    }
}
