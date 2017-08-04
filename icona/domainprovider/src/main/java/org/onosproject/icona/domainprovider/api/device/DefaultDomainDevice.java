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

import com.google.common.collect.ImmutableList;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of a domain device.
 */
public class DefaultDomainDevice implements DomainDevice {

    private final DeviceId deviceId;
    private final DomainId domainId;
    private final List<PortDescription> ports;

    public DefaultDomainDevice(DeviceId deviceId, DomainId domainId, List<PortDescription> ports) {
        this.deviceId = deviceId;
        this.domainId = domainId;
        this.ports = ports;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public DomainId domainId() {
        return domainId;
    }

    @Override
    public List<PortDescription> ports() {
        return ImmutableList.copyOf(ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deviceId, domainId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultDomainDevice) {
            final DefaultDomainDevice other = (DefaultDomainDevice) obj;
            return  Objects.equals(this.deviceId, other.deviceId()) &&
                    Objects.equals(this.domainId, other.domainId());

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("deviceId", deviceId.toString())
                .add("domainId", domainId)
                .add("ports", ports.toString())
                .toString();
    }
}
