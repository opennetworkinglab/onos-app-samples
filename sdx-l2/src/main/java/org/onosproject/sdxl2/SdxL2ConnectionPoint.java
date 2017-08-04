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

import com.google.common.base.MoreObjects;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

/**
 * SDX-L2 Connection Point expressed as composition of a:
 * Connect Point; set of VLAN ids; MAC address (optional).
 */
public class SdxL2ConnectionPoint {

    private final ConnectPoint cPoint;
    private final List<VlanId> vlanIds;
    private final MacAddress ceMac;
    private String name;

    /**
     * Creates a new SDX-L2 Connection Point.
     *
     * @param name SDX-L2 connection point name
     * @param cPoint connect point
     * @param vlans the customer edge VLANs
     * @param ceMac the customer edge router MAC address
     */
    public SdxL2ConnectionPoint(String name, ConnectPoint cPoint, List<VlanId> vlans, MacAddress ceMac) {
        this.name = name;
        this.cPoint = cPoint;
        this.vlanIds = vlans;
        this.ceMac = ceMac;
    }

    /**
     * Parses a device Connection Point from a string, set of VLANs
     * from a string and MAC from a string.
     * The connect point should be in the format "deviceUri/portNumber".
     * The vlans should be in the format "vlan1,vlan2,vlan3"
     * The mac address should be in hex
     *
     * @param name name of the SDX-L2 Connection Point
     * @param connectPoint Connection Point to parse
     * @param vlans VLAN IDs to parse
     * @param mac MAC address to parse
     * @return a Connection Point based on the information in the string
     *
     */
    public static SdxL2ConnectionPoint sdxl2ConnectionPoint(
            String name, String connectPoint, String vlans, String mac) {
        checkNotNull(connectPoint);
        enforceNameFormat(name);
        ConnectPoint connectionPoint = ConnectPoint.deviceConnectPoint(connectPoint);
        List<VlanId> vlansList = enforceVlans(vlans);
        MacAddress macAddress = MacAddress.ZERO;
        if (mac != null) {
            macAddress = MacAddress.valueOf(mac);
        }
        return new SdxL2ConnectionPoint(name, connectionPoint, vlansList, macAddress);
    }

    /**
     * Parses a device Connection Point from a string and set of
     * VLANs from a string.
     * The Connection Point should be in the format "deviceUri/portNumber".
     * The VLANs should be in the format "vlan1,vlan2,vlan3"
     *
     * @param name name of the SDX-L2 CP
     * @param connectPoint Connection Point to parse
     * @param vlans VLAN IDs to parse
     * @return a Connection Point based on the information in the string
     *
     */
    public static SdxL2ConnectionPoint sdxl2ConnectionPoint(
            String name, String connectPoint, String vlans) {
        return sdxl2ConnectionPoint(name, connectPoint, vlans, null);
    }

    /**
     * Enforces proper format on the name of the Connection Point.
     *
     * @param name name of the SDX-L2 Connection Point
     */
    private static void enforceNameFormat(String name) {
        checkState(!(name.contains(",") ||
                name.contains("-") ||
                name.contains("vlanid=") ||
                name.contains("ConnectPoint{") ||
                name.contains("elementId=") ||
                name.contains("portNumber=") ||
                name.contains("{") ||
                name.contains("}") ||
                name.contains("|")), "Names cannot contain some special characters");
    }

    /**
     * Enforces proper format on the requested VLANs.
     *
     * @param vlans VLANs expressed explicitly, as a range or in combination
     * @return a list of VLANs to be added
     */
    private static List<VlanId> enforceVlans(String vlans) {
        String[] splitted = parseVlans(vlans);
        List<VlanId> vlansList = new ArrayList<>();
        for (String vlan : splitted) {
            short vlanNumber = Short.parseShort(vlan);
            if (!vlansList.contains(VlanId.vlanId(vlanNumber)) &&
                    Short.parseShort(vlan) != -1 &&
                    Short.parseShort(vlan) != 1 &&
                    Short.parseShort(vlan) >= 0 &&
                    Short.parseShort(vlan) != 4095) {
                vlansList.add(VlanId.vlanId(vlanNumber));
            }
        }
        return vlansList;
    }

    /**
     * Parses the VLANs requested by the user.
     *
     * @param vlans VLANs expressed explicitly, as a range or in combination
     * @return an array of VLANs to add
     */
    private static String[] parseVlans(String vlans) {
        if (vlans == null) {
            vlans = "-1";
        }
        ArrayList<String> vlanRange = new ArrayList<>();
        String[] splittedVlans;
        String commaSeparator = ",";
        if (vlans.contains(commaSeparator)) {
            splittedVlans = vlans.split(commaSeparator);
            for (String vlan : splittedVlans) {
                vlanRange.addAll(generateNumberRange(vlan));
            }
        } else {
            vlanRange.addAll(generateNumberRange(vlans));
        }
        splittedVlans = new String[vlanRange.size()];
        splittedVlans = vlanRange.toArray(splittedVlans);
        return splittedVlans;
    }

    /**
     * Generates a range of numbers, given a string of type "X-Y" ("%d-%d").
     *
     * @param range range of numbers to compute
     * @return a list with numbers between "X" and "Y" (inclusive)
     */
    private static ArrayList<String> generateNumberRange(String range) {
        ArrayList<String> parsedNumbers = new ArrayList<>();
        Pattern p = Pattern.compile("(\\d+)-(\\d+)");
        Matcher m = p.matcher(range);
        if (m.matches()) {
                int start = Integer.parseInt(m.group(1));
                int end = Integer.parseInt(m.group(2));
                int min = Math.min(start, end);
                int max = Math.max(start, end);
                for (int v = min; v <= max; v++) {
                    parsedNumbers.add(Integer.toString(v));
                }
        } else {
            parsedNumbers.add(range);
        }
        return parsedNumbers;
    }

    /**
     * Returns the name of SDX-L2 Connection Point.
     *
     * @return a string representing the name of Connection Point
     */
    public String name() {
        return name;
    }

    /**
     * Returns the Connection Point.
     *
     * @return Connection Point object
     */
    public ConnectPoint connectPoint() {
        return cPoint;
    }

    /**
     * Returns the set of VLANs that are used by the customer edge.
     *
     * @return a set of VLAN ids
     */
    public List<VlanId> vlanIds() {
        return vlanIds;
    }

    /**
     * Returns the customer edge MAC address.
     *
     * @return a MAC address object
     */
    public MacAddress macAddress() {
        return ceMac;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, cPoint, vlanIds, ceMac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SdxL2ConnectionPoint) {
            final SdxL2ConnectionPoint other = (SdxL2ConnectionPoint) obj;
            return  Objects.equals(this.name, other.name) &&
                    Objects.equals(this.cPoint, other.cPoint) &&
                    Objects.equals(this.vlanIds, other.vlanIds) &&
                    Objects.equals(this.ceMac, other.ceMac);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("connectionPoint", cPoint)
                .add("vlanIds", vlanIds)
                .add("ceMac", ceMac)
                .toString();
    }
}
