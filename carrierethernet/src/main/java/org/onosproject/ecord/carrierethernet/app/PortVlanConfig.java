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
package org.onosproject.ecord.carrierethernet.app;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.Optional;

/**
 * Configuration information for edge connect point and corresponding VLAN tag.
 */
public class PortVlanConfig extends Config<ConnectPoint> {
    public static final String CONFIG_KEY = "portVlan";

    public static final String S_TAG_KEY = "s-tag";

    public Optional<VlanId> portVlanId() {
        String s = get(S_TAG_KEY, null);
        if (s == null) {
            return Optional.empty();
        }
        return Optional.of(VlanId.vlanId(Short.valueOf(s)));
    }

    public PortVlanConfig portVlanId(VlanId vlanId) {
        if (vlanId == null) {
            return (PortVlanConfig) setOrClear(S_TAG_KEY, (String) null);
        }
        return (PortVlanConfig) setOrClear(S_TAG_KEY, String.valueOf(vlanId.toShort()));
    }
}
