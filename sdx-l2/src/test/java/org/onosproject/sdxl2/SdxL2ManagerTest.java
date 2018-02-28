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

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.TestApplicationId;
import org.onosproject.net.intent.MockIdGenerator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.*;

/**
 * Tests SdxL2Manager functionality.
 */
public class SdxL2ManagerTest {

    private static final String SDXL2 = "test";
    private static final String SDXL2_2 = "test2";
    private static final String CP1 = "of:00000000000001/1";
    private static final String CP2 = "of:00000000000002/1";
    private static final String CP3 = "of:00000000000003/1";
    private static final String CP4 = "of:00000000000003/2";
    private static final String CP5 = "of:00000000000004/2";
    private static final String VLANS1 = "1,2,3,4";
    private static final String VLANS2 = "-1";
    private static final String VLANS3 = "1,2,3";
    private static final String VLANS4 = "2,2,2";
    private static final String VLANS7 = "5";
    private static final String VLANS8 = "3,2,1";
    private static final String CEMAC1 = "52:40:00:12:44:01";
    private static final String CEMAC2 = "52:44:00:12:44:01";
    private static final String CEMAC3 = "54:40:00:12:44:01";
    private static final String CEMAC4 = "52:40:00:12:42:01";
    private static final String CEMAC5 = "52:40:00:10:44:01";
    private static final String CEMAC6 = "52:40:00:12:46:01";
    private static final String CP6 = "of:00000000000004/3";
    private static final String CP7 = "of:00000000000004/3";
    private static final String CP8 = "of:00000000000005/2";
    private static final String VLANS6 = "1,2,3,4";
    private static final String VLANS5 = "8,9,10";
    private static final String CEMAC7 = "52:40:00:12:46:01";
    private static final String CEMAC8 = "52:40:90:12:46:01";

    /**
     * Exception expected to raise when creating VC with non-existing CP(s).
     */
    @Rule
    public ExpectedException exceptionAddVC = ExpectedException.none();
    /**
     * Exception expected to raise when removing non-existing VC.
     */
    @Rule
    public ExpectedException exceptionRemoveVC = ExpectedException.none();
    /**
     * Exception expected to raise when creating SDX-L2 with missing or bad name.
     */
    @Rule
    public ExpectedException exceptionSdxL2Name = ExpectedException.none();
    /**
     * Exception expected to raise when removing SDX-L2 passing no name.
     */
    @Rule
    public ExpectedException exceptionDelSdxL2Name = ExpectedException.none();
    /**
     * Exception expected to raise when retrieving VC using a bad name format.
     */
    @Rule
    public ExpectedException exceptionGetVC = ExpectedException.none();
    /**
     * Exception expected to raise when removing SDX-L2 CPs that do not exist.
     */
    @Rule
    public ExpectedException exceptionRemoveCP = ExpectedException.none();
    private SdxL2Manager manager;

    @Before
    public void setUp() {
        MockIdGenerator.cleanBind();
        manager = new SdxL2Manager();
        manager.appId = new TestApplicationId("sdxl2-test");
        SdxL2DistributedStore store = new SdxL2DistributedStore();
        store.initForTest();
        manager.sdxL2Store = store;
        manager.vcManager = new SdxL2MacVCManager(
                manager.appId, manager.sdxL2Store, new IntentServiceTest());
    }

    @After
    public void tearDown() {
        MockIdGenerator.unbind();
    }

