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

package org.onosproject.sdxl3.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.sdxl3.SdxL3;
import org.onosproject.sdxl3.config.SdxParticipantsConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.onosproject.sdxl3.impl.SdxL3PeerConnectivityTest.dpidToUri;

/**
 * Unit tests for SdxL3PeerManager for the administration of peer details.
 */
public class SdxL3PeerAdministrationTest extends AbstractIntentTest {
    private static final ApplicationId ROUTER_APPID =
            TestApplicationId.create("foo");
    private static final ApplicationId SDXL3_APPID =
            TestApplicationId.create("bar");

    private static final String DPID1 = "00:00:00:00:00:00:00:01";
    private static final String DPID2 = "00:00:00:00:00:00:00:02";
    private static final String DPID3 = "00:00:00:00:00:00:00:03";

    private static final String PEER_IP = "192.168.10.1";
    private static final String NEW_PEER1_IP = "192.168.10.2";
    private static final String NEW_PEER2_IP = "192.168.30.1";
    private static final String SPEAKER1_IP = "192.168.10.101";
    private static final String NON_MATCHING_IP = "192.168.20.101";

    private static final String PREFIX24 = "/24";

    private static final String INTERFACE_SW1_ETH1 = "s1-eth1";
    private static final String INTERFACE_SW2_ETH1 = "s2-eth1";
    private static final String INTERFACE_SW3_ETH1 = "s3-eth1";

    private static final String MAC1 = "00:00:00:00:00:01";

    private static final String PEER1_NAME = "router1";
    private static final String NEW_PEER_NAME = "new-router";

    private SdxL3PeerManager peerManager;
    private CoreService coreService;
    private IntentSynchronizationService intentSynchronizer;
    private NetworkConfigService configService;
    private InterfaceService interfaceService;
    private NetworkConfigRegistry registry;

    private BgpConfig bgpConfig;
    private SdxParticipantsConfig participantsConfig;

    private Set<BgpConfig.BgpSpeakerConfig> bgpSpeakers;
    private Map<String, Interface> interfaces;

    SdxParticipantsConfig.PeerConfig newPeer = createNewPeer();

    private List<PointToPointIntent> intentList;

    private static final DeviceId DEVICE1_ID =
            DeviceId.deviceId(dpidToUri(DPID1));
    private static final DeviceId DEVICE2_ID =
            DeviceId.deviceId(dpidToUri(DPID2));
    private static final DeviceId DEVICE3_ID =
            DeviceId.deviceId(dpidToUri(DPID3));

    // Ports where BGP speakers are connected
    private static final ConnectPoint SW1_ETH100 =
            new ConnectPoint(DEVICE1_ID, PortNumber.portNumber(100));

    // Ports where BGP peers are connected
    private static final ConnectPoint SW1_ETH1 =
            new ConnectPoint(DEVICE1_ID, PortNumber.portNumber(1));
    private static final ConnectPoint SW2_ETH1 =
            new ConnectPoint(DEVICE2_ID, PortNumber.portNumber(1));
    private static final ConnectPoint SW3_ETH1 =
            new ConnectPoint(DEVICE3_ID, PortNumber.portNumber(1));

    // Interface configured for peer
    Interface intfSw1Eth1;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // Setup services and configurations
        setupEnvironment();
        // Initiate the object used for testing
        initPeerManager();

