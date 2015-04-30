package org.onos.oneping;

import com.google.common.collect.HashMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_SRC;

/**
 * Sample application that permits only one ICMP ping per minute for a unique
 * src/dst MAC pair per switch.
 */
@Component(immediate = true)
public class OnePing {

    private static final String MSG_PINGED_ONCE =
            "Thank you, Vasili. One ping from {} to {} received by {}";
    private static final String MSG_PINGED_TWICE =
            "What are you doing, Vasili?! I said one ping only!!! " +
                    "Ping from {} to {} has already been received by {};" +
                    " 60 second ban has been issued";
    private static final String MSG_PING_REENABLED =
            "Careful next time, Vasili! Re-enabled ping from {} to {} on {}";


    private static Logger log = LoggerFactory.getLogger(OnePing.class);

    private static final int PRIORITY = 128;
    private static final int DROP_PRIORITY = 129;
    private static final int TIMEOUT_SEC = 60; // seconds

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;
    private final PacketProcessor packetProcessor = new PingPacketProcessor();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final FlowRuleListener flowListener = new InternalFlowListener();

    private final TrafficSelector selector = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_ICMP)
            .build();

//    private TrafficTreatment treatment = DefaultTrafficTreatment.builder().punt().build();  // requires ONOS 1.1.0+
    private TrafficTreatment treatment = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.CONTROLLER).build();  // requires ONOS 1.0.1+

    private final HashMultimap<DeviceId, PingRecord> pings = HashMultimap.create();
    private final Timer timer = new Timer("oneping-sweeper");

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onos.oneping");
        packetService.addProcessor(packetProcessor, PRIORITY);
        deviceService.addListener(deviceListener);
        flowRuleService.addListener(flowListener);
        pushInterceptRules();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(packetProcessor);
        flowRuleService.removeListener(flowListener);
        log.info("Stopped");
    }

    // Pushes ICMP intercept rules to all connected devices
    private void pushInterceptRules() {
        deviceService.getDevices().forEach(this::pushInterceptRule);
    }

    // Pushes ICMP intercept rule to the specified device.
    private void pushInterceptRule(Device device) {
        DefaultFlowRule rule = new DefaultFlowRule(device.id(), selector, treatment,
                                                   PRIORITY, appId, 0, true);
        flowRuleService.applyFlowRules(rule);
    }

    // Processes the specified ICMP ping packet.
    private void processPing(PacketContext context, Ethernet eth) {
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        MacAddress src = eth.getSourceMAC();
        MacAddress dst = eth.getDestinationMAC();
        PingRecord ping = new PingRecord(src, dst);
        boolean pinged = pings.get(deviceId).contains(ping);

        if (pinged) {
            log.warn(MSG_PINGED_TWICE, src, dst, deviceId);
            banPings(deviceId, src, dst);
            context.block();
        } else {
            log.info(MSG_PINGED_ONCE, src, dst, deviceId);
            pings.put(deviceId, ping);
            timer.schedule(new PingPruner(deviceId, ping), TIMEOUT_SEC * 1000);
        }
    }

    // Installs a temporary drop rule for the ICMP pings between given srd/dst.
    private void banPings(DeviceId deviceId, MacAddress src, MacAddress dst) {
        TrafficSelector sel = DefaultTrafficSelector.builder()
                .matchEthSrc(src).matchEthDst(dst).build();
        //The default behavior of DefaultTrafficTreatment.build has changed,
        //where the implicit DROP instruction will not get added when the
        //instruction set is empty, hence explicitly adding it.
        TrafficTreatment treat = DefaultTrafficTreatment.builder().drop().build();
        DefaultFlowRule drop = new DefaultFlowRule(deviceId, sel, treat,
                                                   DROP_PRIORITY, appId,
                                                   TIMEOUT_SEC, false);
        flowRuleService.applyFlowRules(drop);
    }


    // Indicates whether the specified packet corresponds to ICMP ping.
    private boolean isIcmpPing(Ethernet eth) {
        if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
            return ((IPv4) eth.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP;
        }
        return false;
    }


    // Intercepts packets
    private class PingPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            Ethernet eth = context.inPacket().parsed();
            if (isIcmpPing(eth)) {
                processPing(context, eth);
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() == DeviceEvent.Type.DEVICE_ADDED) {
                pushInterceptRule(event.subject());
            }
        }
    }

    private class PingRecord {
        private final MacAddress src;
        private final MacAddress dst;

        PingRecord(MacAddress src, MacAddress dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final PingRecord other = (PingRecord) obj;
            return Objects.equals(this.src, other.src) && Objects.equals(this.dst, other.dst);
        }
    }

    // Prunes the given ping record from the specified device.
    private class PingPruner extends TimerTask {
        private final DeviceId deviceId;
        private final PingRecord ping;

        public PingPruner(DeviceId deviceId, PingRecord ping) {
            this.deviceId = deviceId;
            this.ping = ping;
        }

        @Override
        public void run() {
            pings.remove(deviceId, ping);
        }
    }

    // Listens for our removed flows.
    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            FlowRule flowRule = event.subject();
            if (event.type() == FlowRuleEvent.Type.RULE_REMOVED &&
                    flowRule.appId() == appId.id()) {
                MacAddress src = ((EthCriterion) flowRule.selector().getCriterion(ETH_SRC)).mac();
                MacAddress dst = ((EthCriterion) flowRule.selector().getCriterion(ETH_DST)).mac();
                log.warn(MSG_PING_REENABLED, src, dst, flowRule.deviceId());
            }
        }
    }
}
