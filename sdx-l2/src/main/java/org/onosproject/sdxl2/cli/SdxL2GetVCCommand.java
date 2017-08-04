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

package org.onosproject.sdxl2.cli;

import com.google.common.collect.Iterables;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.sdxl2.SdxL2ConnectionPoint;
import org.onosproject.sdxl2.SdxL2ConnectionPointMatcher;
import org.onosproject.sdxl2.SdxL2Service;
import org.onosproject.sdxl2.SdxL2State;
import org.onosproject.sdxl2.VirtualCircuit;

import java.util.Iterator;

/**
 * CLI to print the details of a Virtual Circuit in a SDX-L2.
 */
@Command(scope = "sdxl2", name = "sdxl2vc", description = "Prints the details of a SDX-L2 Virtual Circuit")
public class SdxL2GetVCCommand extends AbstractShellCommand {

    @Argument(name = "sdxl2vcname", description = "Name of SDX-L2 VC",
            required = true)
    private String sdxl2vcname = null;

    private static final String HEADER_CP =
            "\n\u001B[1;37mStatus\t\tConnection Point\t\tName\t\tVlan IDs\t\tCE Mac Address\u001B[0m";
    private static final String SEPARATOR_CP = "\u001B[1;37m------------" +
            "-----------------------------------------------------------" +
            "--------------------------------------\u001B[0m";
    private static final String FORMAT_SDXL2CP_ONLINE =
            "\u001B[1;32m%s\u001B[0m\t\t\u001B[1;37m%s/%s\t\t%s\t\t%s\t\t%s\u001B[0m";
    private static final String FORMAT_SDXL2CP_OFFLINE =
            "\u001B[1;31m%s\u001B[0m\t\t\u001B[1;37m%s/%s\t\t%s\t\t%s\t\t%s\u001B[0m";

    private static final String HEADER_VC =
            "\n\u001B[1;37mStatus\t\tIntent\u001B[0m";
    private static final String SEPARATOR_VC =
            "\u001B[1;37m--------------------------------------------\u001B[0m";
    private static final String FORMAT_SDXL2VC_ONLINE =
            "\u001B[1;32m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";
    private static final String FORMAT_SDXL2VC_OFFLINE =
            "\u001B[1;31m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";
    private static final String FORMAT_SDXL2VC_CHECK =
            "\u001B[1;33m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        SdxL2ConnectionPointMatcher matcher = new SdxL2ConnectionPointMatcher();
        VirtualCircuit virtualCircuit;
        SdxL2ConnectionPoint sdxl2ConnectionPoint;
        SdxL2State state;
        virtualCircuit = sdxl2Service.getVirtualCircuit(sdxl2vcname);
        if (virtualCircuit == null) {
            return;
        }

        print(HEADER_CP);
        print(SEPARATOR_CP);

        sdxl2ConnectionPoint = virtualCircuit.lhs();
        state = sdxl2Service.getEdgePortState(sdxl2ConnectionPoint.connectPoint());
        if (state == SdxL2State.ONLINE) {
            print(FORMAT_SDXL2CP_ONLINE,
                  "ONLINE",
                  sdxl2ConnectionPoint.connectPoint().elementId(),
                  sdxl2ConnectionPoint.connectPoint().port(),
                  sdxl2ConnectionPoint.name(),
                  sdxl2ConnectionPoint.vlanIds(),
                  sdxl2ConnectionPoint.macAddress());
        } else if (state == SdxL2State.OFFLINE) {
            print(FORMAT_SDXL2CP_OFFLINE,
                  "OFFLINE",
                  sdxl2ConnectionPoint.connectPoint().elementId(),
                  sdxl2ConnectionPoint.connectPoint().port(),
                  sdxl2ConnectionPoint.name(),
                  sdxl2ConnectionPoint.vlanIds(),
                  sdxl2ConnectionPoint.macAddress());
        }


        sdxl2ConnectionPoint = virtualCircuit.rhs();
        state = sdxl2Service.getEdgePortState(sdxl2ConnectionPoint.connectPoint());
        if (state == SdxL2State.ONLINE) {
            print(FORMAT_SDXL2CP_ONLINE,
                  "ONLINE",
                  sdxl2ConnectionPoint.connectPoint().elementId(),
                  sdxl2ConnectionPoint.connectPoint().port(),
                  sdxl2ConnectionPoint.name(),
                  sdxl2ConnectionPoint.vlanIds(),
                  sdxl2ConnectionPoint.macAddress());
        } else if (state == SdxL2State.OFFLINE) {
            print(FORMAT_SDXL2CP_OFFLINE,
                  "OFFLINE",
                  sdxl2ConnectionPoint.connectPoint().elementId(),
                  sdxl2ConnectionPoint.connectPoint().port(),
                  sdxl2ConnectionPoint.name(),
                  sdxl2ConnectionPoint.vlanIds(),
                  sdxl2ConnectionPoint.macAddress());
        }
        print("");

        print(HEADER_VC);
        print(SEPARATOR_VC);
        IntentService intentService = get(IntentService.class);
        Iterator<Intent> intents = Iterables.filter(
                intentService.getIntents(), intent ->
                        (matcher.matches(virtualCircuit.lhs(),
                                                    virtualCircuit.rhs(), intent) ||
                                (matcher.matches(virtualCircuit.rhs(),
                                                            virtualCircuit.lhs(), intent))))
                .iterator();
        Intent intent;
        Key key;
        while (intents.hasNext()) {
            intent = intents.next();
            key = intent.key();
            state = sdxl2Service.getIntentState(key);
            if (state == SdxL2State.ONLINE) {
                print(FORMAT_SDXL2VC_ONLINE,
                      "ONLINE",
                      key);
            } else if (state == SdxL2State.OFFLINE) {
                print(FORMAT_SDXL2VC_OFFLINE,
                      "OFFLINE",
                      key);
            } else {
                print(FORMAT_SDXL2VC_CHECK,
                      "CHECK",
                      key);
            }
        }
        print("");
    }
}
