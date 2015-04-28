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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mfwd.impl.McastRouteTable;

import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

/**
 * Displays the source, multicast group flows entries.
 */
@Command(scope = "onos", name = "mcast-show", description = "Displays the source, multicast group flows")
public class McastShowCommand extends AbstractShellCommand {

    private final Logger log = getLogger(getClass());
    private static final String FMT = "(%s, %s) ingress=%s, egress=%s";

    @Override
    protected void execute() {
        McastRouteTable mrt = McastRouteTable.getInstance();
        if (outputJson()) {
            log.error("Not implemented yet");
        } else {
            print(mrt.printMcastRouteTable());
        }
    }
}

