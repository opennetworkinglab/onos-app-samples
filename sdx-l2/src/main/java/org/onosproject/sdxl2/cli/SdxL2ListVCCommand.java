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

package org.onosproject.sdxl2.cli;

import com.google.common.collect.Iterables;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.sdxl2.SdxL2ConnectionPoint;
import org.onosproject.sdxl2.SdxL2Service;
import org.onosproject.sdxl2.SdxL2State;
import org.onosproject.sdxl2.VirtualCircuit;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * CLI to delete a Connection Point in an SDX-L2.
 */
@Command(scope = "sdxl2", name = "sdxl2vc-list",
        description = "Lists all the SDX-L2 Virtual Circuits. Argument not required the name of SDX-L2")
public class SdxL2ListVCCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "Name of SDX-L2",
            description = "Sdxl2 name", required = false, multiValued = false)
    private String sdxl2 = null;

    private static final String MATCH_FORMAT = "%s-%s";

    private static final String HEADER =
            "\n\u001B[1;37mStatus\t\tVirtual Circuit\u001B[0m";
    private static final String SEPARATOR =
            "\u001B[1;37m-----------------------------------------------\u001B[0m";
    private static final String FORMAT_SDXL2VC_ONLINE =
            "\u001B[1;32m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";
    private static final String FORMAT_SDXL2VC_OFFLINE =
            "\u001B[1;31m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";
    private static final String FORMAT_SDXL2VC_CHECK =
            "\u001B[1;33m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";


    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        Optional<String> sdxl2name = Optional.ofNullable(sdxl2);
        Set<String> result = sdxl2Service.getVirtualCircuits(sdxl2name);
        VirtualCircuit vc;
        SdxL2State state;
        print(HEADER);
        print(SEPARATOR);
        if (result.size() > 0) {
            String[] sdxl2VC;
            for (String sdxl2vc : result) {
                sdxl2VC = sdxl2vc.split(":");
                vc = sdxl2Service.getVirtualCircuit(sdxl2vc);
                if (vc == null) {
                    break;
                }
                state = this.getVirtualCircuitState(vc);
                if (state == SdxL2State.ONLINE) {
                    print(FORMAT_SDXL2VC_ONLINE, "ONLINE", sdxl2VC[1]);
                } else if (state == SdxL2State.OFFLINE) {
                    print(FORMAT_SDXL2VC_OFFLINE, "OFFLINE", sdxl2VC[1]);
                } else {
                    print(FORMAT_SDXL2VC_CHECK, "CHECK", sdxl2VC[1]);
                }
            }
            print("");
        }
    }

    /**
     * Retrieves status of a Virtual Circuit from the status of its
     * Connection Points.
     *
     * @param vc VirtualCircuit object
     * @return state of the Virtual Circuit
     */
    private SdxL2State getVirtualCircuitState(VirtualCircuit vc) {
        IntentService intentService = get(IntentService.class);
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        SdxL2State intentState = SdxL2State.ONLINE;
        SdxL2State lhsState;
        SdxL2State rhsstate;
        Iterator<Intent> intents = Iterables.filter(intentService.getIntents(), intent ->
                (matches(vc.lhs(), vc.rhs(), intent) ||
                        (matches(vc.rhs(), vc.lhs(), intent)))).iterator();
        Intent intent;
        Key key;
        int numIntents = 0;
        int numIntentsOffline = 0;
        while (intents.hasNext()) {
            intent = intents.next();
            key = intent.key();
            intentState = sdxl2Service.getIntentState(key);
            if (intentState == SdxL2State.OFFLINE || intentState == SdxL2State.CHECK) {
                numIntentsOffline = numIntentsOffline + 1;
            }
            numIntents = numIntents + 1;
        }

        if (numIntents == numIntentsOffline) {
            return SdxL2State.OFFLINE;
        }

        lhsState = sdxl2Service.getEdgePortState(vc.lhs().connectPoint());
        if (lhsState == SdxL2State.OFFLINE) {
            return SdxL2State.OFFLINE;
        }

        rhsstate = sdxl2Service.getEdgePortState(vc.rhs().connectPoint());
        if (rhsstate == SdxL2State.OFFLINE) {
            return SdxL2State.OFFLINE;
        }

        if (intentState == SdxL2State.ONLINE && lhsState == SdxL2State.ONLINE &&
                rhsstate == SdxL2State.ONLINE) {
            return SdxL2State.ONLINE;
        }

        return SdxL2State.CHECK;
    }

    /**
     * Matches an intent given two SDX-L2 Connection Points.
     *
     * @param lhs    left hand side of the virtual circuit
     * @param rhs    right hand side of the virtual circuit
     * @param intent intent to match
     * @return result of the match
     */
    private boolean matches(SdxL2ConnectionPoint lhs, SdxL2ConnectionPoint rhs, Intent intent) {
        String key = intent.key().toString();
        String[] fields = key.split(":");
        String cps = format(MATCH_FORMAT, lhs.name(), rhs.name());

        return fields.length == 2 && fields[1].contains(cps);
    }
}
