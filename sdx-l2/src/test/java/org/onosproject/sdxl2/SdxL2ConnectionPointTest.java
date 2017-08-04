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

import com.google.common.testing.EqualsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.packet.VlanId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests SdxL2ConnectionPoint functionality.
 */
public class SdxL2ConnectionPointTest {

    private static final String CP1 = "of:00000000000001/1";
    private static final String CP2 = "of:00000000000002/1";
    private static final String CP3 = "of:00000000000003/1";
    private static final String CP4 = "of:00000000000003/2";
    private static final String CP5 = "-1";
    private static final String CP6 = "of:00000000000004/2";
    private static final String CP7 = "of:00000000000004/1";

    private static final String VLANS1 = "1,2,3,4";
    private static final String VLANS2 = "-1";
    private static final String VLANS3 = "1,2,3";
    private static final String VLANS4 = "2,2,2";
    private static final String VLANS5 = "2";
    private static final String VLANS6 = "-1";
    private static final String VLANS7 = "5";
    private static final String VLANS8 = "3,2,1";
    private static final String VLANS10 = "a";
    private static final String VLANS11 = "5196";
    private static final String VLANS12 = "2,-1,6";

    private static final String CEMAC1 = "52:40:00:12:44:01";
    private static final String CEMAC2 = "-1";
    private static final String CEMAC3 = "-1";
    private static final String CEMAC5 = "52:40:00:12:44:01";
    private static final String CEMAC6 = "52:40:00:12:44:01";
    private static final String CEMAC7 = "-1";

    /**
     * Exception expected to raise on bad format of CP name.
     */
    @Rule
    public ExpectedException exceptionconnectionpoint = ExpectedException.none();
    /**
     * Exception expected to raise on missing MAC.
     */
    @Rule
    public ExpectedException exceptionmac = ExpectedException.none();
    /**
     * Exception expected to raise on symbols on CP name.
     */
    @Rule
    public ExpectedException exceptionname = ExpectedException.none();
    /**
     * Exception expected to raise on VLANs not in allowed range.
     */
    @Rule
    public ExpectedException exceptionVlans = ExpectedException.none();

    /**
     * Prepares environment before starting testing.
     */
    @Before
    public void setUp() {
    }

    /**
     * Cleans up environment after finishing testing.
     */
    @After
    public void tearDown() {
    }

    /**
     * Ensures CP with MAC address is properly created (i.e. not null).
     */
    @Test
    public void testSdxL2ConnectionPoint1() {
        SdxL2ConnectionPoint scp1 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO1", CP1, VLANS1, CEMAC1);
        // scp1 != null
        new EqualsTester()
                .addEqualityGroup(scp1)
                .addEqualityGroup()
                .testEquals();
    }

    /**
     * Ensures CP with all VLANs is properly created (i.e. not null).
     */
    @Test
    public void testSdxL2ConnectionPoint2() {
        SdxL2ConnectionPoint scp2 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO2", CP2, VLANS2);
        // scp2 != null
        new EqualsTester()
                .addEqualityGroup(scp2)
                .addEqualityGroup()
                .testEquals();
    }

    /**
     * Ensures CP is properly created (i.e. not null).
     */
    @Test
    public void testSdxL2ConnectionPoint3() {
        SdxL2ConnectionPoint scp3 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI1", CP3, VLANS3);
        // scp3 != null
        new EqualsTester()
                .addEqualityGroup(scp3)
                .addEqualityGroup()
                .testEquals();
    }

    /**
     * Ensures CP with list of same VLANs is properly created (i.e. not null).
     */
    @Test
    public void testSdxL2ConnectionPoint4() {
        SdxL2ConnectionPoint scp4 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO3", CP4, VLANS4);
        // scp4 != null
        new EqualsTester()
                .addEqualityGroup(scp4)
                .addEqualityGroup()
                .testEquals();
    }

