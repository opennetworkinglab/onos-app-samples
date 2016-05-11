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
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdxl2.SdxL2ConnectionPoint;
import org.onosproject.sdxl2.SdxL2Service;

/**
 * CLI to create a named SDX-L2 connection point.
 */
@Command(scope = "sdxl2", name = "sdxl2cp-add", description = "Create a named sdx-l2 connection point")
public class SdxL2AddCPCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "sdxl2name", description = "Sdxl2name",
            required = true, multiValued = false)
    String sdxl2name = null;

    @Argument(index = 1, name = "connectionpoint", description = "Connection point",
            required = true, multiValued = false)
    String cp = null;

    @Argument(index = 2, name = "vlans", description = "Customer edge vlans separated by comma",
            required = true, multiValued = false)
    String vlans = null;

    @Argument(index = 3, name = "sdxl2cpname", description = "Sdxl2 connection point name",
            required = true, multiValued = false)
    String sdxl2cpname = null;

    @Option(name = "-ce_mac", description = "Customer edge mac address",
            required = false, multiValued = false)
    String mac = null;

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        SdxL2ConnectionPoint sdxl2cp;
        if (mac != null) {
            sdxl2cp = SdxL2ConnectionPoint.sdxl2ConnectionPoint(sdxl2cpname, cp, vlans, mac);
        } else {
            sdxl2cp = SdxL2ConnectionPoint.sdxl2ConnectionPoint(sdxl2cpname, cp, vlans);
        }
        sdxl2Service.addSdxL2ConnectionPoint(sdxl2name, sdxl2cp);
    }

}

