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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Optional;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SDX-L2 MPLS VC Manager.
 */
public class SdxL2MplsVCManagerTest extends AbstractIntentTest {

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
    private static final String VLANS2 = "4,5,6";
    private static final String VLANS5 = "100";
    private static final String VLANS6 = "1";
    private static final String VLANS7 = "1";
    private static final String VLANS8 = "111";
    private static final String VLANS9 = "1";
    private static final String VLANS10 = "1";
    private static final String NAME_FORMAT = "%s:%s-%s";
    private static final String KEY_FORMAT = "%s,%s";
    private static final ApplicationId APPID = TestApplicationId.create("foo");
    private SdxL2MplsVCManager manager;
    private List<PointToPointIntent> intentList;

    /**
     * Prepares environment before starting testing.
     */
    @Before
    public void setUp() {
        super.setUp();
        SdxL2DistributedStore store = new SdxL2DistributedStore();
        store.initForTest();
        manager = new SdxL2MplsVCManager(APPID, store, new IntentServiceTest());
        intentList = setIntents();
    }

    /**
     * Cleans up environment after finishing testing.
     */
    @After
    public void tearDown() {
        super.tearDown();
    }

    /**
     * Defines the intents to be used when testing.
     *
     * @return List of point-to-point intents
     */
    private List<PointToPointIntent> setIntents() {
        List<PointToPointIntent> intents = new ArrayList<>();
        intents.addAll(setupConnectionPoints1To2());
        intents.addAll(setupConnectionPoints5To6());
        intents.addAll(setupConnectionPoints7To8());
        intents.addAll(setupConnectionPoints9To10());
        return intents;
    }

    /**
     * Returns the traffic treatment, used in the definition of the intents.
     *
     * @param setVlan VLAN to set
     * @param pushVlan VLAN to push
     * @param popVlan boolean to indicate whether a popVlan action is
     *                performed (true) or not (false)
     * @return TrafficTreatment object
     */
    private TrafficTreatment buildTreatment(VlanId setVlan,
                                            VlanId pushVlan,
                                            boolean popVlan) {
        return manager.buildTreatment(setVlan, pushVlan, popVlan);
    }

    /**
     * Returns the traffic selector, used in the definition of the intents.
     *
     * @param ethertype name of the Ethernet type used (e.g.  of SDX-L2
     * @param ingresstag VLAN id used at the ingress
     * @return TrafficSelector object
     */
    private TrafficSelector buildSelector(Short ethertype,
                                          VlanId ingresstag) {
        return manager.buildSelector(ethertype, ingresstag);
    }

    /**
     * Returns constraints depending on the encapsulation used on the VC.
     *
     * @return list of constraints to be used in the intents
     */
    private List<Constraint> buildConstraints() {
        return manager.buildConstraints();
    }

    /**
     * Returns Intent key from SDX-L2 and two SDX-L2 Connection Points.
     *
     * @param sdxl2 name of SDX-L2
     * @param cpOne sdxl2 connection point one
     * @param cpTwo sdxl2 connection point two
     * @param index digit used to help identify Intent
     * @return canonical intent string key
     */
    private Key generateIntentKey(String sdxl2, SdxL2ConnectionPoint cpOne, SdxL2ConnectionPoint cpTwo, String index) {
        return manager.generateIntentKey(sdxl2, cpOne, cpTwo, index);
    }

    /**
     * Ensures that when adding a VC its related intents are inserted.
     */
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

