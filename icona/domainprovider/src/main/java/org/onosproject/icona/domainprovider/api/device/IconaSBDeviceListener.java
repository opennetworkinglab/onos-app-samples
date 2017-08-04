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

package org.onosproject.icona.domainprovider.api.device;

import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortDescription;

/**
 * Methods implemented by the southbound components for (local topology) device events.
 */
public interface IconaSBDeviceListener {

    /**
     * Connects a new domain device with its list of ports to a remote domain.
     * The first parameter is the domain ID of the receiving domain, while the domain
     * ID inside the device parameter is the local domain identifier which is
     * the "owner" of the device.
     *
     * @param domainId domain interested to this device
     * @param device to be added
     */
    void connectDevice(DomainId domainId, DomainDevice device);

    /**
     * Adds a port to an existing domain device.
     *
     * @param domainId domain interested to this addition
     * @param deviceId device identifier
     * @param portDescription description of the port
     */
    void addPort(DomainId domainId, DeviceId deviceId, PortDescription portDescription);

    /**
     * Enables or disables a port on a domain device.
     * No distinction between administrative and operational state currently.
     *
     * @param domainId domain interested to this change
     * @param deviceId device identifier
     * @param portNumber port identifier
     * @param enable true if port is to be enabled, false to disable
     */
    void updatePortState(DomainId domainId, DeviceId deviceId, PortNumber portNumber, boolean enable);

    /**
     * Disconnects the device with the specified identifier.
     *
     * @param  domainId domain interested to this update
     * @param deviceId device identifier
     */
    void disconnectDevice(DomainId domainId, DeviceId deviceId);

    /**
     * Removes the port with the specified identifier.
     *
     * @param domainId domain interested to this update
     * @param deviceId device identifier
     * @param portNumber port number
     */
    void removePort(DomainId domainId, DeviceId deviceId, PortNumber portNumber);
}