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
package org.onosproject.ipfix;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.statistic.StatisticStore;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * OpenFlow to IPFIX Manager.
 */
@Component(immediate = true)
public class IpfixManager {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StatisticStore statisticStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ApplicationId appId;

    protected FlowRemovedListener flowRemovedListener = null;
    protected PortStatsListener portStatsListener = null;
    protected IpfixSender ipfixSender = null;

    private static final boolean R_FWD_FLOWS_EXPORT = true;
    @Property(name = "ReactiveForwardingFlowExport", boolValue = R_FWD_FLOWS_EXPORT,
            label = "Reactive Forwarding application flows exported over IPFIX when removed")
    private boolean reactiveForwardingFlowExport = R_FWD_FLOWS_EXPORT;

    private static final boolean PORTSTATS_FLOWS_EXPORT = false;
    @Property(name = "PortStatsFlowExport", boolValue = PORTSTATS_FLOWS_EXPORT,
            label = "Switch Port Statistics exported over IPFIX")
    private boolean portStatsFlowExport = PORTSTATS_FLOWS_EXPORT;

    private static final String COLLECTOR_ADDRESS = "127.0.0.1";
    @Property(name = "CollectorAddress", value = COLLECTOR_ADDRESS,
    label = "IPFIX Collector IP Address")
    private String collectorAddress = COLLECTOR_ADDRESS;
    protected static IpAddress collectorIp;

    private static final int COLLECTOR_PORT = 2055;
    @Property(name = "CollectorPort", intValue = COLLECTOR_PORT,
    label = "IPFIX Collector UDP Port")
    protected static int collectorPort = COLLECTOR_PORT;

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication("net.sdnmon.of2ipfix");
        cfgService.registerProperties(getClass());
        getProperties(context);
        collectorIp = IpAddress.valueOf(collectorAddress);
        if (reactiveForwardingFlowExport) {
            flowRemovedListener = new FlowRemovedListener(this);
            flowRuleService.addListener(flowRemovedListener);
        }
        if (portStatsFlowExport) {
            portStatsListener = new PortStatsListener(this);
            deviceService.addListener(portStatsListener);
        }
        ipfixSender = new IpfixSender(this);
        log.info("Started. reactiveForwardingFlowExport={}, portStatsFlowExport={}, IPFIX collector: ip={}, port={}",
                reactiveForwardingFlowExport, portStatsFlowExport, collectorAddress, collectorPort);
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        if (flowRemovedListener != null) {
            flowRuleService.removeListener(flowRemovedListener);
            flowRemovedListener = null;
        }
        if (portStatsListener != null) {
            deviceService.removeListener(portStatsListener);
            portStatsListener = null;
        }
        ipfixSender = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        getProperties(context);
        if (reactiveForwardingFlowExport) {
            if (flowRemovedListener == null) {
                flowRemovedListener = new FlowRemovedListener(this);
                flowRuleService.addListener(flowRemovedListener);
            }
        } else if (flowRemovedListener != null) {
            // reactiveForwardingFlowExport is false
            flowRuleService.removeListener(flowRemovedListener);
            flowRemovedListener = null;
        }
        if (portStatsFlowExport) {
            if (portStatsListener == null) {
                portStatsListener = new PortStatsListener(this);
                deviceService.addListener(portStatsListener);
            }
        } else if (portStatsListener != null) {
            // portStatsFlowExport is false
            deviceService.removeListener(portStatsListener);
            portStatsListener = null;
        }
        log.info("Modified. reactiveForwardingFlowExport={}, portStatsFlowExport={}, IPFIX collector: ip={}, port={}",
                reactiveForwardingFlowExport, portStatsFlowExport, collectorAddress, collectorPort);
    }

    public void getProperties(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        // parse CollectorPort Property
        String s = get(properties, "CollectorPort");
        try {
            collectorPort = isNullOrEmpty(s) ? collectorPort : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            log.info("CollectorPort Format Exception");
        }

        // parse CollectorAddress Property
        s = get(properties, "CollectorAddress");
        collectorAddress = isNullOrEmpty(s) ? collectorAddress : s;
        collectorIp = IpAddress.valueOf(collectorAddress);

        // parse reactiveForwardingFlowExport Property
        s = get(properties, "ReactiveForwardingFlowExport");
        reactiveForwardingFlowExport = Strings.isNullOrEmpty(s) ? R_FWD_FLOWS_EXPORT : Boolean.valueOf(s);

        // parse portStatsFlowExport Property
        s = get(properties, "PortStatsFlowExport");
        portStatsFlowExport = Strings.isNullOrEmpty(s) ? PORTSTATS_FLOWS_EXPORT : Boolean.valueOf(s);
    }

}