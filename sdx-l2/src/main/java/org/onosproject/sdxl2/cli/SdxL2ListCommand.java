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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdxl2.SdxL2Service;

import java.util.Set;

/**
 * CLI to list the SDX-L2s.
 */
@Command(scope = "sdxl2", name = "sdxl2-list", description = "Lists the SDX-L2s")
public class SdxL2ListCommand extends AbstractShellCommand {

    private static final String HEADER = "\n\u001B[1;37mSDXL2\u001B[0m";
    private static final String SEPARATOR = "\u001B[1;37m--------------\u001B[0m";
    private static final String FORMAT_SDXL2 = "\u001B[1;37m%s\u001B[0m";

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        Set<String> sdxl2s = sdxl2Service.getSdxL2s();
        if (!sdxl2s.isEmpty()) {
            print(HEADER);
            print(SEPARATOR);
            for (String sdxl2 : sdxl2s) {
                print(FORMAT_SDXL2, sdxl2);
            }
            print("");
        }
    }
}
