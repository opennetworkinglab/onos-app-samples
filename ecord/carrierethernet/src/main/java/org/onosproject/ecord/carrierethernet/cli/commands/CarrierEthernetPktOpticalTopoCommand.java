/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetProvisioner;

/**
 * CLI command for indicating whether CE app controls a packet optical topology.
 */
@Command(scope = "onos", name = "ce-pkt-optical-topo",
        description = "Carrier Ethernet packet-optical topology setup command.")
public class CarrierEthernetPktOpticalTopoCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "pktOptTopoArg", description = "Set to true if CE app " +
            "controls a packet-optical topology", required = true, multiValued = false)
    String pktOptTopoArg = null;

    @Override
    protected void execute() {
        CarrierEthernetProvisioner ceProvisioner = get(CarrierEthernetProvisioner.class);
        ceProvisioner.setPktOpticalTopo(Boolean.parseBoolean(pktOptTopoArg));
    }
}
