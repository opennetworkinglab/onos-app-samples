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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;

import java.util.List;

import static org.onosproject.ecord.carrierethernet.app.CarrierEthernetEvcUtils.*;

/**
 * CLI command for installing an Ethernet Virtual Connection.
 */
@Command(scope = "onos", name = "ce-evc-create",
        description = "Carrier Ethernet EVC creation command.")
public class CarrierEthernetCreateEvcCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argEvcCfgId",
            description = "EVC configuration ID", required = true, multiValued = false)
    String argEvcCfgId = null;
    @Argument(index = 1, name = "argEvcType", description =
            "EVC type (defaults to POINT_TO_POINT or MULTIPOINT_TO_MULTIPOINT, depending on number of UNIs)",
            required = false, multiValued = false)
    String argEvcType = null;
    @Argument(index = 2, name = "argUniList",
            description = "List of UNIs (if point to multipoint, first is root, other are leaves)",
            required = true, multiValued = true)
    List<String> argUniList = Lists.newArrayList();
    @Option(name = "-v", aliases = "--cevlan", description = "CE-VLAN ID (applied to all UNIs)",
            required = false, multiValued = false)
    short argCeVlanId = -1;
    @Option(name = "-id", aliases = "--evc-id", description = "The ID of a evc to be updated" +
            " (if evc does not exist, a new evc will be installed)", required = false, multiValued = false)
    String argEvcId = null;
    @Option(name = "-u", aliases = "--maxNumUni", description = "The maximum number of UNIs in the EVC",
            required = false, multiValued = false)
    int argMaxNumUni = -1;
    @Option(name = "-c", aliases = "--cir", description = "The CIR in Mbps", required = false, multiValued = false)
    double argCir = 0;
    @Option(name = "-e", aliases = "--eir", description = "The EIR in Mbps", required = false, multiValued = false)
    double argEir = 0;
    @Option(name = "-cbs", aliases = "--cbs", description = "The CBS in Bytes", required = false, multiValued = false)
    long argCbs = 0;
    @Option(name = "-ebs", aliases = "--ebs", description = "The EBS in Bytes", required = false, multiValued = false)
    long argEbs = 0;

    // TODO: Add further arguments for VLAN tag preservation, CoS preservation etc.

    @Override
    protected void execute() {
        CarrierEthernetService ceManager = get(CarrierEthernetService.class);
        ceManager.installEvc(CarrierEthernetVirtualConnection.builder().id(argEvcId).cfgId(argEvcCfgId)
                                     .type(generateEvcType(argEvcType, argUniList))
                                     .maxNumUni(generateMaxNumUni(argMaxNumUni, argEvcType, argUniList))
                                     .uniSet(generateUniSet(argEvcType,  argUniList.subList(1, argUniList.size()),
                                                            argCeVlanId, argUniList.get(0), argEvcCfgId, argCir,
                                                            argEir, argCbs, argEbs))
                                     .build());
    }

}