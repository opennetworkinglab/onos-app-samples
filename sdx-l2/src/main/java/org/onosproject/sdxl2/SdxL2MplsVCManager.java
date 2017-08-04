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


import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages Virtual Circuits using MPLS headers.
 */
public class SdxL2MplsVCManager extends SdxL2VCManager {

    private static final int PRIORITY_OFFSET = 2000;
    private static Logger log = LoggerFactory.getLogger(SdxL2MplsVCManager.class);

    // TODO Remember to create two intents: one for IPv4 and one for IPv6

    /**
     * Creates a SDX-L2 MPLS VC Manager.
     *
     * @param sdxl2id application ID
     * @param store reference to the SDX-L2 store
     * @param intentService reference to the Intent service
     */
    public SdxL2MplsVCManager(ApplicationId sdxl2id,
                              SdxL2Store store,
                              IntentService intentService) {

        super(sdxl2id, store, intentService);
        log.info("Started");
    }

    @Override
    public Collection<Intent> buildIntents(String sdxl2, SdxL2ConnectionPoint ingress,
                                           SdxL2ConnectionPoint egress) {
        List<Intent> intents = null;
        TrafficSelector selector;
        TrafficTreatment treatment;
        List<Constraint> encapsulation;
        Key key;

        if (ingress.vlanIds().size() == egress.vlanIds().size()) {
            intents = new ArrayList<>();
            if (ingress.vlanIds().size() == 0) {

                selector = buildSelector(null, null);
                treatment = DefaultTrafficTreatment.emptyTreatment();
                encapsulation = buildConstraints();

                key = generateIntentKey(sdxl2, ingress, egress, "1");

                intents.add(PointToPointIntent.builder()
                                    .appId(appId)
                                    .key(key)
                                    .selector(selector)
                                    .treatment(treatment)
                                    .constraints(encapsulation)
                                    .ingressPoint(ingress.connectPoint())
                                    .egressPoint(egress.connectPoint())
                                    .priority(PRIORITY_OFFSET)
                                    .build());
            } else {

                Iterator<VlanId> ingressTags = ingress.vlanIds().iterator();
                Iterator<VlanId> egressTags = egress.vlanIds().iterator();
                int index = 1;
                while (ingressTags.hasNext()) {
                    selector = buildSelector(null, ingressTags.next());
                    treatment = buildTreatment(egressTags.next(),
                                               null,
                                               false);
                    encapsulation = buildConstraints();

                    key = generateIntentKey(sdxl2, ingress, egress, String.valueOf(index));

                    intents.add(PointToPointIntent.builder()
                                        .appId(appId)
                                        .key(key)
                                        .selector(selector)
                                        .treatment(treatment)
                                        .constraints(encapsulation)
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

            selector = buildSelector(null, ingressTags.next());
            treatment = buildTreatment(null,
                                       null,
                                       true);
            encapsulation = buildConstraints();


            key = generateIntentKey(sdxl2, ingress, egress, "1");

            intents.add(PointToPointIntent.builder()
                                .appId(appId)
                                .key(key)
                                .selector(selector)
                                .treatment(treatment)
                                .constraints(encapsulation)
                                .ingressPoint(ingress.connectPoint())
                                .egressPoint(egress.connectPoint())
                                .priority(PRIORITY_OFFSET)
                                .build());
            return intents;

        }

        if (ingress.vlanIds().size() == 0 && egress.vlanIds().size() == 1) {

            Iterator<VlanId> egressTags = egress.vlanIds().iterator();
            intents = new ArrayList<>();

            selector = buildSelector(null, null);
            treatment = buildTreatment(null,
                                       egressTags.next(),
                                       false);
            encapsulation = buildConstraints();

            key = generateIntentKey(sdxl2, ingress, egress, "1");

            intents.add(PointToPointIntent.builder()
                                .appId(appId)
                                .key(key)
                                .selector(selector)
                                .treatment(treatment)
                                .constraints(encapsulation)
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
    protected List<Constraint> buildConstraints() {
        final List<Constraint> constraints = new LinkedList<>();
        constraints.add(new EncapsulationConstraint(EncapsulationType.MPLS));
        return constraints;
    }

}