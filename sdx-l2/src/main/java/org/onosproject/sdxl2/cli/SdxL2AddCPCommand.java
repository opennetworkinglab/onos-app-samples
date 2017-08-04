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
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdxl2.SdxL2ConnectionPoint;
import org.onosproject.sdxl2.SdxL2Service;

/**
 * CLI to create a named SDX-L2 connection point.
 */
@Command(scope = "sdxl2", name = "sdxl2cp-add", description = "Creates a named SDX-L2 Connection Point")
public class SdxL2AddCPCommand extends AbstractShellCommand {

    @Argument(name = "sdxl2name", description = "Name of SDX-L2",
            required = true)
    private String sdxl2name = null;

    @Argument(index = 1, name = "connectionpoint", description = "Identifier of SDX-L2 Connection point",
            required = true)
    private String cp = null;

    @Argument(index = 2, name = "sdxl2cpname", description = "Name of SDX-L2 Connection Point",
            required = true)
    private String sdxl2cpname = null;

    @Argument(index = 3, name = "vlans", description = "Customer edge VLANs, separated by dash " +
            "and comma")
    private String vlans = null;

    @Option(name = "-ce_mac", description = "Customer edge MAC address")
    private String mac = null;

    @Override
    protected void execute() {
        SdxL2Service sdxl2Service = get(SdxL2Service.class);
        SdxL2ConnectionPoint sdxl2cp = SdxL2ConnectionPoint.sdxl2ConnectionPoint(sdxl2cpname, cp, vlans, mac);
        sdxl2Service.addSdxL2ConnectionPoint(sdxl2name, sdxl2cp);
    }
}