    /**
     * Asseses that CP with bad name is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint5() {
        exceptionconnectionpoint.expect(IllegalArgumentException.class);
        exceptionconnectionpoint.expectMessage("Connect point must be in \"deviceUri/portNumber\" format");
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI2", CP5, VLANS5, CEMAC5);
    }

    /**
     * Ensures CP with all VLANs and MAC address is properly created (i.e. not null).
     */
    @Test
    public void testSdxL2ConnectionPoint6() {
        SdxL2ConnectionPoint scp6 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("MI3", CP6, VLANS6, CEMAC6);
        // scp6 != null
        new EqualsTester()
                .addEqualityGroup(scp6)
                .addEqualityGroup()
                .testEquals();
    }

    /**
     * Ensures CP with bad input for MAC is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint7() {
        exceptionmac.expect(IllegalArgumentException.class);
        exceptionmac.expectMessage("Specified MAC Address must contain");
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("RO4", CP7, VLANS7, CEMAC7);
    }

    /**
     * Ensures CP with all VLANs and MAC address is properly created (i.e. not null).
     */
    @Test
    public void testSdxL2ConnectionPoint8() {
        SdxL2ConnectionPoint scp8 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("", CP4, VLANS2, CEMAC6);
        // scp8 != null
        new EqualsTester()
                .addEqualityGroup(scp8)
                .addEqualityGroup()
                .testEquals();
    }

    /**
     * Ensures CP with bad input for name is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint9a() {
        exceptionname.expect(IllegalStateException.class);
        exceptionname.expectMessage("Names cannot contain some special characters");
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("NA,1", CP1, VLANS1, CEMAC1);
    }

    /**
     * Ensures CP with bad input for name is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint9b() {
        exceptionname.expect(IllegalStateException.class);
        exceptionname.expectMessage("Names cannot contain some special characters");
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("{NA2}", CP2, VLANS2, CEMAC2);
    }

    /**
     * Ensures CP with bad input for name is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint9c() {
        exceptionname.expect(IllegalStateException.class);
        exceptionname.expectMessage("Names cannot contain some special characters");
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("NA3,elementId=50", CP3, VLANS3, CEMAC3);
    }

    /**
     * Ensures CP with bad input for VLAN is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint10() {
        exceptionVlans.expect(NumberFormatException.class);
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("VE1", CP1, VLANS10, CEMAC7);
    }

    /**
     * Ensures CP with VLAN in a non-allowed range is not created.
     * The exception blocks the flow of the program and no CP is created,
     */
    @Test
    public void testSdxL2ConnectionPoint11() {
        exceptionVlans.expect(IllegalArgumentException.class);
        exceptionVlans.expectMessage("value exceeds allowed maximum VLAN ID value (4095)");
        SdxL2ConnectionPoint.sdxl2ConnectionPoint("VE2", CP2, VLANS11, CEMAC6);
    }

    /**
     * Ensures CP using all VLANs and specific, repeated VLANs at the same time
     * do only consider the latter (non-repeated).
     */
    @Test
    public void testSdxL2ConnectionPoint12() {
        SdxL2ConnectionPoint scp12 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("AQ1", CP1, VLANS12, CEMAC5);
        List<VlanId> vlanIdList = new ArrayList<>();
        vlanIdList.add(VlanId.vlanId(Short.parseShort("2")));
        vlanIdList.add(VlanId.vlanId(Short.parseShort("6")));
        assertEquals(vlanIdList, scp12.vlanIds());
    }

    /**
     * Assesses that endpoints using same, but reversed VLANs, are not regarded as similar.
     */
    @Test
    public void testSdxL2ConnectionPointEquality() {
        SdxL2ConnectionPoint scp1 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP1, VLANS1, CEMAC1);
        SdxL2ConnectionPoint scp2 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("FI1", CP1, VLANS1, CEMAC6);
        SdxL2ConnectionPoint scp3 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("GE4", CP2, VLANS3, CEMAC6);
        SdxL2ConnectionPoint scp4 = SdxL2ConnectionPoint.sdxl2ConnectionPoint("GE4", CP2, VLANS8, CEMAC6);
        // scp1 == scp2, but those are different from scp3 and scp4
        new EqualsTester()
                .addEqualityGroup(scp1, scp2)
                .addEqualityGroup(scp3)
                .addEqualityGroup(scp4)
                .testEquals();
    }
}
