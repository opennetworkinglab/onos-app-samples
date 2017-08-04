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
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;

import java.util.List;
import java.util.SortedSet;

/**
 * UNI id completer, not including UNIs that have been removed.
 */
public class CarrierEthernetValidUniCompleter extends AbstractCompleter {

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

        StringsCompleter delegate = new UniqueStringsCompleter();
        SortedSet<String> strings = delegate.getStrings();

        CarrierEthernetService ceManager = AbstractShellCommand.get(CarrierEthernetService.class);
        ceManager.getUnisFromTopo(false, false).forEach(uni -> strings.add(uni.id()));

        return delegate.complete(buffer, cursor, candidates);
    }
}