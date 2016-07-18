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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SdxL2VlanVCManager.
 */
public class SdxL2VlanVCManagerTest extends AbstractIntentTest {

    private static final String SDXL2_2 = "sdxl2_test2";
    private static final String CP1 = "of:00000000000001/1";
    private static final String CP2 = "of:00000000000002/1";
    private static final String CP5 = "of:00000000000002/4";
    private static final String CP6 = "of:00000000000004/4";
    private static final String CP7 = "of:0000000000000a/4";
    private static final String CP8 = "of:00000000000009/4";
    private static final String CP9 = "of:0000000000000a/4";
    private static final String CP10 = "of:00000000000009/4";
    private static final String VLANS1 = "2,3,4";
    private static final ArrayList<String> VLANS1_ARRAY =
            new ArrayList<String>(Arrays.asList(VLANS1.split(",")));;
    private static final String VLANS2 = "4,5,6";
    private static final ArrayList<String> VLANS2_ARRAY =
            new ArrayList<String>(Arrays.asList(VLANS2.split(",")));;
    private static final String VLANS5 = "100";
    private static final String VLANS6 = "1";
    private static final String VLANS7 = "1";
    private static final String VLANS8 = "111";
    private static final String VLANS9 = "1";
    private static final String VLANS10 = "1";
    private static final String NAME_FORMAT = "%s:%s-%s";
    private static final String KEY_FORMAT = "%s,%s";
    private static final ApplicationId APPID = TestApplicationId.create("foo");
    private static final int POINT_TO_POINT_INDEXES = 3;
    private SdxL2VlanVCManager manager;
    private List<PointToPointIntent> intentList;

    /**
     * Prepare environment before starting testing VLAN-based VCs.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        SdxL2DistributedStore store = new SdxL2DistributedStore();
        store.initForTest();
        manager = new SdxL2VlanVCManager(
                APPID, store, new IntentServiceTest());
        intentList = setIntents();
    }

    /**
     * Clean up environment after finishing testing VLAN-based VCs.
     */
    @After
    public void tearDown() {
        super.tearDown();
    }

