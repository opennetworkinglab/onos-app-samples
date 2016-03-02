/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.ecord.carrierethernet.cli;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;

import java.util.List;
import java.util.SortedSet;

/**
 * UNI ConnectPoint completer.
 */
public class CarrierEthernetUniCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

        // TODO: Add memory

        StringsCompleter delegate = new StringsCompleter();

        LinkService linkService = AbstractShellCommand.get(LinkService.class);
        DeviceService service = AbstractShellCommand.get(DeviceService.class);

        // Generate the device ID/port number identifiers
        for (Device device : service.getDevices()) {
            SortedSet<String> strings = delegate.getStrings();
            for (Port port : service.getPorts(device.id())) {
                // Consider only physical ports which are currently active
                if (!port.number().isLogical() && port.isEnabled()) {
                    String cpString = device.id().toString() + "/" + port.number();
                    ConnectPoint cp = ConnectPoint.deviceConnectPoint(cpString);
                    // Add the generated connect point only if it doesn't belong to any link
                    // and if the device is a packet switch
                    if (linkService.getEgressLinks(cp).isEmpty() && linkService.getIngressLinks(cp).isEmpty() &&
                            device.type().equals(Device.Type.SWITCH)) {
                        strings.add(cpString);
                    }
                }
            }
        }

        return delegate.complete(buffer, cursor, candidates);
    }

}
