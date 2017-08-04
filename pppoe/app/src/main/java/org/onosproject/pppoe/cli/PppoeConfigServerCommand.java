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

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Changes/configures PPPoE server parameters.
 */
@Command(scope = "onos", name = "pppoe-server-config",
        description = "Changes/configures PPPoE server parameters")
public class PppoeConfigServerCommand extends AbstractShellCommand {

    private static final String COLON = ":";
    private static final String RADIUS_IP = "radius-ip";
    private static final String RADIUS_KEY = "radius-key";
    private static final Map<String, Integer> PPPOE_SERVER_PARAMS = new HashMap<String, Integer>() {
        {
            put(RADIUS_IP, 2);
            put(RADIUS_KEY, 2);
        }
    };

    @Argument(index = 0, name = "target", description = "parameter:value",
            required = true, multiValued = false)
    String target = null;

    @Override
    protected void execute() {
        PppoeService service = AbstractShellCommand.get(PppoeService.class);

        String[] data = target.split(COLON);
        short argc = 0;

        String paramName = data[argc++];
        checkArgument(PPPOE_SERVER_PARAMS.containsKey(paramName),
                "Unsupported parameter %s", paramName);

        checkArgument((data.length == PPPOE_SERVER_PARAMS.get(paramName)),
                "Invalid number of inputs %s", target);
        String paramValue = data[argc++];

        service.setPppoeServer(paramName, paramValue);
    }

}
