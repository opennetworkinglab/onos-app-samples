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
package org.onosproject.ecord.carrierethernet.cli.commands;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetBandwidthProfile;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetEnni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetForwardingConstruct;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetInni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetNetworkInterface;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onosproject.net.ConnectPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CLI command for installing a Carrier Ethernet Forwarding Construct.
 */
@Command(scope = "onos", name = "ce-fc-create",
        description = "Creates and installs a Carrier Ethernet Forwarding Construct.")
public class CarrierEthernetCreateFcCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argFcCfgId",
            description = "Service configuration ID", required = true, multiValued = false)
    String argFcCfgId = null;
    @Argument(index = 1, name = "argEvcType", description =
            "Service type (defaults to POINT_TO_POINT or MULTIPOINT_TO_MULTIPOINT, depending on number of LTPs)",
            required = false, multiValued = false)
    String argEvcType = null;
    @Argument(index = 2, name = "argFirstLtp", description =
            "First LTP in list (if point to multipoint, this is the root)", required = true, multiValued = false)
    String argFirstLtp = null;
    @Argument(index = 3, name = "argLtpList",
            description = "List of remaining LTPs (if point to multipoint, these are the leaves)",
            required = true, multiValued = true)
    List<String> argLtpList = Lists.newArrayList();
    @Option(name = "-v", aliases = "--cevlan", description = "CE-VLAN ID (applied to all UNIs)",
            required = false, multiValued = false)
    String argCeVlanId = null;
    @Option(name = "-s", aliases = "--svlan", description = "S-TAG (applied to all INNIs/ENNIs)",
            required = false, multiValued = false)
    String argsTag = null;
    @Option(name = "-id", aliases = "--fc-id", description = "The ID of an FC to be updated" +
            " (if FC does not exist, a new FC will be installed)", required = false, multiValued = false)
    String argFcId = null;
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
        CarrierEthernetService ceManager = get(CarrierEthernetService.class);
        ceManager.installFc(CarrierEthernetForwardingConstruct.builder()
                                    .id(argFcId)
                                    .cfgId(argFcCfgId)
                                    .type(generateServiceType())
                                    .ltpSet(generateLtpSet())
                                    .build());
    }

    /**
     * Return the CE-VLAN ID for the CE service based on the CLI-supplied argument.
     *
     * @return CE-VLAN ID for the CE service
     */
    private VlanId generateVlanId(String argVlanId) {
        return ((argVlanId == null) ? null : VlanId.vlanId(Short.parseShort(argVlanId)));
    }

    /**
     * Return the CE service type based on the CLI-supplied arguments.
     *
     * @return the CE service type
     */
    private CarrierEthernetVirtualConnection.Type generateServiceType() {
        if (argEvcType == null) {
            // FIXME: This probably applies only to list of UNIs
            return ((argLtpList.size() > 2) ?
                    CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT :
                    CarrierEthernetVirtualConnection.Type.POINT_TO_POINT);
        } else {
            // TODO: Catch exception
            return CarrierEthernetVirtualConnection.Type.valueOf(argEvcType);
        }
    }

    /**
     * Return the BW profile type based on the CLI-supplied arguments.
     *
     * @return the BWP profile type
     */
    private CarrierEthernetBandwidthProfile.Type generateBandwidthProfileType() {
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
    private String generateBandwidthProfileId(String uniId) {
        // TODO: Add the CoS BW profile case
        return ((argCeVlanId == null) ? uniId : argFcCfgId);
    }

    /**
     * Return the set of UNIs for the FC based on the CLI-supplied arguments.
     *
     * @return the set of UNIs for the FC
     */
    private Set<CarrierEthernetLogicalTerminationPoint> generateLtpSet() {

        CarrierEthernetService ceManager = get(CarrierEthernetService.class);

        // Update list of global LTPs in the network
        ceManager.getLtpsFromTopo(true, false).forEach(ltp -> ceManager.addGlobalLtp(ltp));

        Set<CarrierEthernetLogicalTerminationPoint> ltpSet = new HashSet<>();

        CarrierEthernetVirtualConnection.Type serviceType = generateServiceType();

        // We assume that first UNI supplied is always root
        ltpSet.add(new CarrierEthernetLogicalTerminationPoint(argFirstLtp,
                generateNi(argFirstLtp, CarrierEthernetUni.Role.ROOT)));

        // For E-Line and E-LAN all UNIs are roots. For E-Tree all UNIs are leafs except from one
        argLtpList.forEach(argLtp -> ltpSet
                .add(new CarrierEthernetLogicalTerminationPoint(argLtp,
                        generateNi(argLtp,
                                ((serviceType == CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT) ?
                                CarrierEthernetUni.Role.LEAF : CarrierEthernetUni.Role.ROOT)))));

        return ltpSet;
    }

    private CarrierEthernetNetworkInterface generateNi(String ltpId, CarrierEthernetUni.Role role) {

        CarrierEthernetService ceManager = get(CarrierEthernetService.class);

        if (ceManager.ltpMap().get(ltpId).ni() instanceof CarrierEthernetUni) {
            return CarrierEthernetUni.builder()
                    .cp(ConnectPoint.deviceConnectPoint(ltpId))
                    .role(role)
                    .ceVlanId(generateVlanId(argCeVlanId))
                    .bwp(CarrierEthernetBandwidthProfile.builder()
                                 .id(ltpId)
                                 .type(generateBandwidthProfileType())
                                 .cir(Bandwidth.mbps(Double.parseDouble(argCir)))
                                 .eir(Bandwidth.mbps(Double.parseDouble(argEir)))
                                 .cbs(Long.parseLong(argCbs))
                                 .ebs(Long.parseLong(argEbs))
                                 .build())
                    .build();
        } else if (ceManager.ltpMap().get(ltpId).ni() instanceof CarrierEthernetInni) {
            // FIXME: Use role properly
            return CarrierEthernetInni.builder()
                    .cp(ConnectPoint.deviceConnectPoint(ltpId))
                    .role(CarrierEthernetInni.Role.TRUNK)
                    .sVlanId(generateVlanId(argsTag))
                    .usedCapacity(Bandwidth.bps((double) 0))
                    .build();
        } else {
            // FIXME: Use role properly
            return CarrierEthernetEnni.builder()
                    .cp(ConnectPoint.deviceConnectPoint(ltpId))
                    .role(CarrierEthernetEnni.Role.HUB)
                    .sVlanId(generateVlanId(argsTag))
                    .usedCapacity(Bandwidth.bps((double) 0))
                    .build();
        }
    }

}
