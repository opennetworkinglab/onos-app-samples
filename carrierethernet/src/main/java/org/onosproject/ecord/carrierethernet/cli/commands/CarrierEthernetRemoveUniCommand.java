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
 * CLI command for removing one or more UNIs.
 */
@Command(scope = "onos", name = "ce-uni-remove",
        description = "Carrier Ethernet UNI removal command.")
public class CarrierEthernetRemoveUniCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "argUniIdList", description = "UNI IDs"
            + " to be removed", required = true, multiValued = true)
    List<String> argUniIdList = Lists.newArrayList();

    @Override
    protected void execute() {
        CarrierEthernetService ceManager = get(CarrierEthernetService.class);
        argUniIdList.forEach(argUniId -> ceManager.removeGlobalUni(argUniId));
    }
}
