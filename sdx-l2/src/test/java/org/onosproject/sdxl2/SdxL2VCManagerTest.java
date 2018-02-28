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
import org.onosproject.core.ApplicationId;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;


/**
 * Unit tests for SDX-L2 VC Manager.
 */
public class SdxL2VCManagerTest {

    private static final String SDXL2_1 = "sdxl2_test1";
    private static final String CP1 = "of:00000000000001/1";
    private static final String CP2 = "of:00000000000002/1";
    private static final String VLANS1 = "2,3,4";
    private static final String VLANS2 = "4,5,6";
    private static final String CEMAC1 = "52:40:00:12:44:01";
    private static final String CEMAC2 = "51:12:11:00:23:01";
    private static final String CP3 = "of:00000000000002/2";
    private static final String VLANS3 = "8,9,10";
    private static final String CEMAC3 = "52:12:11:00:23:01";
    private static final String SDXL2_2 = "sdxl2_test2";
    private static final String CP5 = "of:00000000000002/4";
    private static final String VLANS5 = "100";
    private static final String CEMAC5 = "52:12:11:00:23:11";
    private static final String CP6 = "of:00000000000004/4";
    private static final String VLANS6 = "1";
    private static final String CEMAC6 = "52:12:11:a0:23:11";
    private static final String CP7 = "of:0000000000000a/4";
    private static final String VLANS7 = "1";
    private static final String CEMAC7 = "52:12:21:00:25:11";
    private static final String CP8 = "of:00000000000009/4";
    private static final String VLANS8 = "111";
    private static final String CEMAC8 = "52:12:14:a0:23:11";
    private static final String CP9 = "of:0000000000000a/4";
    private static final String VLANS9 = "1";
    private static final String CEMAC9 = "52:12:21:00:28:11";
    private static final String CP10 = "of:00000000000009/4";
    private static final String VLANS10 = "1";
    private static final String CEMAC10 = "52:12:14:aa:23:11";
    private static final ApplicationId APPID = TestApplicationId.create("foo");
    /**
     * Exception expected to raise when creating VC with CPs using both same MAC.
     */
    @Rule
    public ExpectedException exceptionAddVC = ExpectedException.none();
    /**
     * Exception expected to raise when removing VC with CPs using both same MAC.
     */
    @Rule
    public ExpectedException exceptionRemoveVC = ExpectedException.none();
    private SdxL2MacVCManager manager;

    /**
     * Prepare environment before starting testing VCs.
     */
    @Before
    public void setUp() {

        SdxL2DistributedStore store = new SdxL2DistributedStore();
        store.initForTest();
        manager = new SdxL2MacVCManager(
                APPID, store, new IntentServiceTest());
        MockIdGenerator.cleanBind();
    }

    /**
     * Clean up environment after finishing testing VCs.
     */
    @After
    public void tearDown() {
        MockIdGenerator.unbind();
    }

    /**
     * Ensure that intents generated when reversing endpoints are different.
     */
    @Test
    public void testGeneratedIntentsHaveDifferentKey() {
        SdxL2ConnectionPoint cpOne = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpOneAux = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC1);

