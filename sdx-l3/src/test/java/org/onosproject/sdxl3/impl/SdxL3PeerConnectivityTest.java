/*
 * Copyright 2016 Open Networking Laboratory
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

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.sdxl3.SdxL3;
import org.onosproject.sdxl3.config.SdxProvidersConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.onosproject.routing.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests on setting up peer connectivity through SdxL3PeerManager.
 */
public class SdxL3PeerConnectivityTest extends AbstractIntentTest {
    private static final ApplicationId ROUTER_APPID =
            TestApplicationId.create("foo");
    private static final ApplicationId SDXL3_APPID =
            TestApplicationId.create("bar");

    private static final String DPID1 = "00:00:00:00:00:00:00:01";
    private static final String DPID2 = "00:00:00:00:00:00:00:02";
    private static final String DPID3 = "00:00:00:00:00:00:00:03";

    private static final String PEER1_IP = "192.168.10.1";
    private static final String PEER2_IP = "192.168.20.1";
    private static final String PEER3_IP = "192.168.10.2";
    private static final String SPEAKER1_IP = "192.168.10.101";
    private static final String SPEAKER2_IP = "192.168.20.101";
    private static final String PREFIX32 = "/32";
    private static final String PREFIX24 = "/24";

    private static final String INTERFACE_SW1_ETH1 = "s1-eth1";
    private static final String INTERFACE_SW2_ETH1 = "s2-eth1";
    private static final String INTERFACE_SW3_ETH1 = "s3-eth1";

    private static final String MAC1 = "00:00:00:00:00:01";
    private static final String MAC2 = "00:00:00:00:00:02";

    private SdxL3PeerManager peerManager;
    private CoreService coreService;
    private IntentSynchronizationService intentSynchronizer;
    private NetworkConfigService configService;
    private InterfaceService interfaceService;
    private NetworkConfigRegistry registry;

    private BgpConfig bgpConfig;
    private SdxProvidersConfig providersConfig;

    private Set<BgpConfig.BgpSpeakerConfig> bgpSpeakers;
    private Map<String, Interface> interfaces;

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
    private static final ConnectPoint SW2_ETH100 =
            new ConnectPoint(DEVICE2_ID, PortNumber.portNumber(100));

    // Ports where BGP peers are connected
    private static final ConnectPoint SW1_ETH1 =
            new ConnectPoint(DEVICE1_ID, PortNumber.portNumber(1));
    private static final ConnectPoint SW2_ETH1 =
            new ConnectPoint(DEVICE2_ID, PortNumber.portNumber(1));
    private static final ConnectPoint SW3_ETH1 =
            new ConnectPoint(DEVICE3_ID, PortNumber.portNumber(1));

    private final TrafficTreatment noTreatment =
            DefaultTrafficTreatment.emptyTreatment();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Setup services and configurations
        setupEnvironment();
        // Initiate the object used for testing
        initPeerManager();

        // Set expectations on bgpConfig and interfaceService
        interfaces = Collections.unmodifiableMap(setUpInterfaces());
        bgpSpeakers = setUpBgpSpeakers();
        setUpPeers();

