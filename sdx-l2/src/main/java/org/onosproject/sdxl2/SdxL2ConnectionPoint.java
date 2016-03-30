/*
 * Copyright 2016 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.*;

/**
 * Sdx-l2 connection point expressed as composition of a:
 * ConnectPoint; Set of Vlanid; Customer edge MAC address (optional).
 */
public class SdxL2ConnectionPoint {

    private String name;
    private final ConnectPoint cPoint;
    private final List<VlanId> vlanIds;
    private final MacAddress ceMac;

    /**
     * Creates a new SDXL2 connection point.
     *
     * @param name connection point name
     * @param cPoint connection point
     * @param vlans the customer edge vlans
     * @param ceMac the customer edge router mac address
     */
    public SdxL2ConnectionPoint(String name, ConnectPoint cPoint, List<VlanId> vlans, MacAddress ceMac) {
        this.name = name;
        this.cPoint = cPoint;
        this.vlanIds = vlans;
        this.ceMac = ceMac;
    }

    /**
     * Return the name of SDXL2 connection point.
     *
     * @return a string representing the name of connection point
     */
    public String name() {
        return name;
    }

    /**
     * Returns the connection point.
     *
     * @return connection point object
     */
    public ConnectPoint connectPoint() {
        return cPoint;
    }

    /**
     * Returns the set of vlans that are used by the customer edge.
     *
     * @return a set of vlan ids
     */
    public List<VlanId> vlanIds() {
        return vlanIds;
    }

    /**
     * Returns the customer edge mac address.
     *
     * @return a mac address object
     */
    public MacAddress macAddress() {
        return ceMac;
    }

    /**
     * Parse a device connect point from a string, set of vlans from a string
     * and mac from a string.
     * The connect point should be in the format "deviceUri/portNumber".
     * The vlans should be in the format "vlan1,vlan2,vlan3"
     * The mac address should be in hex
     *
     * @param name name of the SDXL2 cp
     * @param connectPoint connection point to parse
     * @param vlans vlanIds to parse
     * @param mac mac address to parse
     * @return a ConnectPoint based on the information in the string.
     *
     */
    public static SdxL2ConnectionPoint
    sdxl2ConnectionPoint(String name, String connectPoint, String vlans, String mac) {
        checkNotNull(connectPoint);
        checkNotNull(vlans);
        checkState(!(name.contains(",") ||
                name.contains("-") ||
                name.contains("vlanid=") ||
                name.contains("ConnectPoint{") ||
                name.contains("elementId=") ||
                name.contains("portNumber=") ||
                name.contains("{") ||
                name.contains("}") ||
                name.contains("|")), "Names cannot contain some special characters");
        checkNotNull(mac);
        ConnectPoint connectionPoint = ConnectPoint.deviceConnectPoint(connectPoint);
        String[] splitted = vlans.split(",");
        checkArgument(splitted.length != 0, "At least '-1' or '1' as value");
        List<VlanId> vlanslist = new ArrayList<>();
        for (String vlan : splitted) {
            if (!vlanslist.contains(VlanId.vlanId(Short.parseShort(vlan))) &&
                    Short.parseShort(vlan) != -1 &&
                    Short.parseShort(vlan) != 1) {
                vlanslist.add(VlanId.vlanId(Short.parseShort(vlan)));
            }
        }
        MacAddress macAddress = MacAddress.valueOf(mac);
        return new SdxL2ConnectionPoint(name, connectionPoint, vlanslist, macAddress);
    }

    /**
     * Parse a device connect point from a string and set of vlans from a string.
     * The connect point should be in the format "deviceUri/portNumber".
     * The vlans should be in the format "vlan1,vlan2,vlan3"
     *
     * @param name name of the SDXL2 cp
     * @param connectPoint connection point to parse
     * @param vlans vlanIds to parse
     * @return a ConnectPoint based on the information in the string.
     *
     */
    public static SdxL2ConnectionPoint sdxl2ConnectionPoint(String name, String connectPoint, String vlans) {
        checkNotNull(connectPoint);
        checkNotNull(vlans);
        checkState(!(name.contains(",") ||
                name.contains("-") ||
                name.contains("vlanid=") ||
                name.contains("ConnectPoint{") ||
                name.contains("elementId=") ||
                name.contains("portNumber=") ||
                name.contains("{") ||
                name.contains("}") ||
                name.contains("|")), "Names cannot contain some special characters");
        ConnectPoint connectionPoint = ConnectPoint.deviceConnectPoint(connectPoint);
        String[] splitted = vlans.split(",");
        checkArgument(splitted.length != 0, "At least '-1' or '1' as value");
        List<VlanId> vlanslist = new ArrayList<>();
        for (String vlan : splitted) {
            if (!vlanslist.contains(VlanId.vlanId(Short.parseShort(vlan))) &&
                    Short.parseShort(vlan) != -1 &&
                    Short.parseShort(vlan) != 1) {
                vlanslist.add(VlanId.vlanId(Short.parseShort(vlan)));
            }
        }
        MacAddress macAddress = MacAddress.ZERO;
        return new SdxL2ConnectionPoint(name, connectionPoint, vlanslist, macAddress);
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
