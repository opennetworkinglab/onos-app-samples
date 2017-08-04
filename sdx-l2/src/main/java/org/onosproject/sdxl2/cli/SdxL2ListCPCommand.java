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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdxl2.SdxL2ConnectionPoint;
import org.onosproject.sdxl2.SdxL2Service;
import org.onosproject.sdxl2.SdxL2State;

import java.util.Optional;
import java.util.Set;

/**
 * CLI to list the SDX-L2 connection points.
 */
@Command(scope = "sdxl2", name = "sdxl2cp-list",
    description = "Lists all SDX-L2 Connection Points or details for a given SDX-L2")
public class SdxL2ListCPCommand extends AbstractShellCommand {

    @Argument(name = "sdxl2name", description = "Name of SDX-L2")
    private String sdxl2 = null;

    private static final String HEADER = "\n\u001B[1;37mStatus\t\tSDXL2 Connection Point\u001B[0m";
    private static final String SEPARATOR = "\u001B[1;37m-----------------------------------------------\u001B[0m";
    private static final String FORMAT_SDXL2CP_ONLINE = "\u001B[1;32m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";
    private static final String FORMAT_SDXL2CP_OFFLINE = "\u001B[1;31m%s\u001B[0m\t\t\u001B[1;37m%s\u001B[0m";

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        Optional<String> sdxl2name = Optional.ofNullable(sdxl2);
        Set<String> result = sdxl2Service.getSdxL2ConnectionPoints(sdxl2name);
        SdxL2ConnectionPoint sdxl2ConnectionPoint;
        SdxL2State sdxl2cpState;
        if (!result.isEmpty()) {
            print(HEADER);
            print(SEPARATOR);
            for (String sdxl2cp : result) {
                sdxl2ConnectionPoint = sdxl2Service.getSdxL2ConnectionPoint(sdxl2cp);
                if (sdxl2ConnectionPoint == null) {
                    return;
                }
                sdxl2cpState = sdxl2Service.getEdgePortState(sdxl2ConnectionPoint.connectPoint());
                if (sdxl2cpState == SdxL2State.ONLINE) {
                    print(FORMAT_SDXL2CP_ONLINE, "ONLINE", sdxl2cp);
                } else if (sdxl2cpState == SdxL2State.OFFLINE) {
                    print(FORMAT_SDXL2CP_OFFLINE, "OFFLINE", sdxl2cp);
                }
            }
            print("");
        }
    }
}
