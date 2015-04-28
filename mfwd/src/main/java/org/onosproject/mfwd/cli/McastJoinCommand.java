/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.mfwd.cli;

//import org.onosproject.mfwd.impl.McastRouteTable;
//import org.onosproject.mfwd.impl.McastRouteGroup;
//import org.onosproject.mfwd.impl.McastRouteSource;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Installs a source, multicast group flow.
 */
@Command(scope = "onos", name = "mcast-join",
         description = "Installs a source, multicast group flow")
public class McastJoinCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "sourceIp",
              description = "IP Address of the multicast source. '*' can be used for any source (*, G) entry",
              required = true, multiValued = false)
    String saddr = null;

    @Argument(index = 1, name = "multicastIp",
              description = "IP Address of the multicast group",
              required = true, multiValued = false)
    String maddr = null;

    @Argument(index = 3, name = "ingressPort",
            description = "Ingress ports",
            required = false, multiValued = false)
    String[] ingressPorts = null;

    @Argument(index = 4, name = "egressPorts",
              description = "Egress ports",
              required = false, multiValued = true)
    String[] egressPorts = null;

    /**
     * All static multicast joins.
     * TODO: Make this function work!
     */
    @Override
    protected void execute() {
    }
}