        SdxL2ConnectionPoint cpTwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC1);
        SdxL2ConnectionPoint cpTwoAux = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);

        Key key1 = manager.generateIntentKey(SDXL2_1, cpOne, cpTwo, "1");
        Key key2 = manager.generateIntentKey(SDXL2_1, cpOneAux, cpTwoAux, "1");
        assertNotEquals(key1, key2);

        Key key3 = manager.generateIntentKey(SDXL2_1, cpOne, cpTwoAux, "1");
        Key key4 = manager.generateIntentKey(SDXL2_1, cpOneAux, cpTwo, "1");
        assertNotEquals(key3, key4);
    }

    /**
     * Tests proper creation of VCs only when endpoints have different MAC address.
     */
    @Test
    public void testAddVCChecks() {
        SdxL2ConnectionPoint cpFive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpSix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC2);
        manager.addVC(SDXL2_1, cpFive, cpSix);

        SdxL2ConnectionPoint cpSeven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC2);
        SdxL2ConnectionPoint cpEight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        manager.addVC(SDXL2_1, cpSeven, cpEight);

        SdxL2ConnectionPoint cpNine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpTen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST3", CP3, VLANS3, CEMAC3);
        manager.addVC(SDXL2_1, cpNine, cpTen);

        exceptionAddVC.expect(IllegalStateException.class);
        SdxL2ConnectionPoint cpOne = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpTwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC1);
        manager.addVC(SDXL2_1, cpOne, cpTwo);
    }

    /**
     * Tests proper removal of VCs only when endpoints have different MAC address.
     */
    @Test
    public void testremoveVCChecks() {
        SdxL2ConnectionPoint cpOne = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpTwo = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC1);

        SdxL2ConnectionPoint cpFive = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpSix = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC2);

        SdxL2ConnectionPoint cpSeven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST2", CP2, VLANS2, CEMAC2);
        SdxL2ConnectionPoint cpEight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);

        SdxL2ConnectionPoint cpNine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpTen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST3", CP3, VLANS3, CEMAC3);

        manager.addVC(SDXL2_1, cpFive, cpSix);
        manager.removeVC(cpFive, cpSix);
        manager.removeVC(cpSeven, cpEight);
        manager.removeVC(cpNine, cpTen);

        exceptionRemoveVC.expect(IllegalStateException.class);
        manager.removeVC(cpOne, cpTwo);
    }

    /**
     * Verifies that only VCs created properly (here, during set up) are retrieved.
     */
    @Test
    public void testGetVC() {
        connectionSetup();

        Iterator<SdxL2ConnectionPoint> lhs = setupLhsCPs().iterator();
        Iterator<SdxL2ConnectionPoint> rhs = setupRhsCPs().iterator();
        String vc;
        SdxL2ConnectionPoint one;
        SdxL2ConnectionPoint two;
        while (lhs.hasNext()) {
            one = lhs.next();
            two = rhs.next();
            vc = one.toString().compareTo(two.toString()) < 0 ?
                    format(SdxL2VCManager.SDXL2_CPS_FORMAT, one, two) :
                    format(SdxL2VCManager.SDXL2_CPS_FORMAT, two, one);
            assertEquals(vc, manager.getVC(one, two));
        }

        SdxL2ConnectionPoint cpLeft = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint cpRight = SdxL2ConnectionPoint.sdxl2ConnectionPoint("TEST1", CP3, VLANS3, CEMAC1);
        assertNull(manager.getVC(cpLeft, cpRight));
    }

    /**
     * Verifies that only VCs created properly (here, during set up) are retrieved.
     */
    @Test
    public void testGetVCs() {
        connectionSetup();
        Iterator<SdxL2ConnectionPoint> lhs = setupLhsCPs().iterator();
        Iterator<SdxL2ConnectionPoint> rhs = setupRhsCPs().iterator();
        Set<String> expectedVCs = Sets.newHashSet();
        String vc;
        String lhsName;
        String rhsName;
        while (lhs.hasNext()) {
            lhsName = lhs.next().name();
            rhsName = rhs.next().name();
            vc = lhsName.compareTo(rhsName) < 0 ?
                    format(SdxL2VCManager.NAME_FORMAT, SDXL2_2, lhsName, rhsName) :
                    format(SdxL2VCManager.NAME_FORMAT, SDXL2_2, rhsName, lhsName);
            expectedVCs.add(vc);
        }
        Set<String> vcs = manager.getVCs(Optional.of(SDXL2_2));
        assertEquals(expectedVCs, vcs);
        vcs = manager.getVCs(Optional.of(SDXL2_1));
        assertEquals(Collections.emptySet(), vcs);
        vcs = manager.getVCs(Optional.empty());
        assertEquals(expectedVCs, vcs);
    }

    /**
     * Defines the left-hand side endpoints, each with a specific VLAN.
     *
     * @return list of SdxL2ConnectionPoint objects
     */
    private List<SdxL2ConnectionPoint> setupLhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<>();
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

    /**
     * Defines the right-hand side endpoints, each with a specific VLAN.
     *
     * @return list of SdxL2ConnectionPoint objects
     */
    private List<SdxL2ConnectionPoint> setupRhsCPs() {
        List<SdxL2ConnectionPoint> cps = new ArrayList<>();
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

    /**
     * Sets the connection up by adding CPs for left and right hand side of the VCs.
     */
    private void connectionSetup() {
        Iterator<SdxL2ConnectionPoint> lhs = setupLhsCPs().iterator();
        Iterator<SdxL2ConnectionPoint> rhs = setupRhsCPs().iterator();
        while (lhs.hasNext()) {
            manager.addVC(SDXL2_2, lhs.next(), rhs.next());
        }
    }
}