        // Set expectations on bgpConfig and interfaceService
        interfaces = Collections.unmodifiableMap(setUpInterfaces());
        bgpSpeakers = setUpBgpSpeakers();
        setUpPeers();
    }

    /**
     * Initializes services and configurations for the testing environment.
     */
    private void setupEnvironment() {
        // Create mocks for configurations
        bgpConfig = createMock(BgpConfig.class);
        participantsConfig = createMock(SdxParticipantsConfig.class);

        // Create mocks for services
        coreService = new TestCoreService();
        configService = new TestNetworkConfigService();
        registry = new NetworkConfigRegistryAdapter();
        interfaceService = createMock(InterfaceService.class);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().anyTimes();
        intentSynchronizer = createMock(IntentSynchronizationService.class);
    }

    /**
     * Mocks the CoreService.
     */
    private class TestCoreService extends CoreServiceAdapter {
        private final Map<String, ApplicationId> registeredApps =
                new HashMap<>();

        public TestCoreService() {
            registeredApps.put(RoutingService.ROUTER_APP_ID, ROUTER_APPID);
            registeredApps.put(SdxL3.SDX_L3_APP, SDXL3_APPID);
        }

        @Override
        public ApplicationId registerApplication(String name) {
            return registeredApps.get(name);
        }
    }

    /**
     * Mocks the NetworkConfigService.
     */
    private class TestNetworkConfigService extends NetworkConfigServiceAdapter {
        private final Map<ApplicationId, Config> registeredConfigs
                = new HashMap<>();

        public TestNetworkConfigService() {
            registeredConfigs.put(ROUTER_APPID, bgpConfig);
            registeredConfigs.put(SDXL3_APPID, participantsConfig);
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
                return (C) registeredConfigs.get(subject);
        }

        @Override
        public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
            return (C) registeredConfigs.get(subject);
        }
    }

    /**
     * Initializes SdxL3PeerManager object.
     */
    private void initPeerManager() {
        peerManager = new SdxL3PeerManager();
        peerManager.coreService = coreService;
        peerManager.configService = configService;
        peerManager.interfaceService = interfaceService;
        peerManager.registry = registry;
        peerManager.intentSynchronizer = intentSynchronizer;
    }

    /**
     * Sets up logical interfaces, which emulate the configured interfaces
     * in the SDX.
     *
     * @return configured interfaces as a map from interface name to Interface
     */
    private Map<String, Interface> setUpInterfaces() {

        Map<String, Interface> configuredInterfaces = new HashMap<>();

        InterfaceIpAddress ia1 =
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER1_IP),
                                       IpPrefix.valueOf(SPEAKER1_IP + PREFIX24));
        intfSw1Eth1 = new Interface(INTERFACE_SW1_ETH1,
                                              SW1_ETH1,
                                              ImmutableList.of(ia1),
                                              MacAddress.valueOf(MAC1),
                                              VlanId.NONE);

        configuredInterfaces.put(INTERFACE_SW1_ETH1, intfSw1Eth1);
        InterfaceIpAddress ia2 =
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER1_IP),
                                       IpPrefix.valueOf(SPEAKER1_IP + PREFIX24));
        Interface intfSw2Eth1 = new Interface(INTERFACE_SW2_ETH1,
                                              SW2_ETH1,
                                              ImmutableList.of(ia2),
                                              MacAddress.valueOf(MAC1),
                                              VlanId.NONE);

        configuredInterfaces.put(INTERFACE_SW1_ETH1, intfSw1Eth1);

        InterfaceIpAddress ia3 =
                new InterfaceIpAddress(IpAddress.valueOf(NON_MATCHING_IP),
                                       IpPrefix.valueOf(NON_MATCHING_IP + PREFIX24));
        Interface intfSw3Eth1 = new Interface(INTERFACE_SW3_ETH1,
                                              SW3_ETH1,
                                              ImmutableList.of(ia3),
                                              MacAddress.valueOf(MAC1),
                                              VlanId.NONE);
        configuredInterfaces.put(INTERFACE_SW3_ETH1, intfSw3Eth1);

        // Set up the related expectations
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf(SPEAKER1_IP)))
                .andReturn(Collections.singleton(intfSw1Eth1)).anyTimes();
        // Always return the first matching even if not associated interface
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf(PEER_IP)))
                .andReturn(intfSw1Eth1).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf(NEW_PEER1_IP)))
                .andReturn(intfSw1Eth1).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf(NEW_PEER2_IP)))
                .andReturn(null).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW1_ETH1))
                .andReturn(Collections.singleton(intfSw1Eth1)).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW2_ETH1))
                .andReturn(Collections.singleton(intfSw2Eth1)).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW3_ETH1))
                .andReturn(Collections.singleton(intfSw3Eth1)).anyTimes();
        expect(interfaceService.getInterfacesByPort(new ConnectPoint(
                DeviceId.deviceId(dpidToUri("00:00:00:00:00:00:01:00")),
                PortNumber.portNumber(1))))
                .andReturn(null).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(
                Sets.newHashSet(configuredInterfaces.values())).anyTimes();
        replay(interfaceService);
        return configuredInterfaces;
    }

    /**
     * Sets up BGP speakers and the related expectations.
     *
     * @return configured BGP speakers as a map from speaker name to speaker
     */
    private Set<BgpConfig.BgpSpeakerConfig> setUpBgpSpeakers() {

        Set<IpAddress> connectedPeers = new HashSet<>();
        connectedPeers.add(IpAddress.valueOf(PEER_IP));
        connectedPeers.add(IpAddress.valueOf(NEW_PEER1_IP));
        BgpConfig.BgpSpeakerConfig speaker1 = new BgpConfig.BgpSpeakerConfig(
                Optional.empty(), VlanId.NONE, SW1_ETH100, connectedPeers);

        Set<BgpConfig.BgpSpeakerConfig> speakers = Sets.newHashSet();
        speakers.add(speaker1);

        // Set up the related expectations
        expect(bgpConfig.bgpSpeakers()).andReturn(speakers).anyTimes();
        replay(bgpConfig);

        return speakers;
    }

    /**
     * Sets up BGP daemon peers.
     *
     * @return configured BGP peers as a MAP from peer IP address to BgpPeer
     */
    private void setUpPeers() {
        SdxParticipantsConfig.PeerConfig peer1 =
                new SdxParticipantsConfig.PeerConfig(Optional.of(PEER1_NAME),
                                                     IpAddress.valueOf(PEER_IP),
                                                     SW1_ETH1,
                                                     INTERFACE_SW1_ETH1);

        // Set up the related expectations
        expect(participantsConfig.getPortForPeer(IpAddress.valueOf(PEER_IP)))
                .andReturn(SW1_ETH1).anyTimes();
        expect(participantsConfig.
                getInterfaceNameForPeer(IpAddress.valueOf(PEER_IP)))
                .andReturn(INTERFACE_SW1_ETH1).anyTimes();
        expect(participantsConfig.getPeerForName(Optional.of(PEER1_NAME)))
                .andReturn(peer1).anyTimes();
        expect(participantsConfig.getPeerForIp(IpAddress.valueOf(PEER_IP)))
                .andReturn(peer1).anyTimes();

        // Set up expectations for peers that will be added
        expect(participantsConfig.
                getInterfaceNameForPeer(IpAddress.valueOf(NEW_PEER1_IP)))
                .andReturn(null).anyTimes();
        expect(participantsConfig.getPortForPeer(IpAddress.valueOf(NEW_PEER1_IP)))
                .andReturn(null).anyTimes();
        expect(participantsConfig.getPeerForIp(IpAddress.valueOf(NEW_PEER1_IP)))
                .andReturn(null).anyTimes();
        expect(participantsConfig.
                getInterfaceNameForPeer(IpAddress.valueOf(NEW_PEER2_IP)))
                .andReturn(null).anyTimes();
        expect(participantsConfig.getPortForPeer(IpAddress.valueOf(NEW_PEER2_IP)))
                .andReturn(null).anyTimes();
        expect(participantsConfig.getPeerForName(Optional.of(NEW_PEER_NAME)))
                .andReturn(null).anyTimes();
        expect(participantsConfig.node()).andReturn(null).anyTimes();
    }

    /**
     * Tests the addition of peer when no BGP configuratin exists.
     */
    @Test(expected = ItemNotFoundException.class)
    public void testAddPeerWithNoBgpConfig() {
        replay(participantsConfig);
        // Reset NetworkConfigService
        peerManager.configService = new NetworkConfigServiceAdapter();
        peerManager.activate();

        peerManager.addPeerDetails(newPeer.name().get(),
                                   newPeer.ip(),
                                   newPeer.connectPoint(),
                                   newPeer.interfaceName());
    }

    /**
     * Tests the addition of peer whose IP is not declared in BGP config.
     */
    @Test(expected = ItemNotFoundException.class)
    public void testAddPeerUknownIp() {
        replay(participantsConfig);

        peerManager.activate();

        peerManager.addPeerDetails(newPeer.name().get(),
                                   IpAddress.valueOf(NEW_PEER2_IP),
                                   newPeer.connectPoint(),
                                   newPeer.interfaceName());
    }

    /**
     * Tests the association of peer to a non defined interface.
     */
    @Test(expected = ItemNotFoundException.class)
    public void testAddPeerToUnknownInterface() {
        replay(participantsConfig);

        peerManager.activate();

        peerManager.addPeerDetails(newPeer.name().get(),
                                   newPeer.ip(),
                                   newPeer.connectPoint(),
                                   "dummy-interface");
    }

    /**
     * Tests the association of peer to non matching interface.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddPeerToNonMatchingInterface() {
        replay(participantsConfig);

        peerManager.activate();

        peerManager.addPeerDetails(newPeer.name().get(),
                                   newPeer.ip(),
                                   SW3_ETH1,
                                   INTERFACE_SW3_ETH1);
    }

    /**
     * Tests the association of an already registered peer.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddAlreadyRegisteredPeer() {
        replay(participantsConfig);

        peerManager.activate();

        peerManager.addPeerDetails(newPeer.name().get(),
                                   IpAddress.valueOf(PEER_IP),
                                   newPeer.connectPoint(),
                                   newPeer.interfaceName());
    }

    /**
     * Tests the association of peer whose name is in use.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddPeerWithNameInUse() {
        replay(participantsConfig);

        peerManager.activate();

        peerManager.addPeerDetails(PEER1_NAME,
                                   newPeer.ip(),
                                   newPeer.connectPoint(),
                                   newPeer.interfaceName());
    }

    /**
     * Tests the addition of peer whose IP is not declared in BGP config.
     */
    @Test
    public void testAddPeerDetailsSuccess() {
        participantsConfig.addPeer(newPeer);
        expectLastCall().once();
        replay(participantsConfig);

        peerManager.activate();

        peerManager.addPeerDetails(newPeer.name().get(),
                                   newPeer.ip(),
                                   newPeer.connectPoint(),
                                   newPeer.interfaceName());
        verify(participantsConfig);
    }

    private SdxParticipantsConfig.PeerConfig createNewPeer() {
        return new SdxParticipantsConfig.PeerConfig(Optional.of(NEW_PEER_NAME),
                                                    IpAddress.valueOf(NEW_PEER1_IP),
                                                    SW2_ETH1,
                                                    INTERFACE_SW2_ETH1);
    }

    /**
     * Tests the removal of peer when no BGP configuration exists.
     */
    @Test(expected = ItemNotFoundException.class)
    public void testRemovePeerWithNoBgpConfig() {
        replay(participantsConfig);
        // Reset NetworkConfigService
        peerManager.configService = new NetworkConfigServiceAdapter();
        peerManager.activate();

        peerManager.removePeerDetails(IpAddress.valueOf(PEER_IP));
    }

    /**
     * Tests the removal of peer when no details have been specified.
     */
    @Test(expected = ItemNotFoundException.class)
    public void testRemoveNonFoundPeer() {
        replay(participantsConfig);

        peerManager.activate();

        peerManager.removePeerDetails(IpAddress.valueOf(NEW_PEER1_IP));
    }

    /**
     * Tests the removal of peer.
     */
    @Test
    public void testRemovePeerDetailsSuccess() {
        participantsConfig.removePeer(IpAddress.valueOf(PEER_IP));
        expectLastCall().once();
        replay(participantsConfig);

        peerManager.activate();

        peerManager.removePeerDetails(IpAddress.valueOf(PEER_IP));
        verify(participantsConfig);
    }

    /**
     * Tests the retrieval of IP addresses for all BGP peers defined.
     */
    @Test
    public void testGetPeerAddresses() {
        replay(participantsConfig);

        peerManager.activate();

        List<IpAddress> expectedAddresses = new ArrayList<>();
        expectedAddresses.add(IpAddress.valueOf(PEER_IP));
        expectedAddresses.add(IpAddress.valueOf(NEW_PEER1_IP));
        Collections.sort(expectedAddresses);

        List<IpAddress> actualAddresses =
                peerManager.getPeerAddresses(bgpConfig);
        Collections.sort(actualAddresses);

        assertEquals(expectedAddresses, actualAddresses);
    }

    /**
     * Tests the retrieval of interface used as connection point to peer.
     */
    @Test
    public void testGetInterfaceForPeer() {
        replay(participantsConfig);

        peerManager.activate();

        assertEquals(intfSw1Eth1, peerManager
                .getInterfaceForPeer(IpAddress.valueOf(PEER_IP)));
    }
}
