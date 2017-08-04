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

package org.onosproject.sdxl3.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.sdxl3.SdxL3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit tests for SdxParticipantsConfig class.
 */
public class SdxParticipantsConfigTest {
    private static final ApplicationId APP_ID =
            new TestApplicationId(SdxL3.SDX_L3_APP);
    private static final String KEY = "key";

    private static final String NAME1 = "peer1";
    private static final String IP_STRING1 = "10.0.1.1";
    private static final IpAddress IP1 = IpAddress.valueOf(IP_STRING1);
    private static final String PORT_STRING1 = "of:00000000000000a1/1";
    private static final ConnectPoint PORT1 =
            ConnectPoint.deviceConnectPoint(PORT_STRING1);
    private static final String INTF_NAME1 = "conn_point1";

    private static final String IP_STRING2 = "10.0.2.1";
    private static final IpAddress IP2 = IpAddress.valueOf(IP_STRING2);
    private static final String PORT_STRING2 = "of:00000000000000a1/2";
    private static final ConnectPoint PORT2 =
            ConnectPoint.deviceConnectPoint(PORT_STRING2);
    private static final String INTF_NAME2 = "conn_point2";

    private static final String NAME3 = "new_peer";
    private static final String IP_STRING3 = "10.0.3.1";
    private static final String PORT_STRING3 = "of:00000000000000a1/3";
    private static final ConnectPoint PORT3 =
            ConnectPoint.deviceConnectPoint(PORT_STRING3);
    private static final IpAddress IP3 = IpAddress.valueOf(IP_STRING3);
    private static final String INTF_NAME3 = "conn_point3";

    private static final String JSON_TREE = "{\"" + SdxParticipantsConfig.PEERS + "\"" +
            ": [{\"" + SdxParticipantsConfig.NAME + "\" : \"" + NAME1 + "\", " +
            "\"" + SdxParticipantsConfig.IP + "\" : \"" + IP_STRING1 + "\", " +
            "\"" + SdxParticipantsConfig.CONN_POINT + "\" : \"" + PORT_STRING1 + "\", " +
            "\"" + SdxParticipantsConfig.INTF_NAME + "\" : \"" + INTF_NAME1 + "\"}, " +
            "{ \"" + SdxParticipantsConfig.IP + "\" : \"" + IP_STRING2 + "\", " +
            "\"" + SdxParticipantsConfig.CONN_POINT + "\" : \"" + PORT_STRING2 + "\", " +
            "\"" + SdxParticipantsConfig.INTF_NAME + "\" : \"" + INTF_NAME2 + "\"}]}";

    private static final String EMPTY_JSON_TREE = "{ }";

    private Set<SdxParticipantsConfig.PeerConfig> peers = new HashSet<>();
    private SdxParticipantsConfig.PeerConfig peer1;

    private SdxParticipantsConfig peersConfig = new SdxParticipantsConfig();
    private SdxParticipantsConfig emptyPeersConfig = new SdxParticipantsConfig();

    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp()  throws Exception {
        peers = createPeers();
        JsonNode node = new ObjectMapper().readTree(JSON_TREE);
        peersConfig.init(APP_ID, KEY, node, mapper, delegate);
        JsonNode emptyTree = new ObjectMapper().readTree(EMPTY_JSON_TREE);
        emptyPeersConfig.init(APP_ID, KEY, emptyTree, mapper, delegate);
    }

    /**
     * Tests if peers can be retrieved from JSON.
     */
    @Test
    public void testBgpPeers() throws Exception {
        assertEquals(peers, peersConfig.bgpPeers());
    }

    /**
     * Tests if interface name can be retrieved for given peer's IP.
     */
    @Test
    public void testGetInterfaceNameForPeer() throws Exception {
        assertEquals(INTF_NAME1, peersConfig.getInterfaceNameForPeer(IP1));
    }

    /**
     * Tests addition of new peer.
     */
    @Test
    public void testAddPeer() throws Exception {
        int initialSize = peersConfig.bgpPeers().size();
        SdxParticipantsConfig.PeerConfig newPeer = createNewPeer();
        peersConfig.addPeer(newPeer);
        assertEquals(initialSize + 1, peersConfig.bgpPeers().size());
        peers.add(newPeer);
        assertEquals(peers, peersConfig.bgpPeers());
    }

    /**
     * Tests addition of new peer to empty configuration.
     */
    @Test
    public void testAddPeerToEmpty() throws Exception {
        emptyPeersConfig.addPeer(createNewPeer());
        assertFalse(emptyPeersConfig.bgpPeers().isEmpty());
    }

    /**
     * Tests getting peer configuration based on given name.
     */
    @Test
    public void testGetPeerForName() throws Exception {
        assertEquals(peer1, peersConfig.getPeerForName(Optional.of(NAME1)));
    }

    /**
     * Tests getting peer configuration based on given IP.
     */
    @Test
    public void testGetPeerForIp() throws Exception {
        assertEquals(peer1, peersConfig.getPeerForIp(IP1));
    }

    /**
     * Tests removing peer details based on given IP.
     */
    @Test
    public void testRemovePeer() throws Exception {
        int initialSize = peersConfig.bgpPeers().size();
        peersConfig.removePeer(IP1);
        assertEquals(initialSize - 1, peersConfig.bgpPeers().size());
        peers.remove(peer1);
        assertEquals(peers, peersConfig.bgpPeers());    }

    private Set<SdxParticipantsConfig.PeerConfig> createPeers() {
        Set<SdxParticipantsConfig.PeerConfig> peers = Sets.newHashSet();

        peer1 = new SdxParticipantsConfig.
                PeerConfig(Optional.of(NAME1), IP1, PORT1, INTF_NAME1);
        peers.add(peer1);
        peers.add(new SdxParticipantsConfig.
                PeerConfig(Optional.empty(), IP2, PORT2, INTF_NAME2));

        return peers;
    }

    private SdxParticipantsConfig.PeerConfig createNewPeer() {
        return new SdxParticipantsConfig.
                PeerConfig(Optional.of(NAME3), IP3, PORT3, INTF_NAME3);
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }
}

