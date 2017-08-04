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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Changes/configures PPPoE client parameters.
 */
@Command(scope = "onos", name = "pppoe-client-config",
        description = "Changes/configures PPPoE client parameters")
public class PppoeConfigClientCommand extends AbstractShellCommand {

    private static final String COLON = ":";
    private static final String ADMIN_STATE = "admin-state";
    private static final String END_SESSION = "end-session";
    private static final Map<String, Integer> PPPOE_CLIENT_PARAMS = new HashMap<String, Integer>() {
        {
            put(ADMIN_STATE, 2);
            put(END_SESSION, 1);
        }
    };

    @Argument(index = 0, name = "ssid", description = "Service Specific ID",
            required = true, multiValued = false)
    String ssid = null;

    @Argument(index = 1, name = "target", description = "parameter[:value]",
            required = true, multiValued = false)
    String target = null;

    private static final Pattern PATTERN_SSID = Pattern.compile("[0-9a-fA-F]+");


    @Override
    protected void execute() {
        PppoeService service = AbstractShellCommand.get(PppoeService.class);

        Matcher matcher = PATTERN_SSID.matcher(ssid);
        checkArgument(matcher.matches(), "Containing non-hexadecimal SSID %s", ssid);

        boolean flag = ((ssid.length() % 2) == 0);
        checkArgument(flag, "Invalid odd length SSID %s", ssid);

        short argc = 0;

        String[] data = target.split(COLON);
        String paramName = data[argc++];
        checkArgument(PPPOE_CLIENT_PARAMS.containsKey(paramName),
                "Unsupported parameter %s", paramName);

        checkArgument((data.length == PPPOE_CLIENT_PARAMS.get(paramName)),
                "Invalid number of inputs %s", target);

        String paramValue = "";
        if (paramName.equals(ADMIN_STATE)) {
            paramValue = data[argc++];
        }
        service.setPppoeClient(ssid, paramName, paramValue);
    }

}
