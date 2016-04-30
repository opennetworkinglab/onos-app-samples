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
import org.onosproject.TestApplicationId;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


/**
 * Test network manager.
 */
public class SdxL2ManagerTest {

    protected SdxL2Manager manager;

    @Before
    public void setUp() {
        manager = new SdxL2Manager();
        manager.appId = new TestApplicationId("sdxl2-test");
        manager.sdxL2Store = new SdxL2TestStore();
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

}
