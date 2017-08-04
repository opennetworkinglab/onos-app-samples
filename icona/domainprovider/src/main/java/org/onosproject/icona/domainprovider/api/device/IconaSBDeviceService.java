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
 * Service used by the southbound components to advertise remote device events.
 */
public interface IconaSBDeviceService {

    /**
     * Signals the domain provider to connect a domain device.
     *
     * @param domainDevice device object
     */
    void connectRemoteDevice(DomainDevice domainDevice);

    /**
     * Signals the domain provider to add a new port to a domain device.
     *
     * @param domainId domain identifier
     * @param deviceId device identifier
     * @param portDescription description of the port
     */
    void addRemotePort(DomainId domainId, DeviceId deviceId, PortDescription portDescription);

    /**
     * Signals a remote port update.
     *
     * @param domainId domain identifier
     * @param deviceId device identifier
     * @param portDescription port description
     */
    void updateRemotePortState(DomainId domainId, DeviceId deviceId, PortDescription portDescription);


    /**
     * Signals the disconnection of a remote domain device.
     *
     * @param domainId domain identifier
     * @param deviceId device identifier
     */
    void disconnectRemoteDevice(DomainId domainId, DeviceId deviceId);

    /**
     * Signals the deletion of a remote domain device port.
     *
     * @param domainId domain identifier
     * @param deviceId device identifier
     * @param portNumber port identifier
     */
    void removeRemotePort(DomainId domainId, DeviceId deviceId, PortNumber portNumber);

}
