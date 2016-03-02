package org.onosproject.ecord.carrierethernet.app;

import com.google.common.collect.Lists;
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
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
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
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Class used to control Carrier Ethernet nodes according to the OpenFlow protocol.
 */
@Component(immediate = true)
@Service (value = CarrierEthernetOpenFlowPacketNodeManager.class)
public class CarrierEthernetOpenFlowPacketNodeManager extends CarrierEthernetPacketNodeManager {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private final Logger log = getLogger(getClass());

    private static ApplicationId appId;

    private static final int PRIORITY = 50000;

    // TODO: Get the OFDPA2.0 table numbers from the OFDPA Pipeline Class?
    protected static final int PORT_TABLE = 0;
    protected static final int VLAN_TABLE = 10;
    protected static final int TMAC_TABLE = 20;
    protected static final int UNICAST_ROUTING_TABLE = 30;
    protected static final int MULTICAST_ROUTING_TABLE = 40;
    protected static final int MPLS_TABLE_0 = 23;
    protected static final int MPLS_TABLE_1 = 24;
    protected static final int BRIDGING_TABLE = 50;
    protected static final int ACL_TABLE = 60;
    protected static final int MAC_LEARNING_TABLE = 254;

    // TODO: Below maps to be replaced by the meter ids, egress cps and flow rules kept with each service (?)
    private final Map<String, Set<DeviceMeterId>> deviceMeterIdMap = new HashMap<>();
    private final Map<String, Set<ConnectPoint>> egressCpMap = new HashMap<>();
    private final Map<String, Set<FlowRule>> flowRuleMap = new HashMap();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.ecord.carrierethernet");
    }

    @Deactivate
    protected void deactivate() {}

    @Override
    public void setNodeForwarding(CarrierEthernetService service, CarrierEthernetUni srcUni, CarrierEthernetUni dstUni,
                           ConnectPoint ingress, ConnectPoint egress, boolean first, boolean last) {

        // TODO: Produce error if ingress and egress do not belong to same device

        Set<ConnectPoint> egressCpSet = egressCpMap.get(service.id());
        if (egressCpSet == null) {
            egressCpSet = new HashSet<>();
        }
        Set<FlowRule> flowRuleSet = flowRuleMap.get(service.id());
        if (flowRuleSet == null) {
            flowRuleSet = new HashSet<>();
        }

        flowRuleSet.addAll(createFlowRules(service.id(), srcUni.ceVlanId(), service.vlanId(),
                ingress, egress, first, last));

        egressCpSet.add(egress);

        egressCpMap.put(service.id(), egressCpSet);
        flowRuleMap.put(service.id(), flowRuleSet);

    }

    // FIXME: Temporary solution for establishing flow rules according to switch type
    private Set<FlowRule> createFlowRules(String serviceId, VlanId ceVlanId, VlanId vlanId,
                                          ConnectPoint ingress, ConnectPoint egress, boolean first, boolean last) {

        Dpid dpid = Dpid.dpid(egress.deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        Set<FlowRule> flowRuleSet = new HashSet<>();
        if (sw.softwareDescription().equals("OF-DPA 2.0")) {
            flowRuleSet = createOfdpaFlowRules(serviceId, ceVlanId, vlanId, ingress, egress, first, last);
            //createFilteringForwarding(serviceId, ceVlanId, vlanId, ingress, egress, first, last);
        } else if (sw.factory().getVersion() == OFVersion.OF_13) {
            flowRuleSet = createOF13FlowRule(serviceId, ceVlanId, vlanId, ingress, egress, first, last);
        } else {
            flowRuleSet = createOF10FlowRule(serviceId, ceVlanId, vlanId, ingress, egress, first, last);
        }

        return flowRuleSet;
    }

    /**
     * Directly creates FlowRules according to the OFDPA pipeline.
     * To be used instead of FlowObjectives until the OFDPA2Pipeline is modified appropriately
     *
     * @param serviceId User-provided identifier of the CE service
     * @param vlanId VLAN id of the service
     * @param ingress ingress connect point at the particular device
     * @param first indicates whether the current device is the first one in the path
     * @param last indicates whether the current device is the last one in the path
     */
    private Set<FlowRule> createOfdpaFlowRules(String serviceId, VlanId ceVlanId, VlanId vlanId,
                                               ConnectPoint ingress, ConnectPoint egress, boolean first, boolean last) {

        Set<FlowRule> flowRuleSet = new HashSet<>();

        DeviceId deviceId = egress.deviceId();

        // VLAN Table
        // "Vlan Assignment"

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchInPort(ingress.port());

        TrafficSelector.Builder preSelector = null;
        TrafficTreatment.Builder preTreatment = null;

        // Transition to TMAC table
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .transition(TMAC_TABLE);

        if (first) {
            // If this is a virtual service, match also on CE-VLAN ID at first hop
            if (ceVlanId != null) {
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(ceVlanId);
                sBuilder.extension(ofdpaMatchVlanVid, deviceId);
                OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanId);
                tBuilder.extension(ofdpaSetVlanVid, deviceId);
            } else {
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(VlanId.vlanId((short) 0));
                sBuilder.extension(ofdpaMatchVlanVid, deviceId);
                OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanId);
                tBuilder.extension(ofdpaSetVlanVid, deviceId);
                // XXX ofdpa will require an additional vlan match on the assigned vlan
                // and it may not require the push. This is not in compliance with OF
                // standard. Waiting on what the exact flows are going to look like.
                preSelector = DefaultTrafficSelector.builder();
                preSelector.matchInPort(ingress.port());
                OfdpaMatchVlanVid preOfdpaMatchVlanVid = new OfdpaMatchVlanVid(vlanId);
                preSelector.extension(preOfdpaMatchVlanVid, deviceId);
                preTreatment = DefaultTrafficTreatment.builder().transition(20);
            }
        } else {
            OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vlanId);
            sBuilder.extension(ofdpaMatchVlanVid, deviceId);
        }

        FlowRule flowRule = createFlowRule(deviceId, PRIORITY, sBuilder.build(), tBuilder.build(), VLAN_TABLE);
        flowRuleService.applyFlowRules(flowRule);
        flowRuleSet.add(flowRule);

        if (preSelector != null) {

            flowRule = createFlowRule(deviceId, PRIORITY, preSelector.build(), preTreatment.build(), VLAN_TABLE);
            flowRuleService.applyFlowRules(flowRule);
            flowRuleSet.add(flowRule);
        }

        // TMAC Table defaults to Bridging Table

        // Build group
        GroupId groupId = createGroup(serviceId, vlanId, egress, first, last);

        // ACL Table
        // "IPv4 VLAN"

        // NOTE: Directly adding vlanId match to builder causes rule to get continuously installed/uninstalled by ofdpa
        sBuilder = DefaultTrafficSelector.builder()
                .matchInPort(ingress.port())
                .matchEthType(Ethernet.TYPE_IPV4);
        //sBuilder.extension(new OfdpaMatchVlanVid(vlanId), deviceId);

        // TODO: Check if there is existing FlowRule with same filtering and if yes modify this rule with an extra group
        // TODO: NOTE: In OFDPA this probably will be done by first removing the existing flow

        tBuilder = DefaultTrafficTreatment.builder().group(groupId);

        flowRule = createFlowRule(deviceId, PRIORITY, sBuilder.build(), tBuilder.build(), ACL_TABLE);
        flowRuleService.applyFlowRules(flowRule);
        flowRuleSet.add(flowRule);

        return flowRuleSet;
    }

    // Directly creates FlowRules using GROUP action (meant for OF1.3 non-OFDPA devices)
    private Set<FlowRule> createOF13FlowRule(String serviceId, VlanId ceVlanId, VlanId vlanId,
                                         ConnectPoint ingress, ConnectPoint egress,
                                         boolean first, boolean last) {

        Set<FlowRule> flowRuleSet = new HashSet<>();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchInPort(ingress.port());

        TrafficTreatment.Builder tBuilder;
        if (first) {
            // If this is a virtual service, match also on CE-VLAN ID at first hop
            if (ceVlanId != null) {
                sBuilder.matchVlanId(ceVlanId);
            }
            tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.pushVlan().setVlanId(vlanId);
        } else {
            sBuilder.matchVlanId(vlanId);
            tBuilder = DefaultTrafficTreatment.builder();
        }

        // Build group
        GroupId groupId = createGroup(serviceId, vlanId, egress, first, last);
        tBuilder.group(groupId);

        // Check if flow with same selector already exists. If yes, modify existing flow rule if needed
        flowRuleService.getFlowRulesById(appId).forEach(flowRule -> {
            if (flowRule.deviceId().equals(egress.deviceId()) && flowRule.selector().equals(sBuilder.build())) {
                flowRule.treatment().allInstructions().forEach(instruction -> {
                    // If this is an GROUP instruction and group is different than existing, add the group
                    if (instruction.type() == Instruction.Type.GROUP &&
                            !(instruction.equals(Instructions.createGroup(groupId)))) {
                        tBuilder.add(instruction);
                    }
                });
            }
        });

        // FIXME: For efficiency do not send FlowMod again if the new treatment is exactly the same as the existing one
        FlowRule flowRule = createFlowRule(egress.deviceId(), PRIORITY, sBuilder.build(), tBuilder.build(), 0);
        flowRuleService.applyFlowRules(flowRule);
        flowRuleSet.add(flowRule);

        return flowRuleSet;
    }

    // Directly creates FlowRules using OUTPUT action (meant for OF1.0 non-OFDPA devices)
    private Set<FlowRule> createOF10FlowRule(String serviceId, VlanId ceVlanId, VlanId vlanId,
                                         ConnectPoint ingress, ConnectPoint egress,
                                         boolean first, boolean last) {

        Set<FlowRule> flowRuleSet = new HashSet<>();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchInPort(ingress.port());

        TrafficTreatment.Builder tBuilder;
        if (first) {
            // If this is a virtual service, match also on CE-VLAN ID at first hop
            if (ceVlanId != null) {
                sBuilder.matchVlanId(ceVlanId);
            }
            tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.pushVlan().setVlanId(vlanId);
        } else {
            sBuilder.matchVlanId(vlanId);
            tBuilder = DefaultTrafficTreatment.builder();
        }

        if (last) {
            tBuilder.popVlan();
        }
        tBuilder.setOutput(egress.port());

        // Check if flow with same selector already exists. If yes, modify existing flow rule if needed
        flowRuleService.getFlowRulesById(appId).forEach(flowRule -> {
            if (flowRule.deviceId().equals(egress.deviceId()) && flowRule.selector().equals(sBuilder.build())) {
                flowRule.treatment().allInstructions().forEach(instruction -> {
                    // If this is an OUTPUT instruction and output is different than existing, add the group
                    if (instruction.type() == Instruction.Type.OUTPUT &&
                            !(instruction.equals(Instructions.createOutput(egress.port())))) {
                        tBuilder.add(instruction);
                    }
                });
            }
        });

        // FIXME: For efficiency do not send FlowMod again if the new treatment is exactly the same as the existing one
        FlowRule flowRule = createFlowRule(egress.deviceId(), PRIORITY, sBuilder.build(), tBuilder.build(), 0);
        flowRuleService.applyFlowRules(flowRule);
        flowRuleSet.add(flowRule);

        return flowRuleSet;
    }

    /**
     * Creates and submits FilteringObjective and ForwardingObjective with INDIRECT groups based on the role of the.
     * specific device within the path.
     *
     * @param serviceId User-provided identifier of the CE service
     * @param ceVlanId CE-VLAN id of the service, if present
     * @param vlanId VLAN id of the service
     * @param ingress ingress connect point at the particular device
     * @param first indicates whether the current device is the first one in the path
     * @param last indicates whether the current device is the last one in the path
     */
    private void createFilteringForwarding(String serviceId, VlanId ceVlanId, VlanId vlanId,
                                           ConnectPoint ingress, ConnectPoint egress,
                                           boolean first, boolean last) {

        createFilteringObjective(ceVlanId, vlanId, ingress, first);
        createForwardingObjective(serviceId, vlanId, ingress, egress, first, last);
    }

    /**
     * Creates and submits FilteringObjective based on the role of the specific device within the path.
     *
     * @param ceVlanId the CE-VLAN id of the service, if present
     * @param vlanId VLAN id of the service
     * @param ingress ingress connect point at the particular device
     * @param first indicates whether the current device is the first one in the path
     */
    private void createFilteringObjective(VlanId ceVlanId, VlanId vlanId, ConnectPoint ingress, boolean first) {

        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();

        fob.withKey(Criteria.matchInPort(ingress.port()));
        if (first) {
            // If this is a virtual service, match also on CE-VLAN ID at first hop
            if (ceVlanId != null) {
                fob.addCondition(Criteria.matchVlanId(ceVlanId));
            } else {
                fob.addCondition(Criteria.matchVlanId(VlanId.NONE));
            }
            ttb.pushVlan().setVlanId(vlanId);
        } else {
            fob.addCondition(Criteria.matchVlanId(vlanId));
        }

        fob.withPriority(PRIORITY);
        fob.withMeta(ttb.build());
        fob.permit().fromApp(appId);

        flowObjectiveService.filter(ingress.deviceId(), fob.add());

    }

    /**
     * Creates and submits ForwardingObjective based on the role of the specific device within the path.
     *
     * @param serviceId the CE service id
     * @param vlanId VLAN id of the service
     * @param ingress ingress connect point at the particular device
     * @param egress egress connect point at the particular device
     * @param first indicates whether the current device is the first one in the path
     * @param last indicates whether the current device is the last one in the path
     */
    private void createForwardingObjective(String serviceId, VlanId vlanId, ConnectPoint ingress, ConnectPoint egress,
                                           boolean first, boolean last) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(vlanId)
                .matchInPort(ingress.port())
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();

        GroupId groupId = createGroup(serviceId, vlanId, egress, first, last);

        // Add group to original treatment
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().group(groupId).build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(selector)
                .withTreatment(treatment)
                .add();

        flowObjectiveService.forward(ingress.deviceId(), forwardingObjective);
    }

    /**
     * Creates INDIRECT group with single bucket and submits it to GroupService.
     *
     * @param serviceId User-provided identifier of the CE service
     * @param vlanId VLAN id of the service
     * @param egress egress connect point at the particular device
     * @param first indicates whether the current device is the first one in the path
     * @param last indicates whether the current device is the last one in the path
     * @return The GroupId of the created Group
     */
    private GroupId createGroup(String serviceId, VlanId vlanId, ConnectPoint egress, boolean first, boolean last) {

        checkNotNull(serviceId);

        DeviceId deviceId = egress.deviceId();

        GroupKey groupKey = getGroupKey(vlanId, egress);
        Group group = groupService.getGroup(deviceId, groupKey);
        GroupId groupId = getGroupId(vlanId, egress);

        if (group != null) {
            log.warn("Group {} already exists in {}", groupKey.toString(), deviceId);
            return groupId;
        }

        GroupBuckets buckets = getGroupBuckets(egress, last);

        GroupDescription groupDescription = new DefaultGroupDescription(
                deviceId,
                GroupDescription.Type.INDIRECT,
                buckets,
                groupKey,
                groupId.id(),
                appId);

        groupService.addGroup(groupDescription);

        return groupId;
    }

    /**
     * Currently creates single group bucket to be used instead of an OUTPUT action.
     *
     * @param egress egress connect point at the particular device
     * @param last indicates whether the current device is the last one in the path
     * @return GroupBuckets which can be used to create a GroupDescription
     */
    private GroupBuckets getGroupBuckets(ConnectPoint egress, boolean last) {

        List<GroupBucket> buckets = Lists.newArrayList();

        TrafficTreatment.Builder treatmentBuilder;
        if (last) {
            treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.popVlan();
        } else {
            treatmentBuilder = DefaultTrafficTreatment.builder();
        }
        TrafficTreatment treatment = treatmentBuilder.setOutput(egress.port()).build();

        buckets.add(DefaultGroupBucket.createIndirectGroupBucket(treatment));

        return new GroupBuckets(buckets);
    }

    /**
     * Returns globally unique group id according to OFDPA 2.0 specification for "L2 Interface" group types.
     *
     * @param vlanId VLAN id of the service
     * @param egress egress connect point at the particular device
     * @return group id
     */
    private GroupId getGroupId(VlanId vlanId, ConnectPoint egress) {
        return new DefaultGroupId((vlanId.toShort()) << 16 | Integer.parseInt(egress.port().toString()));
    }

    /**
     * Returns globally unique group key.
     *
     * @param vlanId VLAN id of the service
     * @param egress egress connect point at the particular device
     * @return group key
     */
    private GroupKey getGroupKey(VlanId vlanId, ConnectPoint egress) {
        //TODO: Create GroupKey in a better way - perhaps the same as GroupId (unique per device)
        return new DefaultGroupKey(Integer.toString(Objects.hash(egress.deviceId(), egress.port(), vlanId)).getBytes());
    }

    @Override
    void applyBandwidthProfileResources(String serviceId, CarrierEthernetUni uni) {

        Dpid dpid = Dpid.dpid(uni.cp().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        // FIXME: Temporary hack: Do not apply meters to OFDPA2.0 switches
        if (sw.softwareDescription().equals("OF-DPA 2.0")) {
            return;
        }

        // Create meters and add them to global MeterId map
        Set<DeviceMeterId> deviceMeterIdSet = deviceMeterIdMap.get(serviceId);
        if (deviceMeterIdSet == null) {
            deviceMeterIdSet = new HashSet<>();
        }
        deviceMeterIdSet.addAll(createMeters(uni));
        deviceMeterIdMap.put(serviceId, deviceMeterIdSet);

        // Apply meters to already installed flows

        Set<FlowRule> newFlowRuleSet = new HashSet<>();

        // Get flow rules belonging to service and having as in_port the UNI connect point
        flowRuleMap.get(serviceId).forEach(flowRule -> {
            PortNumber inPort = ((PortCriterion) flowRule.selector().getCriterion(Criterion.Type.IN_PORT)).port();
            ConnectPoint flowInCp = new ConnectPoint(flowRule.deviceId(), inPort);
            //VlanId flowInVlanId = ((VlanIdCriterion) flowRule.selector().
            //        getCriterion(Criterion.Type.VLAN_VID)).vlanId();
            // TODO: Compare also to the CE-VLAN ID (if it is not null)
            // FIXME: Maybe check only in_port, vlanid, and if there is output port or group action?
            if (uni.cp().equals(flowInCp)) {
            //if (uni.cp().equals(flowInCp) && (uni.ceVlanId() == null || uni.ceVlanId().equals(flowInVlanId))) {
                // Need to add to the flow the meters associated with the same device
                Set<DeviceMeterId> tmpDeviceMeterIdSet = new HashSet<>();
                deviceMeterIdMap.get(serviceId).forEach(deviceMeterId -> {
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
        });

        flowRuleMap.put(serviceId, newFlowRuleSet);
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

    private FlowRule addMeterToFlowRule(FlowRule flowRule, DeviceMeterId deviceMeterId) {

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                .builder(flowRule.treatment());

        tBuilder.add(Instructions.meterTraffic(deviceMeterId.meterId()));

        return createFlowRule(flowRule.deviceId(), flowRule.priority(),
                flowRule.selector(), tBuilder.build(), flowRule.tableId());
    }

    private FlowRule addMetersToFlowRule(FlowRule flowRule, Set<DeviceMeterId> deviceMeterIdSet) {

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                .builder(flowRule.treatment());

        deviceMeterIdSet.forEach(deviceMeterId -> {
            //tBuilder.add(Instructions.meterTraffic(deviceMeterId.meterId()));
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
    void removeAllForwardingResources(CarrierEthernetService service) {
        removeFlowRules(service.id());
        removeGroups(service);
    }

    /**
     * Removes all flow rules installed by the application which are associated with a specific CE service.
     *
     * @param serviceId the CE service id
     * */
    private void removeFlowRules(String serviceId) {
        // Note: A Flow Rule cannot be shared by multiple services due to different VLAN or CE-VLAN ID match.
        Set<FlowRule> flowRuleSet = flowRuleMap.remove(serviceId);
        flowRuleSet.forEach(flowRule -> flowRuleService.removeFlowRules(flowRule));
    }

    /**
     * Removes all groups installed by the application which are associated with a specific CE service.
     *
     * @param service the CE service definition
     * */
    // Note: A Group cannot be shared by multiple services since GroupIds/GroupKeys include the service VLAN ID
    private void removeGroups(CarrierEthernetService service) {

        Set<ConnectPoint> egressCpSet = egressCpMap.remove(service.id());
        Set<ConnectPoint> uniCpSet = new HashSet<>();

        service.uniSet().forEach(uni -> uniCpSet.add(uni.cp()));

        egressCpSet.forEach(egress -> {
            // The connect points associated with UNIs are the ones including the VLAN pop commands, i.e. the "last"
            boolean last = (uniCpSet.contains(egress));
            DeviceId deviceId = egress.deviceId();
            GroupKey groupKey = getGroupKey(service.vlanId(), egress);
            GroupBuckets buckets = getGroupBuckets(egress, last);
            if (groupService.getGroup(deviceId, groupKey) != null) {
                // Note: Removing buckets before removing group in CpQD causes warnings (but is needed in OFDPA2.0)
                Dpid dpid = Dpid.dpid(deviceId.uri());
                OpenFlowSwitch sw = controller.getSwitch(dpid);
                if (sw.softwareDescription().equals("OF-DPA 2.0")) {
                    groupService.removeBucketsFromGroup(
                            deviceId,
                            groupKey,
                            buckets,
                            groupKey,
                            appId);
                }
                log.info("Trying to remove group with key {} from {}", groupKey, deviceId);
                groupService.removeGroup(deviceId, groupKey, appId);
            }
        });
    }

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
