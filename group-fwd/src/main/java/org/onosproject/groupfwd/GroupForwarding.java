/*
 * Copyright 2017 Open Networking Foundation
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
package org.onosproject.groupfwd;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.PathService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
public class GroupForwarding {

    private static final int DEAFULT_PRIORITY = 500;
    private final Logger log = getLogger(getClass());

    protected static KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Integer.class)
            .register(DeviceId.class)
            .build("group-fwd-app");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    FlowRuleService flowRuleService;

    private ApplicationId appId;

    private final SetMultimap<GroupKey, FlowRule> pendingFlows =
            HashMultimap.create();

    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(1, groupedThreads("group-fwd", "group-fwd-%d", log));

    private Runnable groupCheckerTask = new InternalGroupCheckerTask();

    private final HostListener hostListener = new InternalHostListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.groupfwd");
        setupGroups();
        setupFlows();
        hostService.addListener(hostListener);
        deviceService.addListener(deviceListener);
        groupChecker.scheduleAtFixedRate(groupCheckerTask, 0, 5, TimeUnit.SECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        groupChecker.shutdown();
        hostService.removeListener(hostListener);
        deviceService.removeListener(deviceListener);
        cleanUpFlows();
        log.info("Stopped");
    }

    private void cleanUpFlows() {
        Set<FlowRule> flowRules = Sets.newHashSet(flowRuleService.getFlowRulesById(appId));
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());
        Map<DeviceId, Set<Group>> deviceGroups = Maps.newHashMap();

        devices.forEach(device -> {
            Set<Group> groups = Sets.newHashSet(groupService.getGroups(device.id(), appId));
            deviceGroups.put(device.id(), groups);
        });

        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();

        FlowRuleOperationsContext ctx = new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                cleanUpGroups(devices, deviceGroups);
            }
        };

        flowRules.forEach(opsBuilder::remove);

        flowRuleService.apply(opsBuilder.build(ctx));
    }

    private void cleanUpGroups(Set<Device> devices, Map<DeviceId, Set<Group>> deviceGroups) {
        devices.forEach(device -> {
            Set<Group> groups = deviceGroups.get(device.id());
            groups.forEach(group -> groupService.removeGroup(device.id(), group.appCookie(), appId));
        });
    }

    private synchronized void setupGroups() {
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());

        // create groups for each devices in each devices
        devices.forEach(this::processDeviceGroups);

        // creates groups for each host in each devices
        devices.forEach(this::processHostGroups);

    }

    private synchronized void setupFlows() {
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());

        devices.forEach(this::processHostFlows);
    }

    private void processHostFlows(Device device) {
        Set<Host> hosts = Sets.newHashSet(hostService.getHosts());

        hosts.forEach(host -> {
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

            selectorBuilder.matchEthDst(host.mac());

            Integer groupId;
            GroupKey groupKey;
            if (host.location().deviceId().equals(device.id())) {
                // host connect to device, use host group
                groupId = generateHostGroupId(host);

            } else {
                // host not in this device, use target device group
                groupId = generateDeviceGroupId(host.location().deviceId());
            }
            groupKey = generateGroupKey(device.id(), groupId);

            treatmentBuilder.deferred();
            treatmentBuilder.group(new GroupId(groupId));

            FlowRule flowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .makePermanent()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .forDevice(device.id())
                    .withPriority(DEAFULT_PRIORITY)
                    .build();

            addPendingFlow(groupKey, flowRule);

        });
    }

    private void addPendingFlow(GroupKey groupkey, FlowRule flowRule) {
        synchronized (pendingFlows) {
            pendingFlows.put(groupkey, flowRule);
        }
    }

    private Set<FlowRule> fetchPendingFlows(GroupKey groupKey) {
        Set<FlowRule> flowRules;

        synchronized (pendingFlows) {
            flowRules = pendingFlows.removeAll(groupKey);
        }

        return flowRules;
    }

    private void processDeviceGroups(Device device) {
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());

        devices.forEach(targetDevice -> {
            if (device.equals(targetDevice)) {
                // no need to process if device is equals to target device
                return;
            }
            Integer targetDeviceGroupId = generateDeviceGroupId(targetDevice.id());
            GroupKey targetDeviceGroupKey = generateGroupKey(device.id(), targetDeviceGroupId);

            if (!groupExist(device, targetDeviceGroupKey)) {
                GroupBucket deviceBucket = createBucketForDevice(device, targetDevice);

                if (deviceBucket == null) {
                    // no connection between devices
                    return;
                }
                GroupDescription groupDescription = new DefaultGroupDescription(
                        device.id(),
                        GroupDescription.Type.INDIRECT,
                        new GroupBuckets(ImmutableList.of(deviceBucket)),
                        targetDeviceGroupKey,
                        targetDeviceGroupId,
                        appId
                );

                groupService.addGroup(groupDescription);

            }

        });

    }

    private GroupBucket createBucketForDevice(Device sourceDevice, Device targetDevice) {

        PortNumber outputPort = getOutPortForDeviceLink(sourceDevice, targetDevice);

        if (outputPort == null) {
            // no path between two devices
            return null;
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        treatmentBuilder.setOutput(outputPort);
        return DefaultGroupBucket.createIndirectGroupBucket(treatmentBuilder.build());

    }

    private PortNumber getOutPortForDeviceLink(Device sourceDevice, Device targetDevice) {
        Set<Path> paths = pathService.getPaths(sourceDevice.id(), targetDevice.id());

        if (paths == null || paths.isEmpty()) {
            return null;
        }

        Path path = paths.iterator().next(); // use first path

        if (path.links().isEmpty()) {
            // XXX: will this happened ?
            return null;
        }
        // first link should contains devive+port -> device+port

        Link link = path.links().get(0);
        return link.src().port();
    }

    private Integer generateDeviceGroupId(DeviceId deviceId) {
        // make sure first bit is 1 for device group id
        return deviceId.hashCode() | 0x80000000;
    }

    private void processHostGroups(Device device) {
        Set<Host> hosts = Sets.newHashSet(hostService.getHosts());

        // generates group id for each host
        hosts.forEach(host -> {

            if (!hostService.getConnectedHosts(device.id()).contains(host)) {
                // host not connected to this device, skip
                return;
            }
            Integer hostGroupId = generateHostGroupId(host);
            GroupKey hostGroupKey = generateGroupKey(device.id(), hostGroupId);

            if (!groupExist(device, hostGroupKey)) {
                GroupBucket hostBucket = createBucketsForHost(host);

                GroupDescription groupDescription = new DefaultGroupDescription(
                        device.id(),
                        GroupDescription.Type.INDIRECT,
                        new GroupBuckets(ImmutableList.of(hostBucket)),
                        hostGroupKey,
                        hostGroupId,
                        appId
                );
                groupService.addGroup(groupDescription);
            }
        });

    }

    private GroupBucket createBucketsForHost(Host host) {
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        treatmentBuilder.setOutput(host.location().port());
        return DefaultGroupBucket.createIndirectGroupBucket(treatmentBuilder.build());
    }

    private GroupKey generateGroupKey(DeviceId deviceId, Integer groupId) {
        int hashed = Objects.hash(deviceId, groupId);
        return new DefaultGroupKey(appKryo.serialize(hashed));
    }

    private boolean groupExist(Device device, GroupKey groupKey) {
        return groupService.getGroup(device.id(), groupKey) != null;
    }

    private Integer generateHostGroupId(Host host) {
        // first 1 bit should be 0 for host group id
        return host.mac().hashCode() & 0x7FFFFFFF;
    }

    class InternalGroupCheckerTask implements Runnable {

        @Override
        public void run() {
            Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());

            devices.forEach(device -> {
                Set<Group> groups = Sets.newHashSet(groupService.getGroups(device.id()));
                groups.forEach(group -> {
                    Set<FlowRule> flowRulesToInstall = fetchPendingFlows(group.appCookie());

                    if (flowRulesToInstall != null && !flowRulesToInstall.isEmpty()) {
                        flowRulesToInstall.forEach(flowRuleService::applyFlowRules);
                    }
                });
            });
        }
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent hostEvent) {
            switch (hostEvent.type()) {
                case HOST_ADDED:
                case HOST_UPDATED:
                    setupGroups();
                    setupFlows();
                    break;
                case HOST_REMOVED:
                    removeHostFlows(hostEvent.subject());
                    // group removal operation will operate after flow removed.
                    break;
                default:
                    break;
            }
        }
    }

    private void removeHostFlows(Host host) {
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());
        devices.forEach(device -> {
            Integer groupId;

            if (host.location().deviceId().equals(device.id())) {
                // host connect to device, use host group
                groupId = generateHostGroupId(host);

            } else {
                // host not in this device, use target device group
                groupId = generateDeviceGroupId(host.location().deviceId());
            }

            Set<FlowRule> flowRules = getRelatedFlows(groupId);

            FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
            flowRules.forEach(opsBuilder::remove);
            FlowRuleOperationsContext ctx = new FlowRuleOperationsContext() {

                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    // group removal operation will operate after flow removed.
                    removeHostGroup(device, host);
                }
            };
            FlowRuleOperations ops = opsBuilder.build(ctx);
            flowRuleService.apply(ops);
        });
    }

    private void removeHostGroup(Device device, Host host) {
        Integer hostGroupId = generateHostGroupId(host);
        GroupKey hostGroupKey = generateGroupKey(device.id(), hostGroupId);

        if (groupExist(device, hostGroupKey)) {
            groupService.removeGroup(device.id(), hostGroupKey, appId);
        }
    }

    private Set<FlowRule> getRelatedFlows(Integer groupId) {
        Set<FlowRule> flowRules = Sets.newHashSet(flowRuleService.getFlowRulesById(appId));

        return flowRules.stream()
                .filter(flowRule ->
                                flowRule.treatment().allInstructions().stream()
                                        .filter(inst -> inst.type() == Instruction.Type.GROUP)
                                        .map(inst -> (Instructions.GroupInstruction) inst)
                                        .anyMatch(inst -> inst.groupId().equals(new GroupId(groupId))))
                .collect(Collectors.toSet());

    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent deviceEvent) {
            switch (deviceEvent.type()) {
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                    setupGroups();
                    setupFlows();
                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    removeFlowsRelatedToDevice(deviceEvent.subject());
                    break;
                default:
                    break;
            }

        }
    }

    private void removeFlowsRelatedToDevice(Device targetDevice) {
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());

        devices.forEach(device -> {

            if (device.equals(targetDevice)) {
                return;
            }

            Integer groupId = generateDeviceGroupId(targetDevice.id());

            Set<FlowRule> flowRules = getRelatedFlows(groupId);

            FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
            flowRules.forEach(opsBuilder::remove);
            FlowRuleOperationsContext ctx = new FlowRuleOperationsContext() {

                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    // group removal operation will operate after flow removed.
                    removeDeviceGroups(device, targetDevice);
                }
            };
            FlowRuleOperations ops = opsBuilder.build(ctx);
            flowRuleService.apply(ops);
        });
    }

    private void removeDeviceGroups(Device device, Device targetDevice) {
        Integer groupId = generateDeviceGroupId(targetDevice.id());
        GroupKey deviceGroupeKey = generateGroupKey(device.id(), groupId);

        if (groupExist(device, deviceGroupeKey)) {
            groupService.removeGroup(device.id(), deviceGroupeKey, appId);
        }
    }
}
