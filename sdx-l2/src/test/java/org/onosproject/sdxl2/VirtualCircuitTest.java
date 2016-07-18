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


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests VirtualCircuit functionalities.
 */
public class VirtualCircuitTest {


    public static final String CP1 = "of:00000000000001/1";
    public static final String CP2 = "of:00000000000002/1";
    public static final String VLANS1 = "1,2,3,4";
    public static final String VLANS3 = "1,2,3";
    public static final String CEMAC1 = "52:40:00:12:44:01";
    public static final String CEMAC6 = "52:40:00:12:44:02";

    /*
     * Tests VC with different VLANs.
     */
    @Test
    public void testVC1() {
        SdxL2ConnectionPoint scp1 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint scp2 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("GE4", CP2, VLANS3, CEMAC6);
        VirtualCircuit vc1 = new VirtualCircuit(scp1, scp2);
    }

    /*
    Tests creating VC with the same VLANs.
    */
    @Test
    public void testVCEquality() {
        SdxL2ConnectionPoint scp1 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint scp2 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("GE4", CP2, VLANS3, CEMAC6);
        VirtualCircuit vc1 = new VirtualCircuit(scp1, scp2);
        VirtualCircuit vc2 = new VirtualCircuit(scp2, scp1);
        assertEquals(vc1, vc2);
        SdxL2ConnectionPoint scp3 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI2", CP1, VLANS1, CEMAC1);
        VirtualCircuit vc3 = new VirtualCircuit(scp1, scp3);
        assertNotEquals(vc1, vc3);
    }

}
