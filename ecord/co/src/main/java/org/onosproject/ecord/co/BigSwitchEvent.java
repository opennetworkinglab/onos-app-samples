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

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.device.PortDescription;

// TODO probably Event subject should contain Device info.
//      e.g., (DeviceId, PortDescription)
public class BigSwitchEvent extends AbstractEvent<BigSwitchEvent.Type, PortDescription> {

    public enum Type {
        /**
         * Signifies a port was added to the big switch.
         */
        PORT_ADDED,
        /**
         * Signifies a port was removed from the big switch.
         */
        PORT_REMOVED,
        /**
         * Signifies a port was updated in the big switch.
         */
        PORT_UPDATED
    }

    /**
     * Creates a new big switch event.
     *
     * @param type event type
     * @param subject the port description
     */
    public BigSwitchEvent(Type type, PortDescription subject) {
        super(type, subject);
    }

    /**
     * Creates a new big switch event.
     *
     * @param type event type
     * @param subject the port description
     * @param time occurence time
     */
    public BigSwitchEvent(Type type, PortDescription subject, long time) {
        super(type, subject, time);
    }
}
