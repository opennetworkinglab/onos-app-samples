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

package org.onosproject.sdxl3;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.RoutingServiceAdapter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.onosproject.routing.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests for SdxL3Fib.
 */
public class SdxL3FibTest extends AbstractIntentTest {

    private InterfaceService interfaceService;
    private SdxL3PeerService peerService;

    private static final String DEVICE1_ID = "of:0000000000000001";
    private static final String DEVICE2_ID = "of:0000000000000002";
    private static final String DEVICE3_ID = "of:0000000000000003";
    private static final String DEVICE4_ID = "of:0000000000000004";

    private static final String PEER1_IP = "192.168.10.1";
    private static final String PEER2_IP = "192.168.10.2";
    private static final String PEER3_IP = "192.168.10.3";
    private static final String PEER4_IP = "192.168.20.4";

    private static final String SPEAKER1_IP = "192.168.10.101";
    private static final String SPEAKER2_IP = "192.168.20.101";

    private static final String MAC1 = "00:00:00:00:00:01";
    private static final String MAC2 = "00:00:00:00:00:02";

    private Interface interface1;
    private Interface interface2;
    private Interface interface3;
    private Interface interface4;

    private static final ConnectPoint CONN_POINT1 = new ConnectPoint(
            DeviceId.deviceId(DEVICE1_ID),
            PortNumber.portNumber(1));

    private static final ConnectPoint CONN_POINT2 = new ConnectPoint(
            DeviceId.deviceId(DEVICE2_ID),
            PortNumber.portNumber(1));

    private static final ConnectPoint CONN_POINT3 = new ConnectPoint(
            DeviceId.deviceId(DEVICE3_ID),
            PortNumber.portNumber(1));

    private static final ConnectPoint CONN_POINT4 = new ConnectPoint(
            DeviceId.deviceId(DEVICE4_ID),
            PortNumber.portNumber(1));

    private SdxL3Fib sdxL3Fib;
    private IntentSynchronizationService intentSynchronizer;
    private final Set<Interface> interfaces = Sets.newHashSet();

    private static final ApplicationId APPID = TestApplicationId.create("sdxl3");

    private FibListener fibListener;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        interfaceService = createMock(InterfaceService.class);
        peerService = createMock(SdxL3PeerService.class);

        // These will set expectations on routingConfig and interfaceService
        setUpInterfaceService();
        setUpBgpPeers();

        replay(interfaceService);
        replay(peerService);

        intentSynchronizer = createMock(IntentSynchronizationService.class);

        sdxL3Fib = new SdxL3Fib();
        sdxL3Fib.routingService = new TestRoutingService();
        sdxL3Fib.coreService = new TestCoreService();
        sdxL3Fib.interfaceService = interfaceService;
        sdxL3Fib.intentSynchronizer = intentSynchronizer;
        sdxL3Fib.peerService = peerService;