    /**
     * Ensures SDXs are properly created (no exception is thrown).
     */
    @Test
    public void testCreateSdxL2s() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);
        manager.createSdxL2(SDXL2);
    }

    /**
     * Ensures that SDXs are properly removed (no exception is thrown).
     */
    @Test
    public void testRemoveSdxL2s() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);
        manager.deleteSdxL2(SDXL2);
        manager.deleteSdxL2(SDXL2_2);
        manager.deleteSdxL2(SDXL2);
    }

    /**
     * Verifies that manager is able to retrieve and delete SDXs.
     */
    @Test
    public void testGetSdxL2s() {
        Set<String> old = new HashSet<>();
        old.add(SDXL2_2);
        old.add(SDXL2);
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);
        Set<String> sdxl2 = manager.getSdxL2s();
        assertEquals(sdxl2, old);
        manager.deleteSdxL2(SDXL2);
        sdxl2 = manager.getSdxL2s();
        assertNotEquals(sdxl2, old);
    }

    /**
     * Verifies that manager can add Connection Points using different sets of VLANs and MACs.
     */
    @Test
    public void testAddSdxL2ConnectionPoint() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);
        manager.createSdxL2("test1");

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, one);

        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, three);

        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, six);

        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);

        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, nine);

        SdxL2ConnectionPoint ten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, ten);

        SdxL2ConnectionPoint fourteen = SdxL2ConnectionPoint.
                sdxl2ConnectionPoint("RO1", "of:0000000000000003/3", "1", "52:54:00:04:E5:9E");
        manager.addSdxL2ConnectionPoint("test1", fourteen);

        SdxL2ConnectionPoint fifteen = SdxL2ConnectionPoint.
                sdxl2ConnectionPoint("RO2", "of:0000000000000009/3", "1", "52:54:00:68:F7:D9");
        manager.addSdxL2ConnectionPoint("test1", fifteen);

        SdxL2ConnectionPoint two = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP2, VLANS1, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, two);

        SdxL2ConnectionPoint four = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, four);

        SdxL2ConnectionPoint five = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, five);

        SdxL2ConnectionPoint eight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM4", CP3, VLANS4, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, eight);

        SdxL2ConnectionPoint eleven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI3", CP5, VLANS1, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, eleven);

        SdxL2ConnectionPoint twelve = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI31", CP5, VLANS1, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, twelve);

        SdxL2ConnectionPoint thirteen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI31", CP5, VLANS8, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, thirteen);
    }

    /**
     * Verifies that manager can retrieve Connection Points using different sets of VLANs and MACs.
     */
    @Test
    public void testGetSdxL2ConnectionPoints() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        Set<String> allExt = Sets.newHashSet();
        Set<String> allExtBySdxl2 = Sets.newHashSet();
        Set<String> allExtBySdxl2Aux = Sets.newHashSet();

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, one);
        allExt.add(one.name());
        allExtBySdxl2.add(one.name());
        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, three);
        allExt.add(three.name());
        allExtBySdxl2.add(three.name());
        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, six);
        allExt.add(six.name());
        allExtBySdxl2Aux.add(six.name());
        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);
        allExt.add(seven.name());
        allExtBySdxl2.add(seven.name());
        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, nine);
        allExt.add(nine.name());
        allExtBySdxl2Aux.add(nine.name());
        SdxL2ConnectionPoint ten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, ten);
        allExt.add(ten.name());
        allExtBySdxl2.add(ten.name());

        Set<String> all = manager.getSdxL2ConnectionPoints(Optional.empty());
        Set<String> allBySdxl2 = manager.getSdxL2ConnectionPoints(Optional.of(SDXL2));
        Set<String> allBySdxl22 = manager.getSdxL2ConnectionPoints(Optional.of(SDXL2_2));

        assertEquals(allExt, all);
        assertNotEquals(allExtBySdxl2, all);
        assertNotEquals(allExtBySdxl2Aux, all);

        assertNotEquals(allExt, allBySdxl2);
        assertEquals(allExtBySdxl2, allBySdxl2);
        assertNotEquals(allExtBySdxl2Aux, allBySdxl2);

        assertNotEquals(allExt, allBySdxl22);
        assertNotEquals(allExtBySdxl2, allBySdxl22);
        assertEquals(allExtBySdxl2Aux, allBySdxl22);
    }

    /**
     * Verifies that manager can remove Connection Points using different sets of VLANs and MACs.
     */
    @Test
    public void testRemoveSdxL2ConnectionPoint() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, one);

        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, three);

        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, six);

        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);

        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, nine);

        SdxL2ConnectionPoint ten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, ten);

        manager.removeSdxL2ConnectionPoint(one.name());
        manager.removeSdxL2ConnectionPoint(six.name());
        manager.removeSdxL2ConnectionPoint(one.name());
        manager.removeSdxL2ConnectionPoint("ROM");
    }

    /**
     * Verifies that manager removes all CPs associated to a given SDX when this is removed.
     */
    @Test
    public void test2RemoveSdxL2s() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        Set<String> sdxL2CPs = Sets.newHashSet();
        Set<String> sdxl2CPsAux = Sets.newHashSet();

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        sdxL2CPs.add(one.name());
        manager.addSdxL2ConnectionPoint(SDXL2, one);

        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC1);
        sdxL2CPs.add(three.name());
        manager.addSdxL2ConnectionPoint(SDXL2, three);

        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC1);
        sdxl2CPsAux.add(six.name());
        manager.addSdxL2ConnectionPoint(SDXL2_2, six);

        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC1);
        sdxL2CPs.add(seven.name());
        manager.addSdxL2ConnectionPoint(SDXL2, seven);

        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC1);
        sdxl2CPsAux.add(nine.name());
        manager.addSdxL2ConnectionPoint(SDXL2_2, nine);

        SdxL2ConnectionPoint ten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC1);
        sdxL2CPs.add(ten.name());
        manager.addSdxL2ConnectionPoint(SDXL2, ten);

        manager.deleteSdxL2(SDXL2);

        assertEquals(sdxl2CPsAux, this.manager.getSdxL2ConnectionPoints(Optional.of(SDXL2_2)));
        assertEquals(sdxl2CPsAux, this.manager.getSdxL2ConnectionPoints(Optional.empty()));
        manager.deleteSdxL2(SDXL2_2);

        assertEquals(Collections.emptySet(), this.manager.getSdxL2ConnectionPoints(Optional.of(SDXL2)));
        assertEquals(Collections.emptySet(), this.manager.getSdxL2ConnectionPoints(Optional.of(SDXL2_2)));
    }

    /**
     * Assesses that manager retrieves the expected Connection Points.
     */
    @Test
    public void testGetSdxL2ConnectionPoint() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint two = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP2, VLANS1, CEMAC1);
        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC1);
        SdxL2ConnectionPoint four = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS1, CEMAC1);
        SdxL2ConnectionPoint five = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS2, CEMAC1);
        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC1);
        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC1);
        SdxL2ConnectionPoint eight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM4", CP3, VLANS4, CEMAC1);
        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC1);
        SdxL2ConnectionPoint ten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC1);
        SdxL2ConnectionPoint eleven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI3", CP5, VLANS1, CEMAC1);
        SdxL2ConnectionPoint twelve = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI31", CP5, VLANS1, CEMAC1);
        SdxL2ConnectionPoint thirteen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI31", CP5, VLANS8, CEMAC1);

        manager.addSdxL2ConnectionPoint(SDXL2, one);
        manager.addSdxL2ConnectionPoint(SDXL2, three);
        manager.addSdxL2ConnectionPoint(SDXL2_2, six);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);
        manager.addSdxL2ConnectionPoint(SDXL2_2, nine);
        manager.addSdxL2ConnectionPoint(SDXL2, ten);

        assertEquals(one, manager.getSdxL2ConnectionPoint(one.name()));
        assertNotEquals(two, manager.getSdxL2ConnectionPoint(two.name()));
        assertEquals(three, manager.getSdxL2ConnectionPoint(three.name()));
        assertNotEquals(four, manager.getSdxL2ConnectionPoint(four.name()));
        assertNotEquals(five, manager.getSdxL2ConnectionPoint(five.name()));
        assertEquals(six, manager.getSdxL2ConnectionPoint(six.name()));
        assertEquals(seven, manager.getSdxL2ConnectionPoint(seven.name()));
        assertEquals(nine, manager.getSdxL2ConnectionPoint(nine.name()));
        assertEquals(ten, manager.getSdxL2ConnectionPoint(ten.name()));

        assertNotEquals(eight, manager.getSdxL2ConnectionPoint(eight.name()));
        assertNotEquals(eleven, manager.getSdxL2ConnectionPoint(eleven.name()));
        assertNotEquals(twelve, manager.getSdxL2ConnectionPoint(twelve.name()));
        assertNotEquals(thirteen, manager.getSdxL2ConnectionPoint(thirteen.name()));
    }

    /**
     * Checks that manager is able to add Connection Points to Virtual Circuits.
     */
    @Test
    public void testAddVCChecks() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint two = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC2);
        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC3);
        SdxL2ConnectionPoint four = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC4);
        SdxL2ConnectionPoint five = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC5);
        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC6);
        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI23", CP6, VLANS6, CEMAC7);
        SdxL2ConnectionPoint eight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI24", CP7, VLANS5, CEMAC8);
        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI25", CP8, VLANS5, CEMAC8);

        manager.addSdxL2ConnectionPoint(SDXL2, one);
        manager.addSdxL2ConnectionPoint(SDXL2, two);
        manager.addSdxL2ConnectionPoint(SDXL2_2, three);
        manager.addSdxL2ConnectionPoint(SDXL2, four);
        manager.addSdxL2ConnectionPoint(SDXL2_2, five);
        manager.addSdxL2ConnectionPoint(SDXL2, six);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);
        manager.addSdxL2ConnectionPoint(SDXL2, eight);
        manager.addSdxL2ConnectionPoint(SDXL2, nine);

        manager.addVC(SDXL2, two.name(), six.name());
        manager.addVC(SDXL2, seven.name(), nine.name());
        manager.addVC(SDXL2, one.name(), two.name());
        manager.addVC(SDXL2, one.name(), four.name());
        manager.addVC(SDXL2, one.name(), six.name());
        manager.addVC(SDXL2, two.name(), four.name());
        manager.addVC(SDXL2, seven.name(), eight.name());

        exceptionAddVC.expect(IllegalStateException.class);
        manager.addVC(SDXL2, four.name() + "x", five.name());
        manager.addVC(SDXL2, one.name(), three.name());
        manager.addVC(SDXL2, one.name(), five.name());
        manager.addVC(SDXL2, two.name(), three.name());
        manager.addVC(SDXL2, two.name(), five.name());
        manager.addVC(SDXL2, three.name(), four.name());
        manager.addVC(SDXL2, three.name(), five.name());
        manager.addVC(SDXL2, three.name(), six.name());
        manager.addVC(SDXL2, four.name(), five.name());
    }

    /**
     * Checks that manager is able to remove Virtual Circuits properly.
     */
    @Test
    public void testRemoveVCChecks() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint two = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC2);
        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC3);
        SdxL2ConnectionPoint four = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC4);
        SdxL2ConnectionPoint five = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC5);
        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC6);
        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI23", CP6, VLANS6, CEMAC7);
        SdxL2ConnectionPoint eight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI24", CP7, VLANS5, CEMAC8);
        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI25", CP8, VLANS5, CEMAC8);

        manager.addSdxL2ConnectionPoint(SDXL2, one);
        manager.addSdxL2ConnectionPoint(SDXL2, two);
        manager.addSdxL2ConnectionPoint(SDXL2_2, three);
        manager.addSdxL2ConnectionPoint(SDXL2, four);
        manager.addSdxL2ConnectionPoint(SDXL2_2, five);
        manager.addSdxL2ConnectionPoint(SDXL2, six);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);
        manager.addSdxL2ConnectionPoint(SDXL2, eight);
        manager.addSdxL2ConnectionPoint(SDXL2, nine);

        manager.addVC(SDXL2, two.name(), six.name());
        manager.addVC(SDXL2, seven.name(), nine.name());

        String vc;
        vc = two.name().compareTo(six.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), six.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, six.name(), two.name());
        manager.removeVC(vc);

        vc = seven.name().compareTo(nine.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, seven.name(), nine.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, seven.name(), nine.name());
        manager.removeVC(vc);

        vc = one.name().compareTo(four.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, one.name(), four.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, four.name(), one.name());
        manager.removeVC(vc);
        vc = one.name().compareTo(five.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, one.name(), five.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, five.name(), one.name());
        manager.removeVC(vc);
        vc = one.name().compareTo(six.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, one.name(), six.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, six.name(), one.name());
        manager.removeVC(vc);
        vc = two.name().compareTo(three.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), three.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, three.name(), two.name());
        manager.removeVC(vc);
        vc = two.name().compareTo(four.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), four.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, four.name(), two.name());
        manager.removeVC(vc);
        vc = two.name().compareTo(five.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), five.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, five.name(), two.name());
        manager.removeVC(vc);
        vc = three.name().compareTo(four.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, three.name(), four.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, four.name(), three.name());
        manager.removeVC(vc);
        vc = three.name().compareTo(five.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, three.name(), five.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, five.name(), three.name());
        manager.removeVC(vc);
        vc = three.name().compareTo(six.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, three.name(), six.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, six.name(), three.name());
        manager.removeVC(vc);
        vc = four.name().compareTo(five.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, four.name(), five.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, five.name(), four.name());
        manager.removeVC(vc);
        vc = seven.name().compareTo(eight.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, seven.name(), eight.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, eight.name(), seven.name());
        manager.removeVC(vc);
        vc = seven.name().compareTo(nine.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2_2, seven.name(), nine.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, seven.name(), nine.name());
        manager.removeVC(vc);
        vc = seven.name().compareTo(nine.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2 + "x", seven.name(), nine.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, seven.name(), nine.name());
        manager.removeVC(vc);

        exceptionRemoveVC.expect(IllegalStateException.class);
        vc = one.name().compareTo(three.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, one.name(), three.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, three.name(), one.name());
        manager.removeVC(vc);
        manager.removeVC(":A");
        manager.removeVC("A:B");

        vc = four.name().compareTo(five.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, four.name() + "x", five.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, five.name(), four.name() + "x");
        manager.removeVC(vc);

        vc = one.name().compareTo(two.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, one.name(), two.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), one.name());
        manager.removeVC(vc);
    }

    /**
     * Assesses that SDX instances are not created with invalid characters or no name.
     */
    @Test
    public void testSdxL2NameChecks() {
        String sdxL2 = "";
        String sdxL2Aux = "test3,4";

        exceptionSdxL2Name.expect(NullPointerException.class);
        exceptionSdxL2Name.expectMessage("name cannot be null");
        manager.createSdxL2(null);

        manager.createSdxL2(sdxL2);

        exceptionSdxL2Name.expect(IllegalStateException.class);
        exceptionSdxL2Name.expectMessage("names cannot contain commas");
        manager.createSdxL2(sdxL2Aux);
    }

    /**
     * Assesses that SDX instances are not deleted when having invalid characters or no name.
     */
    @Test
    public void testDeleteSdxL2NameChecks() {
        String sdxL2 = "";
        exceptionDelSdxL2Name.expect(NullPointerException.class);
        exceptionDelSdxL2Name.expectMessage("name cannot be null");
        manager.deleteSdxL2(null);
        manager.createSdxL2(sdxL2);
        manager.deleteSdxL2(sdxL2);
    }

    /**
     * Tests proper cleaning of all SDX-L2 related information.
     */
    @Test
    public void testCleanSdxL2s() {
        manager.createSdxL2(SDXL2);
        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint two = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC2);
        manager.addSdxL2ConnectionPoint(SDXL2, one);
        manager.addSdxL2ConnectionPoint(SDXL2, two);
        manager.getSdxL2ConnectionPoint("ROM1");

        assertTrue(manager.getSdxL2s().contains(SDXL2));
        assertEquals(one, manager.getSdxL2ConnectionPoint(one.name()));
        assertEquals(two, manager.getSdxL2ConnectionPoint(two.name()));

        manager.cleanSdxL2();

        assertFalse(manager.getSdxL2s().contains(SDXL2));
        assertEquals(null, manager.getSdxL2ConnectionPoint(one.name()));
        assertEquals(null, manager.getSdxL2ConnectionPoint(two.name()));
    }

    /**
     * Verifies that the retrieved VC is in a correct state.
     * It specifically checks that it returns the expected name, that the VC
     * returned is null if it does not exist and that an exception is thrown when
     * a badly formatted VC is requested.
     */
    @Test
    public void testGetVC() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        SdxL2ConnectionPoint one = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint two = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM2", CP2, VLANS2, CEMAC2);
        SdxL2ConnectionPoint three = SdxL2ConnectionPoint.sdxl2ConnectionPoint("ROM3", CP1, VLANS7, CEMAC3);
        SdxL2ConnectionPoint four = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC4);
        SdxL2ConnectionPoint five = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC5);
        SdxL2ConnectionPoint six = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC6);
        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI23", CP6, VLANS6, CEMAC7);
        SdxL2ConnectionPoint eight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI24", CP7, VLANS5, CEMAC8);
        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI25", CP8, VLANS5, CEMAC8);

        String vc;
        VirtualCircuit expectedVC;
        VirtualCircuit actualVC;

        manager.addSdxL2ConnectionPoint(SDXL2, one);
        manager.addSdxL2ConnectionPoint(SDXL2, two);
        manager.addSdxL2ConnectionPoint(SDXL2_2, three);
        manager.addSdxL2ConnectionPoint(SDXL2, four);
        manager.addSdxL2ConnectionPoint(SDXL2_2, five);
        manager.addSdxL2ConnectionPoint(SDXL2, six);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);
        manager.addSdxL2ConnectionPoint(SDXL2, eight);
        manager.addSdxL2ConnectionPoint(SDXL2, nine);

        // VC created using the manager, check against manually generated
        manager.addVC(SDXL2, two.name(), six.name());
        vc = two.name().compareTo(six.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), six.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, six.name(), two.name());
        expectedVC = new VirtualCircuit(two, six);
        actualVC = manager.getVirtualCircuit(vc);
        assertEquals(expectedVC, actualVC);

        // VC not created, check that getVC returns null if VC does not exist
        vc = one.name().compareTo(two.name()) < 0 ?
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, one.name(), two.name()) :
                format(SdxL2VCManager.NAME_FORMAT, SDXL2, two.name(), one.name());
        expectedVC = new VirtualCircuit(one, two);
        actualVC = manager.getVirtualCircuit(vc);
        assertNotEquals(expectedVC, actualVC);
        assertNull(actualVC);

        // Testing illegal character
        exceptionGetVC.expect(IllegalStateException.class);
        manager.getVirtualCircuit(":A");
        manager.getVirtualCircuit("A:B");
    }
}
