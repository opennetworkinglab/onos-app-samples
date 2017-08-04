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
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetProvisioner;

/**
 * CLI command for indicating whether CE app controls a packet optical topology.
 */
@Command(scope = "onos", name = "ce-pkt-optical-topo",
        description = "Carrier Ethernet packet-optical topology setup command. " +
        "When used without argument it shows the current pkt-optical topology status.")
public class CarrierEthernetPktOpticalTopoCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "pktOptTopoArg", description = "Set to true if CE app " +
            "controls a packet-optical topology", required = false, multiValued = false)
    String pktOptTopoArg = null;

    @Override
    protected void execute() {
        CarrierEthernetProvisioner ceProvisioner = get(CarrierEthernetProvisioner.class);
        if (pktOptTopoArg != null) {
            boolean pktOpticalTopo = Boolean.parseBoolean(pktOptTopoArg);
            // Change pkt-optical topology flag only if needed
            if (pktOpticalTopo ^ ceProvisioner.getPktOpticalTopo()) {
                ceProvisioner.setPktOpticalTopo(pktOpticalTopo);
                // FIXME: Temporary hack - disable EVC fragmentation for pkt-optical
                // This is needed because CarrierEthernetManager performs path computation
                // during the fragmentation process
                CarrierEthernetService ceManager = get(CarrierEthernetService.class);
                if (pktOpticalTopo) {
                    ceManager.setEvcFragmentation(false);
                } else {
                    ceManager.resetEvcFragmentation();
                }
            }
        } else {
            print("  %s", ceProvisioner.getPktOpticalTopo());
        }
    }
}
