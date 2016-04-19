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
package org.onosproject.ecord.carrierethernet.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetBandwidthProfile;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetManager;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CLI command for generating CE services.
 */
@Command(scope = "onos", name = "ce-service-create",
         description = "Carrier Ethernet service creation command.")
public class CarrierEthernetCreateServiceCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argServiceCfgId",
            description = "Service configuration ID", required = true, multiValued = false)
    String argServiceCfgId = null;
    @Argument(index = 1, name = "argServiceType", description =
            "Service type (defaults to POINT_TO_POINT or MULTIPOINT_TO_MULTIPOINT, depending on number of UNIs)",
            required = false, multiValued = false)
    String argServiceType = null;
    @Argument(index = 2, name = "argFirstUni", description =
            "First UNI in list (if point to multipoint, this is the root)", required = true, multiValued = false)
    String argFirstUni = null;
    @Argument(index = 3, name = "argUniList",
            description = "List of remaining UNIs (if point to multipoint, these are the leaves)",
            required = true, multiValued = true)
    List<String> argUniList = Lists.newArrayList();
    @Option(name = "-v", aliases = "--cevlan", description = "CE-VLAN ID (applied to all UNIs)",
            required = false, multiValued = false)
    String argCeVlanId = null;
    @Option(name = "-id", aliases = "--service-id", description = "The ID of a service to be updated" +
            " (if service does not exist, a new service will be installed)", required = false, multiValued = false)
    String argServiceId = null;
    @Option(name = "-u", aliases = "--maxNumUni", description = "The maximum number of UNIs in the EVC",
            required = false, multiValued = false)
    String argMaxNumUni = null;
    @Option(name = "-c", aliases = "--cir", description = "The CIR in Mbps", required = false, multiValued = false)
    String argCir = "0";
    @Option(name = "-e", aliases = "--eir", description = "The EIR in Mbps", required = false, multiValued = false)
    String argEir = "0";
    @Option(name = "-cbs", aliases = "--cbs", description = "The CBS in Bytes", required = false, multiValued = false)
    String argCbs = "0";
    @Option(name = "-ebs", aliases = "--ebs", description = "The EBS in Bytes", required = false, multiValued = false)
    String argEbs = "0";

    // TODO: Add further arguments for VLAN tag preservation, CoS preservation etc.

    @Override
    protected void execute() {

        CarrierEthernetManager evcManager = get(CarrierEthernetManager.class);

        CarrierEthernetVirtualConnection evc = new CarrierEthernetVirtualConnection(argServiceId, argServiceCfgId,
                generateServiceType(), generateMaxNumUni(), generateUniSet());

        evcManager.establishConnectivity(evc);
    }

    /**
     * Return the CE-VLAN ID for the CE service based on the CLI-supplied argument.
     *
     * @return CE-VLAN ID for the CE service
     */
    VlanId generateCeVlanId() {
        return ((argCeVlanId == null) ? null : VlanId.vlanId(Short.parseShort(argCeVlanId)));
    }

    /**
     * Return the CE service type based on the CLI-supplied arguments.
     *
     * @return the CE service type
     */
    CarrierEthernetVirtualConnection.Type generateServiceType() {
        if (argServiceType == null) {
            return ((argUniList.size() > 2) ?
                    CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT :
                    CarrierEthernetVirtualConnection.Type.POINT_TO_POINT);
        } else {
            // TODO: Catch exception
            return CarrierEthernetVirtualConnection.Type.fromString(argServiceType);
        }
    }

    /**
     * Return the EVC maxNumUni parameter based on the CLI-supplied arguments.
     *
     * @return the maxNumUni parameter
     */
    Integer generateMaxNumUni() {
        if (argMaxNumUni == null) {
            if (argServiceType == null) {
                return ((argUniList.size() > 2) ?
                        CarrierEthernetVirtualConnection.MAX_NUM_UNI : 2);
            } else {
                // TODO: Catch exception
                CarrierEthernetVirtualConnection.Type evcType =
                        CarrierEthernetVirtualConnection.Type.fromString(argServiceType);
                return (evcType.equals(CarrierEthernetVirtualConnection.Type.POINT_TO_POINT) ? 2 :
                        CarrierEthernetVirtualConnection.MAX_NUM_UNI);
            }
        } else {
            return Integer.valueOf(argMaxNumUni);
        }
    }

    /**
     * Return the BW profile type based on the CLI-supplied arguments.
     *
     * @return the BWP profile type
     */
    CarrierEthernetBandwidthProfile.Type generateBandwidthProfileType() {
        // TODO: Add the CoS BW profile case
        return ((argCeVlanId == null) ?
                CarrierEthernetBandwidthProfile.Type.INTERFACE : CarrierEthernetBandwidthProfile.Type.EVC);
    }

    /**
     * Return the BW profile id based on the CLI-supplied arguments.
     *
     * @param uniId the UNI id
     * @return the BW profile id
     */
    String generateBandwidthProfileId(String uniId) {
        // TODO: Add the CoS BW profile case
        return ((argCeVlanId == null) ? uniId : argServiceCfgId);
    }

    /**
     * Return the set of UNIs for the CE service based on the CLI-supplied arguments.
     *
     * @return the set of UNIs for the CE service
     */
    Set<CarrierEthernetUni> generateUniSet() {

        Set<CarrierEthernetUni> uniSet = new HashSet<>();

        CarrierEthernetVirtualConnection.Type serviceType = generateServiceType();

        // We assume that first UNI supplied is always root
        uniSet.add(new CarrierEthernetUni(ConnectPoint.deviceConnectPoint(argFirstUni), null,
                CarrierEthernetUni.Role.ROOT, generateCeVlanId(),
                new CarrierEthernetBandwidthProfile(
                        generateBandwidthProfileId(argFirstUni),
                        null,
                        generateBandwidthProfileType(),
                        Bandwidth.mbps(Double.parseDouble(argCir)),
                        Bandwidth.mbps(Double.parseDouble(argEir)),
                        Long.parseLong(argCbs),
                        Long.parseLong(argEbs)
                )));

        final CarrierEthernetUni.Role uniType;
        // For E-Line and E-LAN all UNIs are roots. For E-Tree all UNIs are leafs except from one
        uniType = ((serviceType == CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT) ?
                CarrierEthernetUni.Role.LEAF : CarrierEthernetUni.Role.ROOT);

        argUniList.forEach(argUni -> uniSet.add(new CarrierEthernetUni(ConnectPoint.deviceConnectPoint(argUni), null,
                uniType, generateCeVlanId(),
                new CarrierEthernetBandwidthProfile(
                        generateBandwidthProfileId(argUni),
                        null,
                        generateBandwidthProfileType(),
                        Bandwidth.mbps(Double.parseDouble(argCir)),
                        Bandwidth.mbps(Double.parseDouble(argEir)),
                        Long.parseLong(argCbs),
                        Long.parseLong(argEbs)
                ))));

        return uniSet;
    }
}
