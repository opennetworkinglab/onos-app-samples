/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.icona.domainmgr.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.icona.domainmgr.api.DomainEvent;
import org.onosproject.icona.domainmgr.api.DomainListener;
import org.onosproject.icona.domainmgr.api.DomainService;
import org.onosproject.icona.domainmgr.api.DomainStore;
import org.onosproject.icona.domainmgr.api.DomainStoreDelegate;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.HostId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exposes domain topology elements and listen for updates of such elements.
 */
@Component(immediate = true)
@Service
public class DomainManager extends AbstractListenerManager<DomainEvent, DomainListener>
        implements DomainService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String DOMAIN_ID = "domainId";
    private static final String SRC_DOMAIN_ID = "srcDomainId";
    private static final String DST_DOMAIN_ID = "dstDomainId";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DomainStore domainStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    protected DomainStoreDelegate delegate = this::post;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();
    private final LinkListener linkListener = new InternalLinkListener();

    @Activate
    public void activate() {
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        linkService.addListener(linkListener);
        domainStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        linkService.removeListener(linkListener);
        domainStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void registerDomainId(DomainId domainId) {
        checkNotNull(domainId);
        domainStore.addDomain(domainId);
    }

    @Override
    public void unregisterDomainId(DomainId domainId) {
        checkNotNull(domainId);
        domainStore.removeDomain(domainId);
    }

    @Override
    public Set<DomainId> getDomainIds() {
        return domainStore.getDomainIds();
    }

    @Override
    public Set<DeviceId> getDeviceIds(DomainId domainId) {
        return domainStore.getDeviceIds(domainId);
    }

    @Override
    public Set<Device> getDevices(DomainId domainId) {
        Set<DeviceId> deviceIds = domainStore.getDeviceIds(domainId);
        Set<Device> devices = new HashSet<>();
        deviceService.getDevices().forEach(device -> {
            if (deviceIds.contains(device.id())) {
                devices.add(device);
            }
        });
        return ImmutableSet.copyOf(devices);
    }

    @Override
    public Set<HostId> getHostIds(DomainId domainId) {
        return domainStore.getHostIds(domainId);
    }

    @Override
    public Set<Host> getHosts(DomainId domainId) {
        Set<HostId> hostIds = domainStore.getHostIds(domainId);
        Set<Host> hosts = new HashSet<>();
        hostService.getHosts().forEach(host -> {
            if (hostIds.contains(host.id())) {
                hosts.add(host);
            }
        });
        return ImmutableSet.copyOf(hosts);
    }

    @Override
    public Set<Link> getInterLinks(Pair<DomainId, DomainId> endDomains) {
        return domainStore.getInterLinks(endDomains);
    }

    @Override
    public Set<Link> getIntraLinks(DomainId domainId) {
        Set<Device> domDevices = getDevices(domainId);
        Set<Link> intralinks = new HashSet<>();
        domDevices.forEach(device -> {
            Set<Link> devLinks = linkService.getDeviceLinks(device.id());
            devLinks.forEach(link -> {
                if (!link.annotations().keys().contains(SRC_DOMAIN_ID)) {
                    intralinks.add(link);
                }
            });
        });
        return ImmutableSet.copyOf(intralinks);
    }

    @Override
    public Map<PortNumber, ConnectPoint> getVirtualPortToPortMapping(DeviceId deviceId) {
        return domainStore.getVirtualPortToPortMapping(deviceId);
    }

    @Override
    public void setVirtualPortToPortMapping(DeviceId deviceId, Map<PortNumber, ConnectPoint> map) {
        domainStore.setVirtualPortToPortMapping(deviceId, map);
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (!device.annotations().keys().contains(DOMAIN_ID)) {
                return;
            }
            DomainId domainId = DomainId.domainId(
                    device.annotations().value(DOMAIN_ID));
            switch (event.type()) {
                case DEVICE_ADDED:
                    domainStore.addDevice(domainId, device.id());
                    break;
                case DEVICE_REMOVED:
                    domainStore.removeDevice(domainId, device.id());
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!host.annotations().keys().contains(DOMAIN_ID)) {
                return;
            }
            DomainId domainId = DomainId.domainId(
                    host.annotations().value(DOMAIN_ID));
            switch (event.type()) {
                case HOST_ADDED:
                    domainStore.addHost(domainId, host.id());
                    break;
                case HOST_REMOVED:
                    domainStore.removeHost(domainId, host.id());
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
        }
    }

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            Link link = event.subject();
            if (!link.annotations().keys().contains(SRC_DOMAIN_ID) ||
                    !link.annotations().keys().contains(DST_DOMAIN_ID)) {
                return;
            }
            DomainId srcDomainId = DomainId.domainId(
                    link.annotations().value(SRC_DOMAIN_ID));
            DomainId dstDomainId = DomainId.domainId(
                    link.annotations().value(DST_DOMAIN_ID));
            Pair<DomainId, DomainId> endDomains = Pair.of(srcDomainId, dstDomainId);
            switch (event.type()) {
                case LINK_ADDED:
                case LINK_UPDATED:
                    domainStore.addOrUpdateInterLink(endDomains, link);
                    break;
                case LINK_REMOVED:
                    domainStore.removeInterLink(endDomains, link);
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
        }
    }
}