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

import java.util.Optional;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;

import com.google.common.annotations.Beta;

// This Config is not specific to E-CORD, so it might make sense to move it out
/**
 * Configuration information about Cross Connect ports.
 */
@Beta
public class CrossConnectConfig extends Config<ConnectPoint> {

    /**
     * Indicates remote ConnectPoint connected to this cross connect port.
     *
     * @return ConnectPoint information
     */
    public Optional<ConnectPoint> remote() {
        String s = get("remote", null);
        if (s == null) {
            return Optional.empty();
        }
        String[] split = s.split("/");
        if (split.length != 2) {
            // ill-formed String
            return Optional.empty();
        }
        DeviceId did = DeviceId.deviceId(split[0]);
        PortNumber num = PortNumber.fromString(split[1]);
        return Optional.of(new ConnectPoint(did, num));
    }

    /**
     * Specifies the remote ConnectPoint connected to this cross connect port.
     *
     * @param remote device connect point identifier
     * @return self
     */
    public CrossConnectConfig remote(ConnectPoint remote) {
        if (remote == null) {
            return (CrossConnectConfig) setOrClear("remote", (String) null);
        }

        return (CrossConnectConfig) setOrClear("remote", String.format("%s/%s", remote.deviceId(), remote.port()));
    }
}
