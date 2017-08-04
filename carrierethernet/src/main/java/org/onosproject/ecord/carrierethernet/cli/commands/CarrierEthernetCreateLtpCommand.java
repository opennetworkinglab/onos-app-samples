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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetNetworkInterface;
import org.onosproject.net.ConnectPoint;

import java.util.Iterator;
import java.util.List;

/**
 * CLI command for generating one or more Carrier Ethernet Logical Termination Points.
 */
@Command(scope = "onos", name = "ce-ltp-create",
        description = "Creates Carrier Ethernet Logical Termination Points.")
public class CarrierEthernetCreateLtpCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argLtpType", description =
            "The LTP type to be generated (UNI/INNI/ENNI)." +
                    " AUTO will choose the type based based on current topology", required = true, multiValued = false)
    String argLtpType = null;
    @Argument(index = 1, name = "argConnectPoint", description =
            "The connect points in the topology to be associated with"
                    + " the generated LTPs", required = true, multiValued = true)
    List<String> argConnectPointList = Lists.newArrayList();


    @Override
    protected void execute() {

        CarrierEthernetService ceManager = get(CarrierEthernetService.class);

        CarrierEthernetNetworkInterface.Type ltpType = null;

        if (!argLtpType.equals("AUTO")) {
            try {
                ltpType = CarrierEthernetNetworkInterface.Type.valueOf(argLtpType);
            } catch (IllegalArgumentException e) {
                log.error("{} is not a valid LTP type, skipping LTP generation.");
                return;
            }
        }

        Iterator<String> cpIt = argConnectPointList.iterator();
        while (cpIt.hasNext()) {
            String argConnectPoint = cpIt.next();
            CarrierEthernetLogicalTerminationPoint ltp = ceManager
                    .generateLtp(ConnectPoint.deviceConnectPoint(argConnectPoint), ltpType);
            if (ltp != null) {
                ceManager.addGlobalLtp(ltp);
            }
        }
    }
}
