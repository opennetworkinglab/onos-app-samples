package org.onosproject.ecord.carrierethernet.app;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.*;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.slf4j.Logger;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Class used to control Carrier Ethernet nodes according to the OpenFlow (1.3 and above) protocol.
 */
@Component(immediate = true)
@Service (value = CarrierEthernetOpenFlowPacketNodeManager.class)
public class CarrierEthernetOpenFlowPacketNodeManager extends CarrierEthernetPacketNodeManager {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private final Logger log = getLogger(getClass());

    private static ApplicationId appId;

    private static final int PRIORITY = 50000;

    // TODO: Below maps to be replaced by the meter ids and flow objectives associated with each CE Intent

    // FIXME: Replace with Pair<DeviceId, MeterId>
    private final Map<String, Set<DeviceMeterId>> deviceMeterIdMap = new HashMap<>();
    private final Map<String, LinkedList<Pair<DeviceId, Objective>>> flowObjectiveMap = new HashMap();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.ecord.carrierethernet");
    }

    @Deactivate
    protected void deactivate() {}

    @Override
    public void setNodeForwarding(CarrierEthernetVirtualConnection evc, CarrierEthernetNetworkInterface srcNi,
                                  CarrierEthernetNetworkInterface dstNi, ConnectPoint ingress, ConnectPoint egress,
                                  boolean first, boolean last) {

        // FIXME: Is this necessary? Can first and last be true at the same time?
        if(srcNi.cp().deviceId().equals(dstNi.cp().deviceId())) {
            log.error("Source and destination NI must belong to different devices.");
            return;
        }

        if (flowObjectiveMap.get(evc.id()) == null) {
            flowObjectiveMap.put(evc.id(), new LinkedList<>());
        }

        // TODO: Get created FlowObjectives from this method
        createFlowObjectives(evc, srcNi, dstNi, ingress, egress, first, last);
    }

    // Directly creates FlowRules using GROUP actions
    private void createFlowObjectives(CarrierEthernetVirtualConnection evc, CarrierEthernetNetworkInterface srcNi,
                                      CarrierEthernetNetworkInterface dstNi, ConnectPoint ingress, ConnectPoint egress,
                                      boolean first, boolean last) {

        /////////////////////////////////////////
        // Prepare and submit filtering objective
        /////////////////////////////////////////

        FilteringObjective.Builder filteringObjectiveBuilder = DefaultFilteringObjective.builder()
                .permit().fromApp(appId)
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(ingress.port()));

        TrafficTreatment.Builder filterTreatmentBuilder = DefaultTrafficTreatment.builder();

        // In general, nodes would match on the VLAN tag assigned to the EVC/FC
        Criterion filterVlanIdCriterion = Criteria.matchVlanId(evc.vlanId());

        if (first) {
            if ((srcNi instanceof CarrierEthernetInni) || (srcNi instanceof CarrierEthernetEnni) ) {
                // TODO: Check TPID? Also: Is is possible to receive untagged pkts at an INNI/ENNI?
                // First node of an FC should match on S-TAG if it's an INNI/ENNI
                filterVlanIdCriterion = Criteria.matchVlanId(srcNi.sVlanId());
                // Translate S-TAG to the one used in the current FC
                filterTreatmentBuilder.setVlanId(evc.vlanId());
            } else {
                // First node of an FC should match on CE-VLAN ID (if present) if it's a UNI
                filterVlanIdCriterion = Criteria.matchVlanId(srcNi.ceVlanId());
                // Push S-TAG of current FC on top of existing CE-VLAN ID
                filterTreatmentBuilder.pushVlan().setVlanId(evc.vlanId());
            }
        }

        filteringObjectiveBuilder.addCondition(filterVlanIdCriterion);

        // Do not add meta if there are no instructions (i.e. if not first)
        if (first) {
            filteringObjectiveBuilder.withMeta(filterTreatmentBuilder.build());
        }

        flowObjectiveService.filter(ingress.deviceId(), filteringObjectiveBuilder.add());
        flowObjectiveMap.get(evc.id()).addFirst(Pair.of(ingress.deviceId(), filteringObjectiveBuilder.add()));

        ////////////////////////////////////////////////////
        // Prepare and submit next and forwarding objectives
        ////////////////////////////////////////////////////

        TrafficSelector fwdSelector = DefaultTrafficSelector.builder()
                .matchVlanId(evc.vlanId())
                .matchInPort(ingress.port())
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();

        TrafficTreatment.Builder nextTreatmentBuilder = DefaultTrafficTreatment.builder();

        // If last NI in FC is not UNI, keep the existing S-TAG - it will be translated at the entrance of the next FC
        if (last && (dstNi instanceof CarrierEthernetUni)) {
            nextTreatmentBuilder.popVlan();
        }
        Instruction outInstruction = Instructions.createOutput(egress.port());
        nextTreatmentBuilder.add(outInstruction);

        // Check if flow rule with same selector already exists (e.g. when branching in an E-LAN or E-Tree).
        // If yes, it will be replaced, so we need to include its output in the currently prepared NextObjective
        Iterator<FlowRule> flowRuleIt = flowRuleService.getFlowRulesById(appId).iterator();
        while (flowRuleIt.hasNext()) {
            FlowRule flowRule = flowRuleIt.next();
            if (flowRule.deviceId().equals(egress.deviceId()) && flowRule.selector().equals(fwdSelector)) {
                Iterator<Instruction> instructionIt = flowRule.treatment().allInstructions().iterator();
                while (instructionIt.hasNext()) {
                    Instruction instruction = instructionIt.next();
                    // If this is an GROUP instruction and it's different than existing, add it to FlowObjective
                    if (instruction.type().equals(outInstruction.type()) &&
                            !(instruction.equals(outInstruction))) {
                            nextTreatmentBuilder.add(instruction);
                    }
                }
            }
        }

        Integer nextId = flowObjectiveService.allocateNextId();

        NextObjective nextObjective = DefaultNextObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withType(NextObjective.Type.SIMPLE)
                .withPriority(PRIORITY)
                .withMeta(fwdSelector)
                .addTreatment(nextTreatmentBuilder.build())
                .withId(nextId)
                .add();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(fwdSelector)
                .nextStep(nextId)
                .add();

        flowObjectiveService.next(egress.deviceId(), nextObjective);
        // Add all NextObjectives at the end of the list so that they will be removed last
        flowObjectiveMap.get(evc.id()).addLast(Pair.of(ingress.deviceId(), nextObjective));

        flowObjectiveService.forward(egress.deviceId(), forwardingObjective);
        flowObjectiveMap.get(evc.id()).addFirst(Pair.of(ingress.deviceId(), forwardingObjective));

        // FIXME: For efficiency do not send FlowObjective again if new treatment is exactly the same as existing one
    }

    @Override
    void applyBandwidthProfileResources(CarrierEthernetVirtualConnection evc, CarrierEthernetUni uni) {

        Dpid dpid = Dpid.dpid(uni.cp().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        // Do not apply meters to OFDPA 2.0 switches since they are not currently supported
        if (sw.softwareDescription().equals("OF-DPA 2.0")) {
            return;
        }

        // Create meters and add them to global MeterId map
        Set<DeviceMeterId> deviceMeterIdSet = deviceMeterIdMap.get(evc.id());
        if (deviceMeterIdSet == null) {
            deviceMeterIdSet = new HashSet<>();
        }
        deviceMeterIdSet.addAll(createMeters(uni));
        deviceMeterIdMap.put(evc.id(), deviceMeterIdSet);

        // Apply meters to already installed flows

        Set<FlowRule> newFlowRuleSet = new HashSet<>();

        // Get flow rules belonging to service and having as in_port the UNI connect point

        // FIXME: Check for flow rules associated with evcId
        for (FlowRule flowRule : flowRuleService.getFlowRulesById(appId)) {
            PortCriterion portCriterion = (PortCriterion) flowRule.selector().getCriterion(Criterion.Type.IN_PORT);
            VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) flowRule.selector()
                    .getCriterion(Criterion.Type.VLAN_VID);
            if (portCriterion == null || vlanIdCriterion == null) {
                continue;
            }
            PortNumber inPort = portCriterion.port();
            VlanId flowInVlanId = vlanIdCriterion.vlanId();
            if (inPort == null || flowInVlanId == null) {
                continue;
            }
            ConnectPoint flowInCp = new ConnectPoint(flowRule.deviceId(), inPort);
            // FIXME: Maybe check also if there is a group action?
            if (uni.cp().equals(flowInCp) && evc.vlanId().equals(flowInVlanId)) {
                // Need to add to the flow the meters associated with the same device
                Set<DeviceMeterId> tmpDeviceMeterIdSet = new HashSet<>();
                deviceMeterIdMap.get(evc.id()).forEach(deviceMeterId -> {
                    if (deviceMeterId.deviceId().equals(flowRule.deviceId())) {
                        tmpDeviceMeterIdSet.add(deviceMeterId);
                    }
                });
                // Modify and submit flow rule only if there are meters to add
                if (!tmpDeviceMeterIdSet.isEmpty()) {
                    FlowRule newFlowRule = addMetersToFlowRule(flowRule, tmpDeviceMeterIdSet);
                    flowRuleService.applyFlowRules(newFlowRule);
                    newFlowRuleSet.add(newFlowRule);
                } else {
                    newFlowRuleSet.add(flowRule);
                }
            } else {
                newFlowRuleSet.add(flowRule);
            }
        }
    }

    /**
     * Creates and submits a meter with the required bands for a UNI.
     *
     * @param uni the UNI descriptor
     * @return set of meter ids of the meters created
     */
    private Set<DeviceMeterId> createMeters(CarrierEthernetUni uni) {

        // TODO: Check if meter already exists before adding it?

        Set<DeviceMeterId> deviceMeterIdSet = new HashSet<>();

        long longCir = (long) (uni.bwp().cir().bps() / 8000);
        long longEir = (long) (uni.bwp().eir().bps() / 8000);

        MeterRequest.Builder meterRequestBuilder;
        Meter meter;
        Band.Builder bandBuilder;

        Set<Band> bandSet = new HashSet<>();

        // If EIR is zero do not create the REMARK meter
        if (longEir != 0) {
            // Mark frames that exceed CIR as Best Effort
            bandBuilder = DefaultBand.builder()
                    .ofType(Band.Type.REMARK)
                    .withRate(longCir)
                    .dropPrecedence((short) 0);

            if (uni.bwp().cbs() != 0) {
                bandBuilder.burstSize(uni.bwp().cbs());
            }

            bandSet.add(bandBuilder.build());
        }

        // If CIR is zero do not create the DROP meter
        if (longCir != 0) {
            // Drop all frames that exceed CIR + EIR
            bandBuilder = DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .withRate(longCir + longEir);

            if (uni.bwp().cbs() != 0 || uni.bwp().ebs() != 0) {
                // FIXME: Use CBS and EBS correctly according to MEF specs
                bandBuilder.burstSize(uni.bwp().cbs() + uni.bwp().ebs());
            }

            bandSet.add(bandBuilder.build());
        }

        // Create meter only if at least one band was created
        if (!bandSet.isEmpty()) {
            meterRequestBuilder = DefaultMeterRequest.builder()
                    .forDevice(uni.cp().deviceId())
                    .fromApp(appId)
                    .withUnit(Meter.Unit.KB_PER_SEC)
                    .withBands(bandSet);

            if (uni.bwp().cbs() != 0 || uni.bwp().ebs() != 0) {
                meterRequestBuilder.burst();
            }

            meter = meterService.submit(meterRequestBuilder.add());
            deviceMeterIdSet.add(new DeviceMeterId(uni.cp().deviceId(), meter.id()));
        }

        return deviceMeterIdSet;
    }

    private FlowRule addMetersToFlowRule(FlowRule flowRule, Set<DeviceMeterId> deviceMeterIdSet) {

        // FIXME: Refactor to use only single meter

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                .builder(flowRule.treatment());

        deviceMeterIdSet.forEach(deviceMeterId -> {
            tBuilder.meter(deviceMeterId.meterId());
        });

        return createFlowRule(flowRule.deviceId(), flowRule.priority(),
                flowRule.selector(), tBuilder.build(), flowRule.tableId());
    }

    @Override
    void removeBandwidthProfileResources(String serviceId, CarrierEthernetUni uni) {

        removeMeters(serviceId, uni);
    }

    /**
     * Removes the meters associated with a specific UNI of a service.
     *
     * @param serviceId the CE service ID
     * @param uni the UNI descriptor
     * */
    private void removeMeters(String serviceId, CarrierEthernetUni uni) {

        Set<DeviceMeterId> newDeviceMeterIdSet = deviceMeterIdMap.get(serviceId);
        DeviceMeterId tmpDeviceMeterId;

        Collection<Meter> meters = meterService.getMeters(uni.cp().deviceId());

        Iterator<Meter> it = meters.iterator();
        while (it.hasNext()) {
            Meter meter = it.next();
            tmpDeviceMeterId = new DeviceMeterId(uni.cp().deviceId(), meter.id());
            if (meter.appId().equals(appId) &&
                    deviceMeterIdMap.get(serviceId).contains(tmpDeviceMeterId)) {
                MeterRequest.Builder mBuilder;
                mBuilder = DefaultMeterRequest.builder()
                        .fromApp(meter.appId())
                        .forDevice(meter.deviceId())
                        .withUnit(meter.unit())
                        .withBands(meter.bands());
                if (uni.bwp().cbs() != 0 || uni.bwp().ebs() != 0) {
                    mBuilder.burst();
                }
                meterService.withdraw(mBuilder.remove(), meter.id());
                newDeviceMeterIdSet.remove(tmpDeviceMeterId);
            }
        }

        deviceMeterIdMap.put(serviceId, newDeviceMeterIdSet);
    }

    @Override
    void removeAllForwardingResources(CarrierEthernetVirtualConnection evc) {
        removeFlowObjectives(evc.id());
    }

    /**
     * Removes all flow objectives installed by the application which are associated with a specific EVC.
     *
     * @param evcId the EVC id
     * */
    private void removeFlowObjectives(String evcId) {
        // Note: A Flow Rule cannot be shared by multiple services due to different VLAN or CE-VLAN ID match.
        List<Pair<DeviceId, Objective>> flowObjectiveList = flowObjectiveMap.remove(evcId);
        // NextObjectives will be removed after all other Objectives
        ListIterator<Pair<DeviceId, Objective>> objIter = flowObjectiveList.listIterator();
        while (objIter.hasNext()) {
            Pair<DeviceId, Objective> deviceObjectivePair = objIter.next();
            flowObjectiveService.apply(deviceObjectivePair.getLeft(), deviceObjectivePair.getRight().copy().remove());
        }

    }

    // FIXME: Replace with Pair<DeviceId, MeterId>
    /**
     * Utility class to compensate for the fact that MeterIds are not unique system-wide.
     * */
    class DeviceMeterId {
        private DeviceId deviceId;
        private MeterId meterId;

        DeviceMeterId(DeviceId deviceId, MeterId meterId) {
            this.deviceId = deviceId;
            this.meterId = meterId;
        }

        public DeviceId deviceId() {
            return deviceId;
        }

        public MeterId meterId() {
            return meterId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, meterId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DeviceMeterId) {
                DeviceMeterId other = (DeviceMeterId) obj;
                if (this.deviceId().equals(other.deviceId()) && this.meterId().equals(other.meterId())) {
                    return true;
                }
            }
            return false;
        }
    }

    private FlowRule createFlowRule(DeviceId deviceId, int priority,
                                    TrafficSelector selector, TrafficTreatment treatment, int tableId) {
        return DefaultFlowRule.builder()
                .fromApp(appId)
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(priority)
                .withSelector(selector)
                .withTreatment(treatment)
                .forTable(tableId)
                .build();
    }

}
