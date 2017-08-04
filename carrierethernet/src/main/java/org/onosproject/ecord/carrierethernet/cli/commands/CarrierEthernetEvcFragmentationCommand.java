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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;

/**
 * CLI command for indicating whether EVCs will be fragmented into FCs.
 */
@Command(scope = "onos", name = "ce-evc-fragmentation",
        description = "Carrier Ethernet EVC fragmentation setup command. " +
                "When used without argument it shows the current fragmentation status.")
public class CarrierEthernetEvcFragmentationCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "evcFragArg", description = "Set to true if CE app " +
            "should fragment EVCs into FCs", required = false, multiValued = false)
    String evcFragArg = null;

    @Override
    protected void execute() {
        CarrierEthernetService ceManager = get(CarrierEthernetService.class);
        if (evcFragArg != null) {
            ceManager.setEvcFragmentation(Boolean.parseBoolean(evcFragArg));
        } else {
            print("  %s", ceManager.getEvcFragmentation());
        }
    }
}
