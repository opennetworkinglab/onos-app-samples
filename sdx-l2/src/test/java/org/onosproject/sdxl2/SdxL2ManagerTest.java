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

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


/**
 * Tests SdxL2Manager functionality.
 */
public class SdxL2ManagerTest {

    protected SdxL2Manager manager;

    @Before
    public void setUp() {
        manager = new SdxL2Manager();
        manager.appId = new TestApplicationId("sdxl2-test");
        SdxL2DistributedStore store = new SdxL2DistributedStore();
        store.initForTest();
        manager.sdxL2Store = store;
    }

    @After
    public void tearDown() {
    }

    public static final String SDXL2 = "test";
    public static final String SDXL2_2 = "test2";


    @Test
    public void testCreateSdxL2s() {

        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);
        manager.createSdxL2(SDXL2);
    }

    @Test
    public void testRemoveSdxL2s() {
        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);
        manager.deleteSdxL2(SDXL2);
        manager.deleteSdxL2(SDXL2_2);
        manager.deleteSdxL2(SDXL2);
    }

    @Test
    public void testGetSdxL2s() {
        Set<String> old = new HashSet<String>();
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

    public static final String CP1 = "of:00000000000001/1";
    public static final String CP2 = "of:00000000000002/1";
    public static final String CP3 = "of:00000000000003/1";
    public static final String CP4 = "of:00000000000003/2";
    public static final String CP5 = "of:00000000000004/2";

    public static final String VLANS1 = "1,2,3,4";
    public static final String VLANS2 = "-1";
    public static final String VLANS3 = "1,2,3";
    public static final String VLANS4 = "2,2,2";
    public static final String VLANS7 = "5";
    public static final String VLANS8 = "3,2,1";

    public static final String CEMAC1 = "52:40:00:12:44:01";

    @Test
    public void testaddSdxL2ConnectionPoint() {

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

        SdxL2ConnectionPoint fourteen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO1",
                "of:0000000000000003/3", "1", "52:54:00:04:E5:9E");
        manager.addSdxL2ConnectionPoint("test1", fourteen);

        SdxL2ConnectionPoint fifteen = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO2",
                "of:0000000000000009/3", "1", "52:54:00:68:F7:D9");
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

    @Test
    public void testgetSdxL2ConnectionPoints() {

        manager.createSdxL2(SDXL2);
        manager.createSdxL2(SDXL2_2);

        Set<String> allExt = Sets.newHashSet();
        Set<String> allExtBySdxl2 = Sets.newHashSet();
        Set<String> allExtBySdxl22 = Sets.newHashSet();

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
        allExtBySdxl22.add(six.name());
        SdxL2ConnectionPoint seven = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP3, VLANS3, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, seven);
        allExt.add(seven.name());
        allExtBySdxl2.add(seven.name());
        SdxL2ConnectionPoint nine = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP4, VLANS8, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2_2, nine);
        allExt.add(nine.name());
        allExtBySdxl22.add(nine.name());
        SdxL2ConnectionPoint ten = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP5, VLANS2, CEMAC1);
        manager.addSdxL2ConnectionPoint(SDXL2, ten);
        allExt.add(ten.name());
        allExtBySdxl2.add(ten.name());

        Set<String> all = manager.getSdxL2ConnectionPoints(Optional.ofNullable(null));
        Set<String> allBySdxl2 = manager.getSdxL2ConnectionPoints(Optional.of(SDXL2));
        Set<String> allBySdxl22 = manager.getSdxL2ConnectionPoints(Optional.of(SDXL2_2));

        assertEquals(allExt, all);
        assertNotEquals(allExtBySdxl2, all);
        assertNotEquals(allExtBySdxl22, all);

        assertNotEquals(allExt, allBySdxl2);
        assertEquals(allExtBySdxl2, allBySdxl2);
        assertNotEquals(allExtBySdxl22, allBySdxl2);

        assertNotEquals(allExt, allBySdxl22);
        assertNotEquals(allExtBySdxl2, allBySdxl22);
        assertEquals(allExtBySdxl22, allBySdxl22);

    }

    @Test
    public void testremoveSdxL2ConnectionPoint() {

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

}
