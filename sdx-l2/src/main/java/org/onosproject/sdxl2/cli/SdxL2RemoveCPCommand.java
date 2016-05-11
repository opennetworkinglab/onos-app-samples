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


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdxl2.SdxL2Service;

/**
 * CLI to delete a named SDX-L2 connection point.
 */
@Command(scope = "sdxl2", name = "sdxl2cp-remove", description = "Remove a named sdxl2 connection point")
public class SdxL2RemoveCPCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "sdxl2cpname", description = "Sdxl2 connection point name",
            required = true, multiValued = false)
    String sdxl2cpname = null;

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        sdxl2Service.removeSdxL2ConnectionPoint(sdxl2cpname);
    }

}