    public List<PointToPointIntent> setIntents() {
        List<PointToPointIntent> intents = new ArrayList<PointToPointIntent>();
        List<Constraint> encapsulation = buildConstraints();

        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);
        for (int i = 0; i < POINT_TO_POINT_INDEXES; i++) {
            intents.add(PointToPointIntent.builder()
                                .appId(APPID)
                                .key(generateIntentKey(SDXL2_2, cpone, cptwo, Integer.toString(i + 1))) // 1
                                .selector(buildSelector(null, VlanId.vlanId(Short.parseShort(VLANS1_ARRAY.get(i)))))
                                .treatment(buildTreatment(
                                        VlanId.vlanId(Short.parseShort(VLANS2_ARRAY.get(i))), null, false))
                                .constraints(encapsulation)
                                .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                .priority(2000)
                                .build());
            intents.add(PointToPointIntent.builder()
                                .appId(APPID)
                                .key(generateIntentKey(SDXL2_2, cptwo, cpone, Integer.toString(i + 1)))
                                .selector(buildSelector(null, VlanId.vlanId(Short.parseShort(VLANS2_ARRAY.get(i)))))
                                .treatment(buildTreatment(
                                        VlanId.vlanId(Short.parseShort(VLANS1_ARRAY.get(i))), null, false))
                                .constraints(encapsulation)
                                .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                .priority(2000)
                                .build());
        }

        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, cpfive, cpsix, "1"))
                            .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("100"))))
                            .treatment(buildTreatment(null, null, true))
                            .constraints(encapsulation)
                            .ingressPoint(ConnectPoint.deviceConnectPoint(CP5))
                            .egressPoint(ConnectPoint.deviceConnectPoint(CP6))
                            .priority(2000)
                            .build());
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, cpsix, cpfive, "1"))
                            .selector(buildSelector(null, null))
                            .constraints(encapsulation)
                            .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("100")), false))
                            .ingressPoint(ConnectPoint.deviceConnectPoint(CP6))
                            .egressPoint(ConnectPoint.deviceConnectPoint(CP5))
                            .priority(2000)
                            .build());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, cpseven, cpeight, "1"))
                            .selector(buildSelector(null, null))
                            .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("111")), false))
                            .constraints(encapsulation)
                            .ingressPoint(ConnectPoint.deviceConnectPoint(CP7))
                            .egressPoint(ConnectPoint.deviceConnectPoint(CP8))
                            .priority(2000)
                            .build());
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, cpeight, cpseven, "1"))
                            .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("111"))))
                            .constraints(encapsulation)
                            .treatment(buildTreatment(null, null, true))
                            .ingressPoint(ConnectPoint.deviceConnectPoint(CP8))
                            .egressPoint(ConnectPoint.deviceConnectPoint(CP7))
                            .priority(2000)
                            .build());

        SdxL2ConnectionPoint cpnine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", CP9, VLANS9);
        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10);
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, cpnine, cpten, "1"))
                            .selector(buildSelector(null, null))
                            .constraints(encapsulation)
                            .treatment(buildTreatment(null, null, false))
                            .ingressPoint(ConnectPoint.deviceConnectPoint(CP9))
                            .egressPoint(ConnectPoint.deviceConnectPoint(CP10))
                            .priority(2000)
                            .build());
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, cpten, cpnine, "1"))
                            .selector(buildSelector(null, null))
                            .constraints(encapsulation)
                            .treatment(buildTreatment(null, null, false))
                            .ingressPoint(ConnectPoint.deviceConnectPoint(CP10))
                            .egressPoint(ConnectPoint.deviceConnectPoint(CP9))
                            .priority(2000)
                            .build());

        return intents;
    }

    private TrafficTreatment buildTreatment(VlanId setVlan,
                                            VlanId pushVlan,
                                            boolean popVlan) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (setVlan != null) {
            treatmentBuilder.setVlanId(setVlan);
        }
        if (pushVlan != null) {
            treatmentBuilder.pushVlan();
            treatmentBuilder.setVlanId(pushVlan);
        }
        if (popVlan) {
            treatmentBuilder.popVlan();
        }
        return treatmentBuilder.build();
    }

    private TrafficSelector buildSelector(Short ethertype,
                                          VlanId ingresstag) {

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        if (ethertype != null) {
            selectorBuilder.matchEthType(ethertype);
        }
        if (ingresstag != null) {
            selectorBuilder.matchVlanId(ingresstag);
        }
        return selectorBuilder.build();
    }

    protected List<Constraint> buildConstraints() {
        final List<Constraint> constraints = new LinkedList<>();
        constraints.add(new EncapsulationConstraint(EncapsulationType.VLAN));
        return constraints;
    }

    private Key generateIntentKey(String sdxl2, SdxL2ConnectionPoint cpone, SdxL2ConnectionPoint cptwo, String index) {
        String cps = format(NAME_FORMAT, sdxl2, cpone.name(), cptwo.name());
        String key = format(KEY_FORMAT, cps, index);
        return Key.of(key, APPID);
    }

    @Test
    public void testConnectionSetup() {
        Iterator<SdxL2ConnectionPoint> lhs = setupLhsCPs().iterator();
        Iterator<SdxL2ConnectionPoint> rhs = setupRhsCPs().iterator();
        while (lhs.hasNext() && rhs.hasNext()) {
            manager.addVC(SDXL2_2, lhs.next(), rhs.next());
        }

        assertEquals(intentList.size(), manager.intentService.getIntentCount());

        for (Intent emulatedIntent : intentList) {
            boolean found = false;
            for (Intent realIntent : manager.intentService.getIntents()) {
                if (emulatedIntent.key().equals(realIntent.key())) {
                    found = true;
                    assertTrue(format("Comparing %s and %s", emulatedIntent, realIntent),
                               IntentUtils.intentsAreEqual(emulatedIntent, realIntent));
                    break;
                }
            }
            assertTrue(found);
        }
    }

    public List<SdxL2ConnectionPoint> setupLhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<SdxL2ConnectionPoint>();
        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        cps.add(cpone);
        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        cps.add(cpfive);
        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        cps.add(cpseven);
        SdxL2ConnectionPoint cpnine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", CP9, VLANS9);
        cps.add(cpnine);
        return cps;
    }

    public List<SdxL2ConnectionPoint> setupRhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<SdxL2ConnectionPoint>();
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);
        cps.add(cptwo);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        cps.add(cpsix);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
        cps.add(cpeight);
        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10);
        cps.add(cpten);
        return cps;
    }


    @Test
    public void removeConnection() {
        testConnectionSetup();

        List<PointToPointIntent> removedIntents = new ArrayList<PointToPointIntent>();
        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);

        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("2"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("4")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("4"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("2")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "2"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("3"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("5")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "2"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("5"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("3")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "3"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("4"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("6")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "3"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("6"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("4")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpfive, cpsix, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("100"))))
                                    .treatment(buildTreatment(null, null, true))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP5))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP6))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpsix, cpfive, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("100")), false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP6))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP5))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpseven, cpeight, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("111")), false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP7))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP8))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpeight, cpseven, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("111"))))
                                    .treatment(buildTreatment(null, null, true))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP8))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP7))
                                    .priority(2000)
                                    .build());

        manager.removeVC(cpone, cptwo);
        manager.removeVC(cpfive, cpsix);
        manager.removeVC(cpseven, cpeight);

        assertEquals(2, manager.intentService.getIntentCount());

        for (Intent removedIntent : removedIntents) {
            boolean found = false;
            for (Intent existingIntent : manager.intentService.getIntents()) {
                if (removedIntent.key().equals(existingIntent.key())) {
                    found = true;
                    assertTrue(format("Intent %s equal %s", removedIntent, existingIntent),
                               !IntentUtils.intentsAreEqual(removedIntent, existingIntent));
                    break;
                }
            }
            assertTrue(!found);
        }

    }


    @Test
    public void testRemoveVCbyCP() {
        testConnectionSetup();

        List<PointToPointIntent> removedIntents = new ArrayList<PointToPointIntent>();
        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);

        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("2"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("4")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("4"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("2")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "2"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("3"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("5")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "2"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("5"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("3")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "3"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("4"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("6")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "3"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("6"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("4")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpfive, cpsix, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("100"))))
                                    .treatment(buildTreatment(null, null, true))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP5))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP6))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpsix, cpfive, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("100")), false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP6))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP5))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpseven, cpeight, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("111")), false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP7))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP8))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpeight, cpseven, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("111"))))
                                    .treatment(buildTreatment(null, null, true))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP8))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP7))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpnine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", CP9, VLANS9);
        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpnine, cpten, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP9))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP10))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpten, cpnine, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP10))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP9))
                                    .priority(2000)
                                    .build());

        manager.removeVC(cpone);
        manager.removeVC(cpsix);
        manager.removeVC(cpseven);
        manager.removeVC(cpten);

        assertEquals(Collections.emptySet(), manager.getVCs(Optional.ofNullable(null)));

        assertEquals(0, manager.intentService.getIntentCount());

        for (Intent removedIntent : removedIntents) {
            boolean found = false;
            for (Intent existingIntent : manager.intentService.getIntents()) {
                if (removedIntent.key().equals(existingIntent.key())) {
                    found = true;
                    assertTrue(format("Intent %s equal %s", removedIntent, existingIntent),
                               !IntentUtils.intentsAreEqual(removedIntent, existingIntent));
                    break;
                }
            }
            assertTrue(!found);
        }

    }

    @Test
    public void testremoveVCbySdx() {
        testConnectionSetup();

        List<PointToPointIntent> removedIntents = new ArrayList<PointToPointIntent>();
        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);

        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("2"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("4")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("4"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("2")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "2"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("3"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("5")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "2"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("5"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("3")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpone, cptwo, "3"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("4"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("6")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cptwo, cpone, "3"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("6"))))
                                    .treatment(buildTreatment(VlanId.vlanId(Short.parseShort("4")), null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP2))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP1))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpfive, cpsix, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("100"))))
                                    .treatment(buildTreatment(null, null, true))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP5))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP6))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpsix, cpfive, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("100")), false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP6))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP5))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpseven, cpeight, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, VlanId.vlanId(Short.parseShort("111")), false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP7))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP8))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpeight, cpseven, "1"))
                                    .selector(buildSelector(null, VlanId.vlanId(Short.parseShort("111"))))
                                    .treatment(buildTreatment(null, null, true))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP8))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP7))
                                    .priority(2000)
                                    .build());

        SdxL2ConnectionPoint cpnine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", CP9, VLANS9);
        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10);
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpnine, cpten, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP9))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP10))
                                    .priority(2000)
                                    .build());
        removedIntents.add(PointToPointIntent.builder()
                                    .appId(APPID)
                                    .key(generateIntentKey(SDXL2_2, cpten, cpnine, "1"))
                                    .selector(buildSelector(null, null))
                                    .treatment(buildTreatment(null, null, false))
                                    .ingressPoint(ConnectPoint.deviceConnectPoint(CP10))
                                    .egressPoint(ConnectPoint.deviceConnectPoint(CP9))
                                    .priority(2000)
                                    .build());

        manager.removeVCs(SDXL2_2);
        assertEquals(Collections.emptySet(), manager.getVCs(Optional.ofNullable(null)));
        assertEquals(Collections.emptySet(), manager.getVCs(Optional.of(SDXL2_2)));

        for (Intent removedIntent : removedIntents) {
            boolean found = false;
            for (Intent existingIntent : manager.intentService.getIntents()) {
                if (removedIntent.key().equals(existingIntent.key())) {
                    found = true;
                    assertTrue(format("Intent %s equal %s", removedIntent, existingIntent),
                               !IntentUtils.intentsAreEqual(removedIntent, existingIntent));
                    break;
                }
            }
            assertTrue(!found);
        }

    }
}