    /**
     * Defines the left-hand side endpoints, each with a specific VLAN.
     *
     * @return list of SdxL2ConnectionPoint objects
     */
    private List<SdxL2ConnectionPoint> setupLhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<>();
        SdxL2ConnectionPoint cpOne = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        cps.add(cpOne);
        SdxL2ConnectionPoint cpFive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        cps.add(cpFive);
        SdxL2ConnectionPoint cpSeven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        cps.add(cpSeven);
        SdxL2ConnectionPoint cpNine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", CP9, VLANS9);
        cps.add(cpNine);
        return cps;
    }

    /**
     * Defines the right-hand side endpoints, each with a specific VLAN.
     *
     * @return list of SdxL2ConnectionPoint objects
     */
    private List<SdxL2ConnectionPoint> setupRhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<>();
        SdxL2ConnectionPoint cpTwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);
        cps.add(cpTwo);
        SdxL2ConnectionPoint cpSix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        cps.add(cpSix);
        SdxL2ConnectionPoint cpEight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
        cps.add(cpEight);
        SdxL2ConnectionPoint cpTen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10);
        cps.add(cpTen);
        return cps;
    }

    /**
     * Creates the intents from the information of couple of endpoints.
     *
     * @param keyIndex identifier of the intent
     * @param lhs left-hand side Connection Point
     * @param lhsID DPID of the left-hand side CP
     * @param lhsVlan VLAN of the left-hand side CP
     * @param lhsBuiltTreatment specific treatment for the left-hand side CP
     * @param rhs right-hand side Connection Point
     * @param rhsID DPID of the right-hand side CP
     * @param rhsVlan VLAN of the right-hand side CP
     * @param rhsBuiltTreatment specific treatment for the right-hand side CP
     * @return List of point-to-point intents
     */
    private List<PointToPointIntent> setupConnectionPoints(String keyIndex,
                                                           SdxL2ConnectionPoint lhs, String lhsID,
                                                           String lhsVlan,
                                                           TrafficTreatment lhsBuiltTreatment,
                                                           SdxL2ConnectionPoint rhs, String rhsID,
                                                           String rhsVlan,
                                                           TrafficTreatment rhsBuiltTreatment) {
        List<PointToPointIntent> intents = new ArrayList<>();
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
                            .selector(buildSelector(null, lhsVlanValue))
                            .constraints(buildConstraints())
                            .treatment(lhsBuiltTreatment)
                            .ingressPoint(ConnectPoint.deviceConnectPoint(lhsID))
                            .egressPoint(ConnectPoint.deviceConnectPoint(rhsID))
                            .priority(2000)
                            .build());
        intents.add(PointToPointIntent.builder()
                            .appId(APPID)
                            .key(generateIntentKey(SDXL2_2, rhs, lhs, keyIndex))
                            .selector(buildSelector(null, rhsVlanValue))
                            .constraints(buildConstraints())
                            .treatment(rhsBuiltTreatment)
                            .ingressPoint(ConnectPoint.deviceConnectPoint(rhsID))
                            .egressPoint(ConnectPoint.deviceConnectPoint(lhsID))
                            .priority(2000)
                            .build());
        return intents;
    }

    /**
     * Defines three pairs of Connection Points, each with a specific VLAN.
     * The intents are created aftewards, from this input.
     *
     * @return list of point-to-point intents
     */
    private List<PointToPointIntent> setupConnectionPoints1To2() {
        List<PointToPointIntent> intents = new ArrayList<>();
        String lhsID = CP1;
        ArrayList<String> lhsVlan =  new ArrayList<>(Arrays.asList(VLANS1.split(",")));
        String rhsID = CP2;
        ArrayList<String> rhsVlan =  new ArrayList<>(Arrays.asList(VLANS2.split(",")));
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint(
                "TEST1", lhsID, VLANS1);
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint(
                "TEST2", rhsID, VLANS2);
        TrafficTreatment lhsBuiltTreatment, rhsBuiltTreatment;

        for (int i = 0; i < 3; i++) {
            lhsBuiltTreatment = buildTreatment(VlanId.vlanId(rhsVlan.get(i)), null, false);
            rhsBuiltTreatment = buildTreatment(VlanId.vlanId(lhsVlan.get(i)), null, false);
            intents.addAll(setupConnectionPoints(Integer.toString(i + 1),
                                                 lhs, lhsID, lhsVlan.get(i), lhsBuiltTreatment,
                                                 rhs, rhsID, rhsVlan.get(i), rhsBuiltTreatment));
        }
        return intents;
    }

    /**
     * Defines a couple of Connection Points, each with a specific VLAN.
     * The intents are created aftewards, from this input.
     *
     * @return list of point-to-point intents
     */
    private List<PointToPointIntent> setupConnectionPoints5To6() {
        String lhsID = CP5;
        String lhsVlan = VLANS5;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", lhsID, lhsVlan);
        String rhsID = CP6;
        String rhsVlan = VLANS6;
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", rhsID, rhsVlan);
        TrafficTreatment lhsBuiltTreatment =  buildTreatment(null, null, true);
        TrafficTreatment rhsBuiltTreatment = buildTreatment(null, VlanId.vlanId(Short.parseShort(lhsVlan)), false);
        return setupConnectionPoints("1", lhs, lhsID, lhsVlan, lhsBuiltTreatment,
                                     rhs, rhsID, null, rhsBuiltTreatment);
    }

    /**
     * Defines a couple of Connection Points, each with a specific VLAN.
     * The intents are created aftewards, from this input.
     *
     * @return list of point-to-point intents
     */
    private List<PointToPointIntent> setupConnectionPoints7To8() {
        String lhsID = CP7;
        String lhsVlan = VLANS7;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", lhsID, lhsVlan);
        String rhsID = CP8;
        String rhsVlan = VLANS8;
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", rhsID, rhsVlan);
        TrafficTreatment lhsBuiltTreatment = buildTreatment(null, VlanId.vlanId(Short.parseShort(rhsVlan)), false);
        TrafficTreatment rhsBuiltTreatment = buildTreatment(null, null, true);
        return setupConnectionPoints("1", lhs, lhsID, null, lhsBuiltTreatment,
                                     rhs, rhsID, rhsVlan, rhsBuiltTreatment);
    }

    /**
     * Defines a couple of Connection Points, each with a specific VLAN.
     * The intents are created aftewards, from this input.
     *
     * @return list of point-to-point intents
     */
    private List<PointToPointIntent> setupConnectionPoints9To10() {
        String lhsID = CP9;
        String lhsVlan = VLANS9;
        SdxL2ConnectionPoint lhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST9", lhsID, lhsVlan);
        String rhsID = CP10;
        String rhsVlan = VLANS10;
        SdxL2ConnectionPoint rhs = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", rhsID, rhsVlan);
        TrafficTreatment nullTreatment = buildTreatment(null, null, false);
        return setupConnectionPoints("1", lhs, lhsID, null, nullTreatment,
                                     rhs, rhsID, null, nullTreatment);
    }

    /**
     * Ensures that when removing a VC its related intents are deleted.
     */
    @Test
    public void removeVCAndIntents() {
        testConnectionSetup();
        List<PointToPointIntent> removedIntents = new ArrayList<>();

        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        SdxL2ConnectionPoint cptwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2);
        removedIntents.addAll(setupConnectionPoints1To2());

        SdxL2ConnectionPoint cpfive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST5", CP5, VLANS5);
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        removedIntents.addAll(setupConnectionPoints5To6());

        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        SdxL2ConnectionPoint cpeight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST8", CP8, VLANS8);
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

    /**
     * Ensures that when removing a CP its related VCs and intents are also deleted.
     */
    @Test
    public void testRemoveVCbyCP() {
        testConnectionSetup();

        List<PointToPointIntent> removedIntents = new ArrayList<>();
        SdxL2ConnectionPoint cpone = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1);
        removedIntents.addAll(setupConnectionPoints1To2());
        SdxL2ConnectionPoint cpsix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST6", CP6, VLANS6);
        removedIntents.addAll(setupConnectionPoints5To6());
        SdxL2ConnectionPoint cpseven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST7", CP7, VLANS7);
        removedIntents.addAll(setupConnectionPoints7To8());
        SdxL2ConnectionPoint cpten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST10", CP10, VLANS10);
        removedIntents.addAll(setupConnectionPoints9To10());

        manager.removeVC(cpone);
        manager.removeVC(cpsix);
        manager.removeVC(cpseven);
        manager.removeVC(cpten);

        assertEquals(Collections.emptySet(), manager.getVCs(Optional.empty()));

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

    /**
     * Ensures that when removing a CP its related VCs and intents are also deleted.
     */
    @Test
    public void testRemoveVCbySdx() {
        testConnectionSetup();

        List<PointToPointIntent> removedIntents = new ArrayList<>();
        removedIntents.addAll(setupConnectionPoints1To2());
        removedIntents.addAll(setupConnectionPoints5To6());
        removedIntents.addAll(setupConnectionPoints7To8());
        removedIntents.addAll(setupConnectionPoints9To10());

        manager.removeVCs(SDXL2_2);
        assertEquals(Collections.emptySet(), manager.getVCs(Optional.empty()));
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
