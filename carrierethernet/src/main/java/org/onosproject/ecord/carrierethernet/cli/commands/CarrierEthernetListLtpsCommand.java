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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;

import java.util.Collection;

/**
 * CLI command for listing all CE LTPs.
 */
@Command(scope = "onos", name = "ce-ltp-list",
        description = "Lists all Carrier Ethernet LTPs.")
public class CarrierEthernetListLtpsCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        CarrierEthernetService evcManager = get(CarrierEthernetService.class);
        // Populate global LTP map
        evcManager.getLtpsFromTopo(false, false).forEach(ltp -> evcManager.addGlobalLtp(ltp));
        printLtps(evcManager.ltpMap().values());
    }

    private void printLtps(Collection<CarrierEthernetLogicalTerminationPoint> ltps) {
        ltps.forEach(ltp -> print("  %s", ltp));
    }
}
