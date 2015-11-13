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
package org.onos.vpls;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import javafx.util.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.app.ApplicationService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.sdnip.IntentSynchronizer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to create L2 broadcast overlay networks using VLAN.
 */
@Component(immediate = true)
public class Vpls {
    private static final String VPLS_APP = "org.onosproject.vpls";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    private final HostListener hostListener = new InternalHostListener();

    private IntentInstaller intentInstaller;

    private IntentSynchronizer intentSynchronizer;

    private ApplicationId appId;

    private LeadershipEventListener leadershipEventListener =
            new InnerLeadershipEventListener();
    private ControllerNode localControllerNode;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(VPLS_APP);

        localControllerNode = clusterService.getLocalNode();

        intentSynchronizer = new IntentSynchronizer(appId, intentService);
        intentSynchronizer.start();

        intentInstaller = new IntentInstaller(appId, intentSynchronizer);

        leadershipService.addListener(leadershipEventListener);
        leadershipService.runForLeadership(appId.name());

        applicationService.registerDeactivateHook(appId,
                                                  intentSynchronizer::removeIntents);

        hostService.addListener(hostListener);

        setupConnectivity();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);

        log.info("Stopped");
    }

    protected void setupConnectivity() {
        /*
         * Parse Configuration and get Connect Point by VlanId.
         */
        SetMultimap<VlanId, ConnectPoint> confCPointsByVlan = getConfigCPoints();

        /*
         * Check that configured Connect Points have hosts attached and
         * associate their Mac Address to the Connect Points configured.
         */
        SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> confHostPresentCPoint =
                pairAvailableHosts(confCPointsByVlan);

        /*
         * Create and submit intents between the Connect Points.
         * Intents for broadcast between all the configured Connect Points.
         * Intents for unicast between all the configured Connect Points with
         * hosts attached.
         */
        intentInstaller.installIntents(confHostPresentCPoint);
    }

    /**
     * Computes the list of configured interfaces with a VLAN Id.
     *
     * @return the interfaces grouped by vlan id
     */
    protected SetMultimap<VlanId, ConnectPoint> getConfigCPoints() {
        log.debug("Checking interface configuration");

        SetMultimap<VlanId, ConnectPoint> confCPointsByVlan =
                HashMultimap.create();

        interfaceService.getInterfaces()
                .forEach(intf -> confCPointsByVlan.put(intf.vlan(),
                                                       intf.connectPoint()));
        return confCPointsByVlan;
    }

    /**
     * Checks if for any ConnectPoint configured there's an host present
     * and in case it associate them together.
     *
     * @param confCPointsByVlan the configured ConnectPoints grouped by vlan id
     * @return the configured ConnectPoints with eventual hosts associated.
     */
    protected SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> pairAvailableHosts(
            SetMultimap<VlanId, ConnectPoint> confCPointsByVlan) {
        log.debug("Binding connected hosts mac addresses");

        SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> confHostPresentCPoint =
                HashMultimap.create();

        confCPointsByVlan.entries()
                .forEach(e -> bindMacAddr(e, confHostPresentCPoint));

        return confHostPresentCPoint;
    }

    private void bindMacAddr(Map.Entry<VlanId, ConnectPoint> e,
                             SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> confHostPresentCPoint) {
        VlanId vlanId = e.getKey();
        ConnectPoint cp = e.getValue();
        Set<Host> connectedHosts = hostService.getConnectedHosts(cp);
        if (!connectedHosts.isEmpty()) {
            connectedHosts.forEach(host -> {
                if (host.vlan().equals(vlanId)) {
                    confHostPresentCPoint.put(vlanId, new Pair<>(cp, host.mac()));
                } else {
                    confHostPresentCPoint.put(vlanId, new Pair<>(cp, null));
                }
            });
        } else {
            confHostPresentCPoint.put(vlanId, new Pair<>(cp, null));
        }
    }

    /**
     * A listener for Leadership Events.
     */
    private class InnerLeadershipEventListener
            implements LeadershipEventListener {

        @Override
        public void event(LeadershipEvent event) {
            log.debug("Leadership Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);

            if (!event.subject().topic().equals(appId.name())) {
                return;         // Not our topic: ignore
            }
            if (!Objects.equals(event.subject().leader(), localControllerNode.id())) {
                return;         // The event is not about this instance: ignore
            }

            switch (event.type()) {
                case LEADER_ELECTED:
                    log.info("Leader Elected");
                    intentSynchronizer.leaderChanged(true);
                    break;
                case LEADER_BOOTED:
                    log.info("Leader Lost Election");
                    intentSynchronizer.leaderChanged(false);
                    break;
                case LEADER_REELECTED:
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Listener for host events.
     */
    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            log.debug("Received HostEvent {}", event);
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_UPDATED:
                case HOST_REMOVED:
                    setupConnectivity();
                    break;
                default:
                    break;
            }
        }
    }
}
