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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;

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
    public void setNodeForwarding(CarrierEthernetForwardingConstruct fc, CarrierEthernetNetworkInterface ingressNi,
                                  Set<CarrierEthernetNetworkInterface> egressNiSet) {

        if (ingressNi == null || egressNiSet.isEmpty()) {
            log.error("There needs to be at least one ingress and one egress NI to set forwarding.");
            return;
        }

        flowObjectiveMap.putIfAbsent(fc.id(), new LinkedList<>());

        // TODO: Get created FlowObjectives from this method
        createFlowObjectives(fc, ingressNi, egressNiSet);
    }

    /**
     * Creates and submits FlowObjectives depending on role of the device in the FC and ingress/egress NI types.
     *
     * @param fc the FC representation
     * @param ingressNi the ingress NI (UNI, INNI, ENNI or GENERIC) of the EVC for this forwarding segment
     * @param  egressNiSet the set of egress NIs (UNI, INNI, ENNI or GENERIC) of the EVC for this forwarding segment
     */
    private void createFlowObjectives(CarrierEthernetForwardingConstruct fc, CarrierEthernetNetworkInterface ingressNi,
                                      Set<CarrierEthernetNetworkInterface> egressNiSet) {

        /////////////////////////////////////////
        // Prepare and submit filtering objective
        /////////////////////////////////////////

        FilteringObjective.Builder filteringObjectiveBuilder = DefaultFilteringObjective.builder()
                .permit().fromApp(appId)
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(ingressNi.cp().port()));

        TrafficTreatment.Builder filterTreatmentBuilder = DefaultTrafficTreatment.builder();

        // In general, nodes would match on the VLAN tag assigned to the EVC/FC
        Criterion filterVlanIdCriterion = Criteria.matchVlanId(fc.vlanId());

        if ((ingressNi.type().equals(CarrierEthernetNetworkInterface.Type.INNI))
                || (ingressNi.type().equals(CarrierEthernetNetworkInterface.Type.ENNI)) ) {
            // TODO: Check TPID? Also: Is is possible to receive untagged pkts at an INNI/ENNI?
            // Source node of an FC should match on S-TAG if it's an INNI/ENNI
            filterVlanIdCriterion = Criteria.matchVlanId(ingressNi.sVlanId());
            // Translate S-TAG to the one used in the current FC
            filterTreatmentBuilder.setVlanId(fc.vlanId());
        } else if (ingressNi.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            // Source node of an FC should match on CE-VLAN ID (if present) if it's a UNI
            filterVlanIdCriterion = Criteria.matchVlanId(ingressNi.ceVlanId());
            // Push S-TAG of current FC on top of existing CE-VLAN ID
            filterTreatmentBuilder.pushVlan().setVlanId(fc.vlanId());
        }

        filteringObjectiveBuilder.addCondition(filterVlanIdCriterion);

        // Do not add meta if there are no instructions (i.e. if not first)
        if (!(ingressNi.type().equals(CarrierEthernetNetworkInterface.Type.GENERIC))) {
            filteringObjectiveBuilder.withMeta(filterTreatmentBuilder.build());
        }

        flowObjectiveService.filter(ingressNi.cp().deviceId(), filteringObjectiveBuilder.add());
        flowObjectiveMap.get(fc.id()).addFirst(Pair.of(ingressNi.cp().deviceId(), filteringObjectiveBuilder.add()));

        ////////////////////////////////////////////////////
        // Prepare and submit next and forwarding objectives
        ////////////////////////////////////////////////////

        TrafficSelector fwdSelector = DefaultTrafficSelector.builder()
                .matchVlanId(fc.vlanId())
                .matchInPort(ingressNi.cp().port())
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();

        Integer nextId = flowObjectiveService.allocateNextId();

        // Setting higher priority to fwd/next objectives to bypass filter in case of match conflict in OVS switches
        NextObjective.Builder nextObjectiveBuider = DefaultNextObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withType(NextObjective.Type.BROADCAST)
                .withPriority(PRIORITY + 1)
                .withMeta(fwdSelector)
                .withId(nextId);

         egressNiSet.forEach(egressNi -> {
            // TODO: Check if ingressNi and egressNi are on the same device?
            TrafficTreatment.Builder nextTreatmentBuilder = DefaultTrafficTreatment.builder();
            // If last NI in FC is not UNI, keep the existing S-TAG - it will be translated at the entrance of the next FC
            if (egressNi.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                nextTreatmentBuilder.popVlan();
            }
            Instruction outInstruction = Instructions.createOutput(egressNi.cp().port());
            nextTreatmentBuilder.add(outInstruction);
            nextObjectiveBuider.addTreatment(nextTreatmentBuilder.build());
        });

        NextObjective nextObjective = nextObjectiveBuider.add();

        // Setting higher priority to fwd/next objectives to bypass filter in case of match conflict in OVS switches
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY + 1)
                .withSelector(fwdSelector)
                .nextStep(nextId)
                .add();

        flowObjectiveService.next(ingressNi.cp().deviceId(), nextObjective);
        // Add all NextObjectives at the end of the list so that they will be removed last
        flowObjectiveMap.get(fc.id()).addLast(Pair.of(ingressNi.cp().deviceId(), nextObjective));

        flowObjectiveService.forward(ingressNi.cp().deviceId(), forwardingObjective);
        flowObjectiveMap.get(fc.id()).addFirst(Pair.of(ingressNi.cp().deviceId(), forwardingObjective));
    }

    @Override
    void applyBandwidthProfileResources(CarrierEthernetForwardingConstruct fc, CarrierEthernetUni uni) {

        Dpid dpid = Dpid.dpid(uni.cp().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        // Do not apply meters to OFDPA 2.0 switches since they are not currently supported
        if (sw.softwareDescription().equals("OF-DPA 2.0")) {
            return;
        }

        // Create meters and add them to global MeterId map
        Set<DeviceMeterId> deviceMeterIdSet = deviceMeterIdMap.get(fc.id());
        if (deviceMeterIdSet == null) {
            deviceMeterIdSet = new HashSet<>();
        }
        deviceMeterIdSet.addAll(createMeters(uni));
        deviceMeterIdMap.put(fc.id(), deviceMeterIdSet);

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
            // TODO: Check the vlanIds of all FCs comprising the EVC????
            if (uni.cp().equals(flowInCp) && fc.vlanId().equals(flowInVlanId)) {
                // Need to add to the flow the meters associated with the same device
                Set<DeviceMeterId> tmpDeviceMeterIdSet = new HashSet<>();
                deviceMeterIdMap.get(fc.id()).forEach(deviceMeterId -> {
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
    void removeBandwidthProfileResources(String fcId, CarrierEthernetUni uni) {
        removeMeters(fcId, uni);
    }

    /**
     * Removes the meters associated with a specific UNI of an FC.
     *
     * @param fcId the EVC ID
     * @param uni the UNI descriptor
     * */
    private void removeMeters(String fcId, CarrierEthernetUni uni) {

        Set<DeviceMeterId> newDeviceMeterIdSet = deviceMeterIdMap.get(fcId);
        DeviceMeterId tmpDeviceMeterId;

        Collection<Meter> meters = meterService.getMeters(uni.cp().deviceId());

        Iterator<Meter> it = meters.iterator();
        while (it.hasNext()) {
            Meter meter = it.next();
            tmpDeviceMeterId = new DeviceMeterId(uni.cp().deviceId(), meter.id());
            if (meter.appId().equals(appId) &&
                    deviceMeterIdMap.get(fcId).contains(tmpDeviceMeterId)) {
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

        deviceMeterIdMap.put(fcId, newDeviceMeterIdSet);
    }

    @Override
    void removeAllForwardingResources(CarrierEthernetForwardingConstruct fc) {
        removeFlowObjectives(fc.id());
    }

    /**
     * Removes all flow objectives installed by the application which are associated with a specific FC.
     *
     * @param fcId the FC id
     * */
    private void removeFlowObjectives(String fcId) {
        // Note: A Flow Rule cannot be shared by multiple FCs due to different VLAN or CE-VLAN ID match.
        List<Pair<DeviceId, Objective>> flowObjectiveList = flowObjectiveMap.remove(fcId);
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
