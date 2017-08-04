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

import java.util.List;

/**
 * CLI command for removing one or more installed EVCs.
 */
@Command(scope = "onos", name = "ce-evc-remove",
        description = "Carrier Ethernet EVC removal command.")
public class CarrierEthernetRemoveEvcCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argEvcIdList", description = "EVC IDs "
            + " to be removed", required = true, multiValued = true)
    List<String> argEvcIdList = Lists.newArrayList();

    @Override
    protected void execute() {
        CarrierEthernetService ceManager = get(CarrierEthernetService.class);
        argEvcIdList.forEach(argEvcId -> ceManager.removeEvc(argEvcId));
    }
}
