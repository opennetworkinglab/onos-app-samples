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
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for SDX-L2 Mac VC Manager.
 */
public class SdxL2MacVCManagerTest extends AbstractIntentTest {

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
            new ArrayList<String>(Arrays.asList(VLANS1.split(",")));
    private static final String VLANS2 = "4,5,6";
    private static final ArrayList<String> VLANS2_ARRAY =
            new ArrayList<String>(Arrays.asList(VLANS2.split(",")));

    private static final String VLANS5 = "100";
    private static final String VLANS6 = "1";
    private static final String VLANS7 = "1";
    private static final String VLANS8 = "111";
    private static final String VLANS9 = "1";
    private static final String VLANS10 = "1";
    private static final String CEMAC1 = "52:40:00:12:44:01";
    private static final String CEMAC2 = "51:12:11:00:23:01";
    private static final String CEMAC5 = "52:12:11:00:23:11";
    private static final String CEMAC6 = "52:12:11:a0:23:11";
    private static final String CEMAC7 = "52:12:21:00:25:11";
    private static final String CEMAC8 = "52:12:14:a0:23:11";
    private static final String CEMAC9 = "52:12:21:00:28:11";
    private static final String CEMAC10 = "52:12:14:aa:23:11";
    private static final String NAME_FORMAT = "%s:%s-%s";
    private static final String KEY_FORMAT = "%s,%s";
    private static final ApplicationId APPID = TestApplicationId.create("foo");
    private static final int POINT_TO_POINT_INDEXES = 3;
    private SdxL2MacVCManager manager;
    private List<PointToPointIntent> intentList;

    /**
     * Prepare environment before starting testing MAC-based VCs.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        SdxL2DistributedStore store = new SdxL2DistributedStore();
        store.initForTest();
        manager = new SdxL2MacVCManager(APPID, store, new IntentServiceTest());
        intentList = setIntents();
    }

    /**
     * Clean up environment after finishing testing MAC-based VCs.
     */
    @After
    public void tearDown() {
        super.tearDown();
    }

