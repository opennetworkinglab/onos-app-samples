/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.sdxl3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.sdxl3.config.BgpPeersConfig;

import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class BgpPeersConfigTest {
    private static final ApplicationId APP_ID =
            new TestApplicationId(SdxL3.SDX_L3_APP);
    private static final String KEY = "key";

    public static final String NAME1 = "peer1";
    public static final String IP1_STRING = "10.0.1.1";
    public static final IpAddress IP1 = IpAddress.valueOf(IP1_STRING);
    public static final String INTF1 = "conn_point1";

    public static final String IP2_STRING = "10.0.2.1";
    public static final IpAddress IP2 = IpAddress.valueOf(IP2_STRING);
    public static final String INTF2 = "conn_point2";

    private static final String JSON_TREE = "[{" +
            "\"" + BgpPeersConfig.NAME + "\" : \"" + NAME1 + "\", " +
            "\"" + BgpPeersConfig.IP + "\" : \"" + IP1_STRING + "\", " +
            "\"" + BgpPeersConfig.INTERFACE + "\" : \"" + INTF1 + "\"}, " +
            "{ \"" + BgpPeersConfig.IP + "\" : \"" + IP2_STRING + "\", " +
            "\"" + BgpPeersConfig.INTERFACE + "\" : \"" + INTF2 + "\"}] ";

    BgpPeersConfig peersConfig = new BgpPeersConfig();

    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp()  throws Exception {
        JsonNode node = new ObjectMapper().readTree(JSON_TREE);
        peersConfig.init(APP_ID, KEY, node, mapper, delegate);
    }

    /**
     * Tests if peers can be retrieved from JSON.
     */
    @Test
    public void testBgpPeers() throws Exception {
        assertEquals(createPeers(), peersConfig.bgpPeers());
    }

    /**
     * Tests if interface name can be retrieved for given peer's IP.
     */
    @Test
    public void testGetInterfaceNameForPeer() throws Exception {
        assertEquals(INTF1, peersConfig.getInterfaceNameForPeer(IP1));
    }

    private Set<BgpPeersConfig.PeerConfig> createPeers() throws ConfigException {
        Set<BgpPeersConfig.PeerConfig> peers = Sets.newHashSet();

        peers.add(new BgpPeersConfig.PeerConfig(Optional.of(NAME1), IP1, INTF1));
        peers.add(new BgpPeersConfig.PeerConfig(Optional.empty(), IP2, INTF2));

        return peers;
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }
}

