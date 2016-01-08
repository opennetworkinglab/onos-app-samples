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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.component.ComponentService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.IntentSynchronizationAdminService;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component for the SDX-L3 application.
 */
@Component(immediate = true)
public class SdxL3  {

    public static final String SDX_L3_APP = "org.onosproject.sdxl3";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationAdminService intentSynchronizerAdmin;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentService componentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    protected ArpHandler arpHandler = null;

    private InternalPacketProcessor processor = null;

    private ApplicationId appId;

    private static List<String> components = new ArrayList<>();
    static {
        components.add("org.onosproject.routing.bgp.BgpSessionManager");
        components.add("org.onosproject.routing.impl.Router");
        components.add(org.onosproject.sdxl3.impl.PeerConnectivityManager.class.getName());
        components.add(org.onosproject.sdxl3.SdxL3Fib.class.getName());
    }

    @Activate
    protected void activate() {
        components.forEach(name -> componentService.activate(appId, name));

        appId = coreService.registerApplication(SDX_L3_APP);

        ApplicationId routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        BgpConfig bgpConfig =
                networkConfigService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        arpHandler = new ArpHandler(bgpConfig,
                edgeService,
                hostService,
                packetService,
                interfaceService);
        processor = new InternalPacketProcessor();
        packetService.addProcessor(processor, PacketProcessor.director(2));

        // TODO fix removing intents
        applicationService.registerDeactivateHook(appId,
                intentSynchronizerAdmin::removeIntents);

        log.info("SDX-L3 started");
    }

    @Deactivate
    protected void deactivate() {
        components.forEach(name -> componentService.deactivate(appId, name));

        packetService.removeProcessor(processor);

        log.info("SDX-L3 stopped");
    }

    private class InternalPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if (ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                arpHandler.processPacketIn(pkt);
            }
        }
    }

}