        intentList = setUpIntentList();
    }

    /**
     * Initializes services and configurations for the testing environment.
     */
    private void setupEnvironment() {
        // Create mocks for configurations
        bgpConfig = createMock(BgpConfig.class);
        providersConfig = createMock(SdxProvidersConfig.class);

        // Create mocks for services
        coreService = new TestCoreService();
        configService = new TestNetworkConfigService();
        registry = new NetworkConfigRegistryAdapter();
        interfaceService = createMock(InterfaceService.class);
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
        public ApplicationId getAppId(String name) {
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
            registeredConfigs.put(SDXL3_APPID, providersConfig);
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
        Interface intfSw1Eth1 = new Interface(INTERFACE_SW1_ETH1,
                                              SW1_ETH1,
                                              Collections.singleton(ia1),
                                              MacAddress.valueOf(MAC1),
                                              VlanId.NONE);

        configuredInterfaces.put(INTERFACE_SW1_ETH1, intfSw1Eth1);

        InterfaceIpAddress ia2 =
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER2_IP),
                                       IpPrefix.valueOf(SPEAKER2_IP + PREFIX24));
        Interface intfSw2Eth1 = new Interface(INTERFACE_SW2_ETH1,
                                              SW2_ETH1,
                                              Collections.singleton(ia2),
                                              MacAddress.valueOf(MAC2),
                                              VlanId.NONE);
        configuredInterfaces.put(INTERFACE_SW2_ETH1, intfSw2Eth1);

        InterfaceIpAddress ia3 =
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER1_IP),
                                       IpPrefix.valueOf(SPEAKER1_IP + PREFIX24));
        Interface intfSw3Eth1 = new Interface(INTERFACE_SW3_ETH1,
                                              SW3_ETH1,
                                              Collections.singleton(ia3),
                                              MacAddress.valueOf(MAC1),
                                              VlanId.NONE);
        configuredInterfaces.put(INTERFACE_SW3_ETH1, intfSw3Eth1);

        // Set up the related expectations
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf(SPEAKER1_IP)))
                .andReturn(Collections.singleton(intfSw1Eth1)).anyTimes();
        // Always return the first matching even if not associated interface
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf(PEER1_IP)))
                .andReturn(intfSw1Eth1).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf(PEER3_IP)))
                .andReturn(intfSw1Eth1).anyTimes();
        expect(interfaceService.getInterfacesByIp(IpAddress.valueOf(SPEAKER2_IP)))
                .andReturn(Collections.singleton(intfSw2Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IpAddress.valueOf(PEER2_IP)))
                .andReturn(intfSw2Eth1).anyTimes();
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
     * Converts DPIDs of the form xx:xx:xx:xx:xx:xx:xx to OpenFlow provider
     * device URIs.
     *
     * @param dpid the DPID string to convert
     * @return the URI string for this device
     */
    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }

    /**
     * Sets up BGP speakers and the related expectations.
     *
     * @return configured BGP speakers as a map from speaker name to speaker
     */
    private Set<BgpConfig.BgpSpeakerConfig> setUpBgpSpeakers() {

        Set<IpAddress> connectedPeers = new HashSet<>();
        connectedPeers.add(IpAddress.valueOf(PEER1_IP));
        connectedPeers.add(IpAddress.valueOf(PEER3_IP));
        BgpConfig.BgpSpeakerConfig speaker1 = new BgpConfig.BgpSpeakerConfig(
                Optional.empty(), VlanId.NONE, SW1_ETH100, connectedPeers);

        BgpConfig.BgpSpeakerConfig speaker2 = new BgpConfig.BgpSpeakerConfig(
                Optional.empty(), VlanId.NONE,
                SW2_ETH100, Collections.singleton(IpAddress.valueOf(PEER2_IP)));

        Set<BgpConfig.BgpSpeakerConfig> speakers = Sets.newHashSet();
        speakers.add(speaker1);
        speakers.add(speaker2);

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

        // Set up the related expectations
        expect(providersConfig.getPortForPeer(IpAddress.valueOf(PEER1_IP)))
                .andReturn(SW1_ETH1).anyTimes();
        expect(providersConfig.
                getInterfaceNameForPeer(IpAddress.valueOf(PEER1_IP)))
                .andReturn(INTERFACE_SW1_ETH1).anyTimes();
        expect(providersConfig.getPortForPeer(IpAddress.valueOf(PEER2_IP)))
                .andReturn(null).anyTimes();
        expect(providersConfig
                .getInterfaceNameForPeer(IpAddress.valueOf(PEER2_IP)))
                .andReturn(null).anyTimes();
        expect(providersConfig.getPortForPeer(IpAddress.valueOf(PEER3_IP)))
                .andReturn(SW3_ETH1).anyTimes();
        expect(providersConfig
                .getInterfaceNameForPeer(IpAddress.valueOf(PEER3_IP)))
                .andReturn(INTERFACE_SW3_ETH1).anyTimes();
        replay(providersConfig);
    }

    /**
     * Sets up expected point to point intent list.
     *
     * @return point to point intent list
     */
    private List<PointToPointIntent> setUpIntentList() {
        intentList = new ArrayList<>();

        setUpBgpIntents();
        setUpIcmpIntents();

        return intentList;
    }

    /**
     * Constructs a BGP intent and put it into the intentList.
     * <p/>
     * The purpose of this method is too simplify the setUpBgpIntents() method,
     * and to make the setUpBgpIntents() easy to read.
     *
     * @param srcPrefix source IP prefix to match
     * @param dstPrefix destination IP prefix to match
     * @param srcTcpPort source TCP port to match
     * @param dstTcpPort destination TCP port to match
     * @param srcConnectPoint source connect point for PointToPointIntent
     * @param dstConnectPoint destination connect point for PointToPointIntent
     */
    private void bgpPathintentConstructor(String srcPrefix, String dstPrefix,
                                          Short srcTcpPort, Short dstTcpPort,
                                          ConnectPoint srcConnectPoint, ConnectPoint dstConnectPoint) {

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchIPSrc(IpPrefix.valueOf(srcPrefix))
                .matchIPDst(IpPrefix.valueOf(dstPrefix));

        if (srcTcpPort != null) {
            builder.matchTcpSrc(TpPort.tpPort(srcTcpPort));
        }
        if (dstTcpPort != null) {
            builder.matchTcpDst(TpPort.tpPort(dstTcpPort));
        }

        Key key = Key.of(srcPrefix.split("/")[0] + "-" + dstPrefix.split("/")[0]
                                 + "-" + ((srcTcpPort == null) ? "dst" : "src"), SDXL3_APPID);

        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(SDXL3_APPID)
                .key(key)
                .selector(builder.build())
                .treatment(noTreatment)
                .ingressPoint(srcConnectPoint)
                .egressPoint(dstConnectPoint)
                .build();

        intentList.add(intent);
    }

    /**
     * Sets up intents for BGP paths.
     */
    private void setUpBgpIntents() {

        Short bgpPort = 179;

        // Start to build intents between BGP speaker1 and BGP peer1
        bgpPathintentConstructor(
                SPEAKER1_IP + PREFIX32, PEER1_IP + PREFIX32, null, bgpPort,
                SW1_ETH100, SW1_ETH1);
        bgpPathintentConstructor(
                SPEAKER1_IP + PREFIX32, PEER1_IP + PREFIX32, bgpPort, null,
                SW1_ETH100, SW1_ETH1);
        bgpPathintentConstructor(
                PEER1_IP + PREFIX32, SPEAKER1_IP + PREFIX32, null, bgpPort,
                SW1_ETH1, SW1_ETH100);
        bgpPathintentConstructor(
                PEER1_IP + PREFIX32, SPEAKER1_IP + PREFIX32, bgpPort, null,
                SW1_ETH1, SW1_ETH100);

        // Start to build intents between BGP speaker2 and BGP peer2
        bgpPathintentConstructor(
                SPEAKER2_IP + PREFIX32, PEER2_IP + PREFIX32, null, bgpPort,
                SW2_ETH100, SW2_ETH1);
        bgpPathintentConstructor(
                SPEAKER2_IP + PREFIX32, PEER2_IP + PREFIX32, bgpPort, null,
                SW2_ETH100, SW2_ETH1);
        bgpPathintentConstructor(
                PEER2_IP + PREFIX32, SPEAKER2_IP + PREFIX32, null, bgpPort,
                SW2_ETH1, SW2_ETH100);
        bgpPathintentConstructor(
                PEER2_IP + PREFIX32, SPEAKER2_IP + PREFIX32, bgpPort, null,
                SW2_ETH1, SW2_ETH100);

        // Start to build intents between BGP speaker1 and BGP peer3
        bgpPathintentConstructor(
                SPEAKER1_IP + PREFIX32, PEER3_IP + PREFIX32, null, bgpPort,
                SW1_ETH100, SW3_ETH1);
        bgpPathintentConstructor(
                SPEAKER1_IP + PREFIX32, PEER3_IP + PREFIX32, bgpPort, null,
                SW1_ETH100, SW3_ETH1);
        bgpPathintentConstructor(
                PEER3_IP + PREFIX32, SPEAKER1_IP + PREFIX32, null, bgpPort,
                SW3_ETH1, SW1_ETH100);
        bgpPathintentConstructor(
                PEER3_IP + PREFIX32, SPEAKER1_IP + PREFIX32, bgpPort, null,
                SW3_ETH1, SW1_ETH100);
    }

    /**
     * Constructs a BGP intent and put it into the intentList.
     * <p/>
     * The purpose of this method is too simplify the setUpBgpIntents() method,
     * and to make the setUpBgpIntents() easy to read.
     *
     * @param srcPrefix source IP prefix to match
     * @param dstPrefix destination IP prefix to match
     * @param srcConnectPoint source connect point for PointToPointIntent
     * @param dstConnectPoint destination connect point for PointToPointIntent
     */
    private void icmpPathintentConstructor(String srcPrefix, String dstPrefix,
                                           ConnectPoint srcConnectPoint, ConnectPoint dstConnectPoint) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPSrc(IpPrefix.valueOf(srcPrefix))
                .matchIPDst(IpPrefix.valueOf(dstPrefix))
                .build();

        Key key = Key.of(srcPrefix.split("/")[0] + "-" + dstPrefix.split("/")[0]
                                 + "-" + "icmp", SDXL3_APPID);

        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(SDXL3_APPID)
                .key(key)
                .selector(selector)
                .treatment(noTreatment)
                .ingressPoint(srcConnectPoint)
                .egressPoint(dstConnectPoint)
                .build();

        intentList.add(intent);
    }

    /**
     * Sets up intents for ICMP paths.
     */
    private void setUpIcmpIntents() {
        // Start to build intents between BGP speaker1 and BGP peer1
        icmpPathintentConstructor(
                SPEAKER1_IP + PREFIX32, PEER1_IP + PREFIX32, SW1_ETH100, SW1_ETH1);
        icmpPathintentConstructor(
                PEER1_IP + PREFIX32, SPEAKER1_IP + PREFIX32, SW1_ETH1, SW1_ETH100);

        // Start to build intents between BGP speaker1 and BGP peer2
        icmpPathintentConstructor(
                SPEAKER2_IP + PREFIX32, PEER2_IP + PREFIX32, SW2_ETH100, SW2_ETH1);
        icmpPathintentConstructor(
                PEER2_IP + PREFIX32, SPEAKER2_IP + PREFIX32, SW2_ETH1, SW2_ETH100);

        icmpPathintentConstructor(
                SPEAKER1_IP + PREFIX32, PEER3_IP + PREFIX32, SW1_ETH100, SW3_ETH1);
        icmpPathintentConstructor(
                PEER3_IP + PREFIX32, SPEAKER1_IP + PREFIX32, SW3_ETH1, SW1_ETH100);
    }

    /**
     * Tests whether manager can set up correct BGP and
     * ICMP intents according to specific configuration.
     * <p/>
     * Two tricky cases included in the configuration are: 2 peers on a same
     * switch port, peer on the same switch with BGPd.
     */
    @Test
    public void testConnectionSetup() {
        reset(intentSynchronizer);
        // Setup the expected intents
        for (Intent intent : intentList) {
            intentSynchronizer.submit(eqExceptId(intent));
        }
        replay(intentSynchronizer);

        // Running the interface to be tested.
        peerManager.activate();

        verify(intentSynchronizer);
    }

    /**
     *  Tests a corner case, when there are no interfaces in the configuration.
     */
    @Test
    public void testNullInterfaces() {
        reset(interfaceService);

        expect(interfaceService.getInterfaces()).andReturn(Sets.newHashSet())
                .anyTimes();
        expect(interfaceService.getInterfacesByPort(anyObject()))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getInterfacesByIp(anyObject()))
                .andReturn(Collections.emptySet()).anyTimes();
        expect(interfaceService.getMatchingInterface(anyObject()))
                .andReturn(null).anyTimes();
        replay(interfaceService);

        reset(intentSynchronizer);
        replay(intentSynchronizer);
        peerManager.activate();
        verify(intentSynchronizer);
    }

    /**
     *  Tests a corner case, when there is no BGP speakers in the configuration.
     */
    @Test
    public void testNullBgpSpeakers() {
        reset(bgpConfig);
        expect(bgpConfig.bgpSpeakers()).andReturn(Collections.emptySet()).anyTimes();
        replay(bgpConfig);

        reset(intentSynchronizer);
        replay(intentSynchronizer);
        peerManager.activate();
        verify(intentSynchronizer);
    }

    /**
     * Tests a corner case, when there is no Interface configured for one BGP
     * peer.
     */
    @Test
    public void testNoPeerInterface() {
        IpAddress ip = IpAddress.valueOf("1.1.1.1");
        bgpSpeakers.clear();
        bgpSpeakers.add(new BgpConfig.BgpSpeakerConfig(Optional.of("foo"),
                VlanId.NONE, SW1_ETH100, Collections.singleton(ip)));
        reset(interfaceService);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expect(interfaceService.getMatchingInterface(ip)).andReturn(null).anyTimes();
        replay(interfaceService);
        reset(providersConfig);
        expect(providersConfig.getPortForPeer(ip)).andReturn(null);
        expect(providersConfig.getInterfaceNameForPeer(ip)).andReturn(null);
        replay(providersConfig);

        // We don't expect any intents in this case
        reset(intentSynchronizer);
        replay(intentSynchronizer);
        peerManager.activate();
        verify(intentSynchronizer);
    }
}
