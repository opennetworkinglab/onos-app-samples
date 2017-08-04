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

/**
 * CLI to wipe out the state of the Application.
 */
@Command(scope = "sdxl2", name = "sdxl2-clean", description = "Wipes out the state of SDX-L2")
public class SdxL2CleanCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        sdxl2Service.cleanSdxL2();
    }
}