        sdxL3Fib.activate();
    }

    /**
     * Sets up InterfaceService.
     */
    private void setUpInterfaceService() {
        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1.add(new InterfaceIpAddress(
                IpAddress.valueOf(SPEAKER1_IP),
                IpPrefix.valueOf("192.168.10.0/24")));
        interface1 = new Interface(CONN_POINT1,
                                   interfaceIpAddresses1, MacAddress.valueOf(MAC1),
                                   VlanId.NONE);
        interfaces.add(interface1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2.add(
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER1_IP),
                                       IpPrefix.valueOf("192.168.10.0/24")));
        interface2 = new Interface(CONN_POINT2,
                                   interfaceIpAddresses2, MacAddress.valueOf(MAC1),
                                   VlanId.NONE);
        interfaces.add(interface2);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3.add(
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER1_IP),
                                       IpPrefix.valueOf("192.168.10.0/24")));
        interface3 = new Interface(CONN_POINT3,
                                   interfaceIpAddresses3, MacAddress.valueOf(MAC1),
                                   VlanId.vlanId((short) 1));
        interfaces.add(interface3);

        Set<InterfaceIpAddress> interfaceIpAddresses4 = Sets.newHashSet();
        interfaceIpAddresses4.add(
                new InterfaceIpAddress(IpAddress.valueOf(SPEAKER2_IP),
                                       IpPrefix.valueOf("192.168.20.0/24")));
        interface4 = new Interface(CONN_POINT4,
                                   interfaceIpAddresses4, MacAddress.valueOf(MAC2),
                                   VlanId.NONE);
        interfaces.add(interface4);

        expect(interfaceService.getInterfacesByPort(CONN_POINT1)).andReturn(
                Collections.singleton(interface1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf(PEER1_IP)))
                .andReturn(interface1).anyTimes();
        expect(interfaceService.getInterfacesByPort(CONN_POINT2)).andReturn(
                Collections.singleton(interface2)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf(PEER4_IP)))
                .andReturn(interface1).anyTimes();
        expect(interfaceService.getInterfacesByPort(CONN_POINT4)).andReturn(
                Collections.singleton(interface4)).anyTimes();

        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
    }

    /**
     * Sets up BGP peers in external networks.
     */
    private void setUpBgpPeers() {
        expect(peerService.getInterfaceForPeer(IpAddress.valueOf(PEER1_IP)))
                .andReturn(interface1).anyTimes();

        expect(peerService.getInterfaceForPeer(IpAddress.valueOf(PEER2_IP)))
                .andReturn(interface2).anyTimes();

        expect(peerService.getInterfaceForPeer(IpAddress.valueOf(PEER3_IP)))
                .andReturn(interface3).anyTimes();

        expect(peerService.getInterfaceForPeer(IpAddress.valueOf(PEER4_IP)))
                .andReturn(null).anyTimes();
    }

    /**
     * Tests adding a FIB entry to the IntentSynchronizer. Peers within the same
     * subnet exist.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testFibAdd() {
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        FibEntry fibEntry = new FibEntry(prefix,
                                         Ip4Address.valueOf(PEER1_IP),
                                         MacAddress.valueOf(MAC1));

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                fibEntry.prefix());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf(MAC1));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(CONN_POINT2);
        ingressPoints.add(CONN_POINT3);
        ingressPoints.add(CONN_POINT4);

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(CONN_POINT1)
                        .constraints(SdxL3Fib.CONSTRAINTS)
                        .build();

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intent));
        replay(intentSynchronizer);

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        fibListener.update(Collections.singleton(fibUpdate), Collections.emptyList());

        verify(intentSynchronizer);
    }

    /**
     * Tests adding a FIB entry with to a next hop in a VLAN.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testFibAddWithVlan() {
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        FibEntry fibEntry = new FibEntry(prefix,
                                         Ip4Address.valueOf(PEER3_IP),
                                         MacAddress.valueOf(MAC1));

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(fibEntry.prefix())
                .matchVlanId(VlanId.ANY);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf(MAC1))
                .setVlanId(VlanId.vlanId((short) 1));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(CONN_POINT1);
        ingressPoints.add(CONN_POINT2);
        ingressPoints.add(CONN_POINT4);

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(CONN_POINT3)
                        .constraints(SdxL3Fib.CONSTRAINTS)
                        .build();

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intent));

        replay(intentSynchronizer);

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        fibListener.update(Collections.singleton(fibUpdate), Collections.emptyList());

        verify(intentSynchronizer);
    }

    /**
     * Tests updating a FIB entry.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testFibUpdate() {
        // Firstly add a route
        testFibAdd();

        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");

        // Start to construct a new route entry and new intent
        FibEntry fibEntryUpdate = new FibEntry(prefix,
                                               Ip4Address.valueOf(PEER2_IP),
                                               MacAddress.valueOf(MAC1));

        // Construct a new MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilderNew =
                DefaultTrafficSelector.builder();
        selectorBuilderNew.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                fibEntryUpdate.prefix());

        TrafficTreatment.Builder treatmentBuilderNew =
                DefaultTrafficTreatment.builder();
        treatmentBuilderNew.setEthDst(MacAddress.valueOf(MAC1));

        Set<ConnectPoint> ingressPointsNew = new HashSet<>();
        ingressPointsNew.add(CONN_POINT1);
        ingressPointsNew.add(CONN_POINT3);
        ingressPointsNew.add(CONN_POINT4);

        MultiPointToSinglePointIntent intentNew =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilderNew.build())
                        .treatment(treatmentBuilderNew.build())
                        .ingressPoints(ingressPointsNew)
                        .egressPoint(CONN_POINT2)
                        .constraints(SdxL3Fib.CONSTRAINTS)
                        .build();

        // Set up test expectation
        reset(intentSynchronizer);

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intentNew));
        replay(intentSynchronizer);

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE,
                                            fibEntryUpdate);
        fibListener.update(Collections.singletonList(fibUpdate),
                           Collections.emptyList());

        verify(intentSynchronizer);
    }

    /**
     * Tests deleting a FIB entry.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     */
    @Test
    public void testFibDelete() {
        // Firstly add a route
        testFibAdd();

        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");

        // Construct the existing route entry
        FibEntry fibEntry = new FibEntry(prefix, null, null);

        // Construct the existing MultiPointToSinglePoint intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                fibEntry.prefix());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf(MAC1));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(CONN_POINT2);
        ingressPoints.add(CONN_POINT3);
        ingressPoints.add(CONN_POINT4);

        MultiPointToSinglePointIntent addedIntent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(CONN_POINT1)
                        .constraints(SdxL3Fib.CONSTRAINTS)
                        .build();

        // Set up expectation
        reset(intentSynchronizer);
        // Setup the expected intents
        intentSynchronizer.withdraw(eqExceptId(addedIntent));
        replay(intentSynchronizer);

        // Send in the DELETE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.DELETE, fibEntry);
        fibListener.update(Collections.emptyList(), Collections.singletonList(fibUpdate));

        verify(intentSynchronizer);
    }

    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId getAppId(String name) {
            return APPID;
        }
    }

    private class TestRoutingService extends RoutingServiceAdapter {

        @Override
        public void addFibListener(FibListener fibListener) {
            SdxL3FibTest.this.fibListener = fibListener;
        }
    }
}

