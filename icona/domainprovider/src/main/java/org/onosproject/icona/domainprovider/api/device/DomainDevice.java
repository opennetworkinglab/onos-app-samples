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

import org.onosproject.icona.domainprovider.api.DomainElement;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;

import java.util.List;

/**
 * Peer domain device.
 */
public interface DomainDevice extends DomainElement {

    /**
     * Returns the device ID.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Returns a list of port descriptions.
     * These ports differ in ports where end-points (hosts) are attached to, intra-link ports
     * and inter-link ports.
     *
     * @return list of port descriptions
     */
    List<PortDescription> ports();
}
