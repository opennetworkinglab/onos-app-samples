/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ecord.metro;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.Optional;

/**
 * Configuration information for edge connect point and corresponding VLAN tag.
 */
public class CeVlanConfig extends Config<ConnectPoint> {
    public static final String CONFIG_KEY = "ceVlan";

    public static final String CE_VLAN_TAG_KEY = "tag";

    public Optional<VlanId> ceVlanId() {
        String s = get(CE_VLAN_TAG_KEY, null);
        if (s == null) {
            return Optional.empty();
        }
        return Optional.of(VlanId.vlanId(Short.valueOf(s)));
    }

    public CeVlanConfig ceVlanId(VlanId vlanId) {
        if (vlanId == null) {
            return (CeVlanConfig) setOrClear(CE_VLAN_TAG_KEY, (String) null);
        }
        return (CeVlanConfig) setOrClear(CE_VLAN_TAG_KEY, String.valueOf(vlanId.toShort()));
    }
}
