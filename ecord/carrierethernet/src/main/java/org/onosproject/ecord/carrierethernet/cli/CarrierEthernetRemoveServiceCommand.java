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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetManager;
import org.onosproject.cli.AbstractShellCommand;

/**
 * CLI command for removing a specific installed CE service.
 */
@Command(scope = "onos", name = "ce-service-remove",
        description = "Carrier Ethernet service removal command.")
public class CarrierEthernetRemoveServiceCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argServiceId", description = "Service ID", required = true, multiValued = false)
    String argServiceId = null;

    @Override
    protected void execute() {
        CarrierEthernetManager evcManager = get(CarrierEthernetManager.class);
        evcManager.removeEvc(argServiceId);
    }
}