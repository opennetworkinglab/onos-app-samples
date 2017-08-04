/*
 * Copyright 2015 Open Networking Foundation
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
package org.onosproject.ecord.co;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortDescription;

import java.util.List;

/**
 * Service to interact with big switch abstraction.
 */
public interface BigSwitchService extends ListenerService<BigSwitchEvent, BigSwitchListener> {
    /**
     * Get list of big switch ports.
     *
     * @return list of port descriptions
     */
    List<PortDescription> getPorts();

    /**
     * Get the big switch port mapped to the physical port.
     *
     * @param port the physical port
     * @return virtual port number
     */
    PortNumber getPort(ConnectPoint port);
}
