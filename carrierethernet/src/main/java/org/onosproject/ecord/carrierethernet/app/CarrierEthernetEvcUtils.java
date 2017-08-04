/*
 * Copyright 2017 Open Networking Foundation
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
package org.onosproject.ecord.carrierethernet.app;

import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection.MAX_NUM_UNI;

/**
 * Utilities to build EthernetVirtualConnections.
 */
public final class CarrierEthernetEvcUtils {

    // no instantiation
    private CarrierEthernetEvcUtils() {
    }

    /**
     * Return the CE-VLAN ID for the CE evc based on the CLI-supplied argument.
     *
     * @param argCeVlanId vlanID
     * @return CE-VLAN ID for the CE evc
     */
    public static VlanId generateCeVlanId(short argCeVlanId) {
        return ((argCeVlanId == -1) ? null : VlanId.vlanId(argCeVlanId));
    }

    /**
     * Return the CE evc type based on the CLI-supplied arguments.
     *
     * @param evcTypeString EVC type
     * @param uniList       UNIs list
     * @return the CE evc type
     */
    public static CarrierEthernetConnection.Type generateEvcType(String evcTypeString, List<String> uniList) {
        if (evcTypeString == null) {
            return ((uniList.size() > 2) ?
                    CarrierEthernetConnection.Type.MULTIPOINT_TO_MULTIPOINT :
                    CarrierEthernetConnection.Type.POINT_TO_POINT);
        } else {
            // TODO: Catch exception
            return CarrierEthernetConnection.Type.valueOf(evcTypeString);
        }
    }

    /**
     * Return the EVC maxNumUni parameter based on the CLI-supplied arguments.
     *
     * @param maxNumUni     maximum numbers of UNIs
     * @param evcTypeString EVC Type
     * @param uniList       UNIs list
     * @return the maxNumUni parameter
     */
    public static Integer generateMaxNumUni(int maxNumUni, String evcTypeString, List<String> uniList) {
        if (maxNumUni == -1) {
            if (evcTypeString == null) {
                return ((uniList.size() > 2) ?
                        MAX_NUM_UNI : 2);
            } else {
                // TODO: Catch exception
                CarrierEthernetConnection.Type evcType =
                        CarrierEthernetConnection.Type.valueOf(evcTypeString);
                return (evcType.equals(CarrierEthernetConnection.Type.POINT_TO_POINT) ? 2 :
                        MAX_NUM_UNI);
            }
        } else {
            return maxNumUni;
        }
    }

    /**
     * Return the BW profile type based on the CLI-supplied arguments.
     *
     * @param ceVlanId vlanID
     * @return the BWP profile type
     */
    public static CarrierEthernetBandwidthProfile.Type generateBandwidthProfileType(short ceVlanId) {
        // TODO: Add the CoS BW profile case
        return ((ceVlanId == -1) ?
                CarrierEthernetBandwidthProfile.Type.INTERFACE : CarrierEthernetBandwidthProfile.Type.EVC);
    }

    /**
     * Return the BW profile id based on the CLI-supplied arguments.
     *
     * @param uniId    UNI id
     * @param evcCfgId EVC configuration Id
     * @param ceVlanId vlanID
     * @return the BW profile id
     */
    public static String generateBandwidthProfileId(String uniId, String evcCfgId, int ceVlanId) {
        // TODO: Add the CoS BW profile case
        return ((ceVlanId == -1) ? uniId : evcCfgId);
    }

    /**
     * Return the set of UNIs for the CE EVC based on the CLI-supplied arguments.
     *
     * @param evcTypeString Type of the UNI
     * @param uniList       list of remaning UNIs
     * @param ceVlanId      vlanID
     * @param firstUni      first UNI id
     * @param evcCfgId      EVC configuration Id
     * @param cir           The CIR in Mbps
     * @param eir           The EIR in Mbps
     * @param cbs           The CBS in Byte
     * @param ebs           The EBS in Byte
     * @return the set of UNIs for the CE EVC
     */
    public static Set<CarrierEthernetUni> generateUniSet(String evcTypeString, List<String> uniList,
                                                         short ceVlanId, String firstUni, String evcCfgId,
                                                         Double cir, Double eir, Long cbs, Long ebs) {

        Set<CarrierEthernetUni> uniSet = new HashSet<>();

        CarrierEthernetConnection.Type evcType = generateEvcType(evcTypeString, uniList);

        // We assume that first UNI supplied is always root
        uniSet.add(CarrierEthernetUni.builder()
                           .cp(ConnectPoint.deviceConnectPoint(firstUni))
                           .role(CarrierEthernetUni.Role.ROOT)
                           .ceVlanId(generateCeVlanId(ceVlanId))
                           .bwp(CarrierEthernetBandwidthProfile.builder()
                                        .id(generateBandwidthProfileId(firstUni, evcCfgId, ceVlanId))
                                        .type(generateBandwidthProfileType(ceVlanId))
                                        .cir(Bandwidth.mbps(cir))
                                        .eir(Bandwidth.mbps(eir))
                                        .cbs(cbs)
                                        .ebs(ebs)
                                        .build())
                           .build());

        final CarrierEthernetUni.Role role;
        // For E-Line and E-LAN all UNIs are roots. For E-Tree all UNIs are leafs except from one
        role = ((evcType == CarrierEthernetConnection.Type.ROOT_MULTIPOINT) ?
                CarrierEthernetUni.Role.LEAF : CarrierEthernetUni.Role.ROOT);

       uniList.forEach(argUni -> uniSet.add(
                CarrierEthernetUni.builder()
                        .cp(ConnectPoint.deviceConnectPoint(argUni))
                        .role(role)
                        .ceVlanId(generateCeVlanId(ceVlanId))
                        .bwp(CarrierEthernetBandwidthProfile.builder()
                                     .id(generateBandwidthProfileId(argUni, evcCfgId, ceVlanId))
                                     .type(generateBandwidthProfileType(ceVlanId))
                                     .cir(Bandwidth.mbps(cir))
                                     .eir(Bandwidth.mbps(eir))
                                     .cbs(cbs)
                                     .ebs(ebs)
                                     .build())
                        .build()));

        return uniSet;
    }
}
