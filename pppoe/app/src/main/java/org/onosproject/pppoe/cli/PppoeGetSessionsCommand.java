/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.pppoe.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.pppoe.api.PppoeService;
import org.onosproject.pppoe.api.PppoeSessionInfo;

import java.util.Collection;


/**
 * Lists one or more PPPoE session information.
 */
@Command(scope = "onos", name = "pppoe-session-info",
        description = "Lists one or more PPPoE session information")
public class PppoeGetSessionsCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ssid", description = "Service Specific ID",
            required = false, multiValued = false)
    String ssid = null;

    @Override
    protected void execute() {

        PppoeService service = AbstractShellCommand.get(PppoeService.class);

        Collection<PppoeSessionInfo> sessions = service.getPppoeSessions();

        if (ssid == null) {
            sessions.forEach(session -> {
                 print(session.toString());
            });
        } else {
            sessions.forEach(session -> {
                 if (ssid.equals(session.serviceSpecificId())) {
                     print(session.toString());
                 }
            });
        }
    }

}
