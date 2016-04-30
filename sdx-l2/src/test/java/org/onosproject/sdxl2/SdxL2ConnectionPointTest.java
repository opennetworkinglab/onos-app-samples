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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.packet.VlanId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test SdxL2ConnectionPoint functionalities.
 */
public class SdxL2ConnectionPointTest {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    public static final String CP1 = "of:00000000000001/1";
    public static final String CP2 = "of:00000000000002/1";
    public static final String CP3 = "of:00000000000003/1";
    public static final String CP4 = "of:00000000000003/2";
    public static final String CP5 = "-1";
    public static final String CP6 = "of:00000000000004/2";
    public static final String CP7 = "of:00000000000004/1";

    public static final String VLANS1 = "1,2,3,4";
    public static final String VLANS2 = "-1";
    public static final String VLANS3 = "1,2,3";
    public static final String VLANS4 = "2,2,2";
    public static final String VLANS5 = "2";
    public static final String VLANS6 = "-1";
    public static final String VLANS7 = "5";
    public static final String VLANS8 = "3,2,1";
    public static final String VLANS10 = "a";
    public static final String VLANS11 = "5196";
    public static final String VLANS12 = "2,-1,6";

    public static final String CEMAC1 = "52:40:00:12:44:01";
    public static final String CEMAC2 = "-1";
    public static final String CEMAC3 = "-1";
    public static final String CEMAC4 = "-1";
    public static final String CEMAC5 = "52:40:00:12:44:01";
    public static final String CEMAC6 = "52:40:00:12:44:01";
    public static final String CEMAC7 = "-1";

    @Test
    public void testSdxL2ConnectionPoint1() {
        SdxL2ConnectionPoint scp1 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO1", CP1, VLANS1, CEMAC1);
    }

    @Test
    public void testSdxL2ConnectionPoint2() {
        SdxL2ConnectionPoint scp2 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO2", CP2, VLANS2);
    }

    @Test
    public void testSdxL2ConnectionPoint3() {
        SdxL2ConnectionPoint scp3 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI1", CP3, VLANS3);
    }

    @Test
    public void testSdxL2ConnectionPoint4() {
        SdxL2ConnectionPoint scp4 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO3", CP4, VLANS4);
    }

    @Rule
    public ExpectedException exceptionconnectionpoint = ExpectedException.none();

    @Test
    public void testSdxL2ConnectionPoint5() {
        exceptionconnectionpoint.expect(IllegalArgumentException.class);
        exceptionconnectionpoint.expectMessage("Connect point must be in \"deviceUri/portNumber\" format");
        SdxL2ConnectionPoint scp5 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI2", CP5, VLANS5, CEMAC5);
    }

    @Test
    public void testSdxL2ConnectionPoint6() {
        SdxL2ConnectionPoint scp6 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP6, VLANS6, CEMAC6);

    }

    @Rule
    public ExpectedException exceptionmac = ExpectedException.none();

    @Test
    public void testSdxL2ConnectionPoint7() {
        exceptionmac.expect(IllegalArgumentException.class);
        exceptionmac.expectMessage("Specified MAC Address must contain");
        SdxL2ConnectionPoint scp7 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO4", CP7, VLANS7, CEMAC7);
    }

    @Test
    public void testSdxL2ConnectionPoint8() {
        SdxL2ConnectionPoint scp8 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("", CP4, VLANS2, CEMAC6);
    }

    @Rule
    public ExpectedException exceptionname = ExpectedException.none();

    @Test
    public void testSdxL2ConnectionPoint9() {
        exceptionname.expect(IllegalStateException.class);
        exceptionname.expectMessage("Names cannot contain some special characters");
        SdxL2ConnectionPoint scp9a = SdxL2ConnectionPoint.sdxl2ConnectionPoint("NA,1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint scp9b = SdxL2ConnectionPoint.sdxl2ConnectionPoint("{NA2}", CP2, VLANS2, CEMAC2);
        SdxL2ConnectionPoint scp9c = SdxL2ConnectionPoint.sdxl2ConnectionPoint("NA3,elementId=50", CP3, VLANS3, CEMAC3);
    }

    @Rule
    public ExpectedException exceptionvlans = ExpectedException.none();

    @Test
    public void testSdxL2ConnectionPoint10() {
        exceptionvlans.expect(NumberFormatException.class);
        SdxL2ConnectionPoint scp10 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("VE1", CP1, VLANS10, CEMAC7);
    }

    @Test
    public void testSdxL2ConnectionPoint11() {
        exceptionvlans.expect(IllegalArgumentException.class);
        exceptionvlans.expectMessage("value exceeds allowed maximum VLAN ID value (4095)");
        SdxL2ConnectionPoint scp11 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("VE2", CP2, VLANS11, CEMAC6);
    }

    @Test
    public void testSdxL2ConnectionPoint12() {
        SdxL2ConnectionPoint scp12 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("AQ1", CP1, VLANS12, CEMAC5);
        List<VlanId> vlanIdList = new ArrayList<>();
        vlanIdList.add(VlanId.vlanId(Short.parseShort("2")));
        vlanIdList.add(VlanId.vlanId(Short.parseShort("6")));
        assertEquals(vlanIdList, scp12.vlanIds());
    }

    @Test
    public void testSdxL2ConnectionPointEquality() {
        SdxL2ConnectionPoint scp1 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint scp2 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP1, VLANS1, CEMAC6);
        assertEquals(scp1, scp2);
        SdxL2ConnectionPoint scp3 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("GE4", CP2, VLANS3, CEMAC6);
        assertNotEquals(scp1, scp3);
        SdxL2ConnectionPoint scp4 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("GE4", CP2, VLANS8, CEMAC6);
        assertNotEquals(scp3, scp4);
    }

}
