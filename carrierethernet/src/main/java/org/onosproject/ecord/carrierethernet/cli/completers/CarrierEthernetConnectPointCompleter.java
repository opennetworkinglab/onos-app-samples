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
package org.onosproject.ecord.carrierethernet.cli.completers;

import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Unique ConnectPoint completer.
 */
public class CarrierEthernetConnectPointCompleter extends AbstractCompleter {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

        StringsCompleter delegate = new UniqueStringsCompleter();
        SortedSet<String> strings = delegate.getStrings();

        DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

        CarrierEthernetLogicalTerminationPoint ltp;
        Set<CarrierEthernetLogicalTerminationPoint> ltpSet = new HashSet<>();
        // Generate the device ID/port number identifiers
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                if (!port.number().isLogical()) {
                    strings.add(device.id().toString() + "/" + port.number());
                }
            }
        }

        return delegate.complete(buffer, cursor, candidates);
    }

}