    public List<PointToPointIntent> setIntents() {
        List<PointToPointIntent> intents = new ArrayList<PointToPointIntent>();
        intents.addAll(setupConnectionPoints1To2());
        intents.addAll(setupConnectionPoints5To6());
        intents.addAll(setupConnectionPoints7To8());
        intents.addAll(setupConnectionPoints9To10());
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

    private TrafficSelector buildSelector(MacAddress ingressMac,
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

    private Key generateIntentKey(String sdxl2, SdxL2ConnectionPoint cpOne, SdxL2ConnectionPoint cpTwo, String index) {
        String cps = format(NAME_FORMAT, sdxl2, cpOne.name(), cpTwo.name());
        String key = format(KEY_FORMAT, cps, index);
        return Key.of(key, APPID);
    }

    @Test
    public void testConnectionSetup() {
        Iterator<SdxL2ConnectionPoint> lhs = setupLhsCPs().iterator();
        Iterator<SdxL2ConnectionPoint> rhs = setupRhsCPs().iterator();
        while (lhs.hasNext()) {
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
        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        cps.add(cpone);
        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5, CEMAC5);
        cps.add(cpfive);
        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7, CEMAC7);
        cps.add(cpseven);
        SdxL2ConnectionPoint cpnine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", CP9, VLANS9, CEMAC9);
        cps.add(cpnine);
        return cps;
    }

    public List<SdxL2ConnectionPoint> setupRhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<SdxL2ConnectionPoint>();
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC2);
        cps.add(cptwo);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6, CEMAC6);
        cps.add(cpsix);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8, CEMAC8);
        cps.add(cpeight);
        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10, CEMAC10);
        cps.add(cpten);
        return cps;
    }

    private List<PointToPointIntent> setupConnectionPoints(String keyIndex,
                                                           SdxL2ConnectionPoint lhs, String lhsID,
                                                           String lhsMac, String lhsVlan,
                                                           TrafficTreatment lhsBuiltTreatment,
                                                           SdxL2ConnectionPoint rhs, String rhsID,
                                                           String rhsMac, String rhsVlan,
                                                           TrafficTreatment rhsBuiltTreatment) {
        List<PointToPointIntent> intents = new ArrayList<PointToPointIntent>();
        VlanId lhsVlanValue = null, rhsVlanValue = null;
        if (lhsVlan != null) {
            lhsVlanValue = VlanId.vlanId(Short.parseShort(lhsVlan));
        }
        if (rhsVlan != null) {
            rhsVlanValue = VlanId.vlanId(Short.parseShort(rhsVlan));
        }

        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, lhs, rhs, keyIndex))
                            .selector(buildSelector(MacAddress.valueOf(lhsMac),
                                                    MacAddress.valueOf(rhsMac),
                                                    null, lhsVlanValue))
                            .treatment(lhsBuiltTreatment)
                            .ingressPoint(ConnectPoint.deviceConnectPoint(lhsID))
                            .egressPoint(ConnectPoint.deviceConnectPoint(rhsID))
                            .priority(2000)
                            .build());
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, rhs, lhs, keyIndex))
                            .selector(buildSelector(MacAddress.valueOf(rhsMac),
                                                    MacAddress.valueOf(lhsMac),
                                                    null, rhsVlanValue))
                            .treatment(rhsBuiltTreatment)
                            .ingressPoint(ConnectPoint.deviceConnectPoint(rhsID))
                            .egressPoint(ConnectPoint.deviceConnectPoint(lhsID))
                            .priority(2000)
                            .build());
        return intents;
    }

    private List<PointToPointIntent> setupConnectionPoints1To2() {
        List<PointToPointIntent> intents = new ArrayList<PointToPointIntent>();
        String lhsID = CP1;
        ArrayList<String> lhsVlan = VLANS1_ARRAY;
        String lhsMac = CEMAC1;
        String rhsID = CP2;
        ArrayList<String> rhsVlan = VLANS2_ARRAY;
        String rhsMac = CEMAC2;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint(
                "TEST1", lhsID, VLANS1, lhsMac);
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint(
                "TEST2", rhsID, VLANS2, rhsMac);
        TrafficTreatment lhsBuiltTreatment, rhsBuiltTreatment;

        for (int i = 0; i < POINT_TO_POINT_INDEXES; i++) {
            lhsBuiltTreatment = buildTreatment(VlanId.vlanId(rhsVlan.get(i)), null, false);
            rhsBuiltTreatment = buildTreatment(VlanId.vlanId(lhsVlan.get(i)), null, false);
            intents.addAll(setupConnectionPoints(Integer.toString(i + 1),
                                    lhs, lhsID, lhsMac, lhsVlan.get(i), lhsBuiltTreatment,
                                    rhs, rhsID, rhsMac, rhsVlan.get(i), rhsBuiltTreatment));
        }
        return intents;
    }

    private List<PointToPointIntent> setupConnectionPoints5To6() {
        String lhsID = CP5;
        String lhsVlan = VLANS5;
        String lhsMac = CEMAC5;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", lhsID, lhsVlan, lhsMac);
        String rhsID = CP6;
        String rhsVlan = VLANS6;
        String rhsMac = CEMAC6;
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", rhsID, rhsVlan, rhsMac);
        TrafficTreatment lhsBuiltTreatment =  buildTreatment(null, null, true);
        TrafficTreatment rhsBuiltTreatment = buildTreatment(null, VlanId.vlanId(Short.parseShort(lhsVlan)), false);
        return setupConnectionPoints("1", lhs, lhsID, lhsMac, lhsVlan, lhsBuiltTreatment,
                                     rhs, rhsID, rhsMac, null, rhsBuiltTreatment);
    }

    private List<PointToPointIntent> setupConnectionPoints7To8() {
        String lhsID = CP7;
        String lhsVlan = VLANS7;
        String lhsMac = CEMAC7;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", lhsID, lhsVlan, lhsMac);
        String rhsID = CP8;
        String rhsVlan = VLANS8;
        String rhsMac = CEMAC8;
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", rhsID, rhsVlan, rhsMac);
        TrafficTreatment lhsBuiltTreatment = buildTreatment(null, VlanId.vlanId(Short.parseShort(rhsVlan)), false);
        TrafficTreatment rhsBuiltTreatment = buildTreatment(null, null, true);
        return setupConnectionPoints("1", lhs, lhsID, lhsMac, null, lhsBuiltTreatment,
                                     rhs, rhsID, rhsMac, rhsVlan, rhsBuiltTreatment);
    }

    private List<PointToPointIntent> setupConnectionPoints9To10() {
        String lhsID = CP9;
        String lhsVlan = VLANS9;
        String lhsMac = CEMAC9;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", lhsID, lhsVlan, lhsMac);
        String rhsID = CP10;
        String rhsVlan = VLANS10;
        String rhsMac = CEMAC10;
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", rhsID, rhsVlan, rhsMac);
        TrafficTreatment nullTreatment = buildTreatment(null, null, false);
        return setupConnectionPoints("1", lhs, lhsID, lhsMac, null, nullTreatment,
                                     rhs, rhsID, rhsMac, null, nullTreatment);
    }

    @Test
    public void removeConnection() {
        testConnectionSetup();
        List<PointToPointIntent> removedIntents = new ArrayList<PointToPointIntent>();

        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC2);
        removedIntents.addAll(setupConnectionPoints1To2());

        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5, CEMAC5);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6, CEMAC6);
        removedIntents.addAll(setupConnectionPoints5To6());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7, CEMAC7);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8, CEMAC8);
        removedIntents.addAll(setupConnectionPoints7To8());

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

        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        removedIntents.addAll(setupConnectionPoints1To2());

        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6, CEMAC6);
        removedIntents.addAll(setupConnectionPoints5To6());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7, CEMAC7);
        removedIntents.addAll(setupConnectionPoints7To8());

        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10, CEMAC10);
        removedIntents.addAll(setupConnectionPoints9To10());

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
    public void testRemoveVCbySdx() {
        testConnectionSetup();

        List<PointToPointIntent> removedIntents = new ArrayList<PointToPointIntent>();

        removedIntents.addAll(setupConnectionPoints1To2());
        removedIntents.addAll(setupConnectionPoints5To6());
        removedIntents.addAll(setupConnectionPoints7To8());
        removedIntents.addAll(setupConnectionPoints9To10());
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
