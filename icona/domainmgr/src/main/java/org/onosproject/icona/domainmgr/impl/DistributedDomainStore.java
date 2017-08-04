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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.onlab.util.Identifier;
import org.onlab.util.KryoNamespace;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainmgr.api.DomainEvent;
import org.onosproject.icona.domainmgr.api.DomainStore;
import org.onosproject.icona.domainmgr.api.DomainStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Link;
import org.onosproject.net.ConnectPoint;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.SetEventListener;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.Serializer;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.icona.domainmgr.api.DomainEvent.Type;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import static org.onosproject.icona.domainmgr.api.DomainEvent.Type.DOMAIN_ADDED;
import static org.onosproject.icona.domainmgr.api.DomainEvent.Type.DOMAIN_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed domain store implementation.
 */
@Component(immediate = true)
@Service
public class DistributedDomainStore extends AbstractStore<DomainEvent, DomainStoreDelegate>
        implements DomainStore {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final Logger log = getLogger(getClass());

    private DistributedSet<DomainId> domainIds;
    private final SetEventListener<DomainId> domainIdSetEventListener = new InternalMapListener();

    private ConsistentMap<DomainId, Set<DeviceId>> domainIdDeviceIdsConsistentMap;
    private Map<DomainId, Set<DeviceId>> domainIdDeviceIdsMap;

    private ConsistentMap<DomainId, Set<HostId>> domainIdHostsIdsConsistentMap;
    private Map<DomainId, Set<HostId>> domainIdHostIdsMap;

    private ConsistentMap<Pair<DomainId, DomainId>, Set<Link>> domainIdLinkSetConsistentMap;
    private Map<Pair<DomainId, DomainId>, Set<Link>> domainIdLinkSetMap;

    private ConsistentMap<DeviceId, Map<PortNumber, ConnectPoint>> virtualPortToLocalHostConsistentMap;
    private Map<DeviceId, Map<PortNumber, ConnectPoint>> virtualPortToLocalHostMap;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(Identifier.class)
                    .register(DomainId.class)
                    .register(ImmutablePair.class)
                    .build());

    @Activate
    public void activate() {

        domainIds = storageService.<DomainId>setBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-domainId")
                .withRelaxedReadConsistency()
                .build()
                .asDistributedSet();
        domainIds.addListener(domainIdSetEventListener);

        domainIdDeviceIdsConsistentMap = storageService.<DomainId, Set<DeviceId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-domain-device-ids")
                .withRelaxedReadConsistency()
                .build();
        domainIdDeviceIdsMap = domainIdDeviceIdsConsistentMap.asJavaMap();

        domainIdHostsIdsConsistentMap = storageService.<DomainId, Set<HostId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-domain-host-ids")
                .withRelaxedReadConsistency()
                .build();
        domainIdHostIdsMap = domainIdHostsIdsConsistentMap.asJavaMap();

        domainIdLinkSetConsistentMap = storageService.<Pair<DomainId, DomainId>, Set<Link>>
                consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-domain-links")
                .withRelaxedReadConsistency()
                .build();
        domainIdLinkSetMap = domainIdLinkSetConsistentMap.asJavaMap();

        virtualPortToLocalHostConsistentMap = storageService
                .<DeviceId, Map<PortNumber, ConnectPoint>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-virtual-port-host-mapping")
                .withRelaxedReadConsistency()
                .build();
        virtualPortToLocalHostMap = virtualPortToLocalHostConsistentMap.asJavaMap();

        log.info("Started");

    }

    @Deactivate
    public void deactivate() {
        domainIds.removeListener(domainIdSetEventListener);
        log.info("Stopped");

    }

    @Override
    public Set<DomainId> getDomainIds() {
        return ImmutableSet.copyOf(domainIds);
    }

    @Override
    public Set<DeviceId> getDeviceIds(DomainId domainId) {
        checkState(domainExists(domainId), "Domain id unknown");
        return ImmutableSet.copyOf(domainIdDeviceIdsMap.get(domainId));
    }

    @Override
    public void addDomain(DomainId domainId) {
        domainIds.add(domainId);
    }

    @Override
    public void removeDomain(DomainId domainId) {
        domainIds.remove(domainId);
        clear(domainId);
    }

    @Override
    public void addDevice(DomainId domainId, DeviceId deviceId) {
        checkState(domainExists(domainId), "Domain id unknown");
        domainIdDeviceIdsMap.compute(domainId, (k, set) -> {
            if (set == null) {
               set = new HashSet<>();
            }
            set.add(deviceId);
            return set;
        });
    }

    @Override
    public void removeDevice(DomainId domainId, DeviceId deviceId) {
        checkState(domainExists(domainId), "Domain id unknown");
        domainIdDeviceIdsMap.computeIfPresent(domainId, (k, existingSet) -> {
            if (existingSet.contains(deviceId)) {
                existingSet.remove(deviceId);
                return existingSet;
            } else {
                return existingSet;
            }
        });
    }

    @Override
    public Set<HostId> getHostIds(DomainId domainId) {
        checkState(domainExists(domainId), "Domain id unknown");
        return ImmutableSet.copyOf(domainIdHostIdsMap.get(domainId));
    }

    @Override
    public void addHost(DomainId domainId, HostId hostId) {
        checkState(domainExists(domainId), "Domain id unknown");
        domainIdHostIdsMap.compute(domainId, (k, set) -> {
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(hostId);
            return set;
        });
    }

    @Override
    public void removeHost(DomainId domainId, HostId hostId) {
        checkState(domainExists(domainId), "Domain id unknown");
        domainIdHostIdsMap.computeIfPresent(domainId, (k, existingSet) -> {
            if (existingSet.contains(hostId)) {
                existingSet.remove(hostId);
                return existingSet;
            } else {
                return existingSet;
            }
        });
    }

    @Override
    public Set<Link> getInterLinks(Pair<DomainId, DomainId> endDomains) {
        checkState(domainExists(endDomains.getLeft()), "Domain id unknown");
        checkState(domainExists(endDomains.getRight()), "Domain id unknown");
        return ImmutableSet.copyOf(domainIdLinkSetMap.get(endDomains));
    }

    @Override
    public void addOrUpdateInterLink(Pair<DomainId, DomainId> endDomains, Link link) {
        checkState(domainExists(endDomains.getLeft()), "Domain id unknown");
        checkState(domainExists(endDomains.getRight()), "Domain id unknown");
        domainIdLinkSetMap.compute(endDomains, (k, set) -> {
           if (set == null) {
               set = new HashSet<>();
           }
           set.add(link);
            return set;
        });
    }

    @Override
    public void removeInterLink(Pair<DomainId, DomainId> endDomains, Link link) {
        checkState(domainExists(endDomains.getLeft()), "Domain id unknown");
        checkState(domainExists(endDomains.getRight()), "Domain id unknown");
        domainIdLinkSetMap.computeIfPresent(endDomains, (k, existingSet) -> {
            if (existingSet.contains(link)) {
                existingSet.remove(link);
                return existingSet;
            } else {
                return existingSet;
            }
        });
    }

    @Override
    public void setVirtualPortToPortMapping(DeviceId deviceId, Map<PortNumber, ConnectPoint> map) {
        virtualPortToLocalHostMap.put(deviceId, map);
    }

    @Override
    public Map<PortNumber, ConnectPoint> getVirtualPortToPortMapping(DeviceId deviceId) {
        return ImmutableMap.copyOf(virtualPortToLocalHostMap.get(deviceId));
    }

    private void clear(DomainId domainId) {
        Set<Pair<DomainId, DomainId>> domainPairs = new HashSet<>();
        // find all domains connected with the one to be removed and remove related links
        domainIdLinkSetMap.keySet().forEach(endDomains -> {
            if (endDomains.getLeft().equals(domainId) ||
                    endDomains.getRight().equals(domainId)) {
                domainPairs.add(endDomains);
            }
        });
        domainPairs.forEach(pair -> domainIdLinkSetMap.remove(pair));
        domainIdDeviceIdsMap.remove(domainId);
        domainIdHostIdsMap.remove(domainId);
    }

    private class InternalMapListener implements SetEventListener<DomainId> {
        @Override
        public void event(SetEvent<DomainId> event) {
            Type type;
            switch (event.type()) {
                case ADD:
                    type = DOMAIN_ADDED;
                    break;
                case REMOVE:
                    type = DOMAIN_REMOVED;
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
                    return;
            }
            notifyDelegate(new DomainEvent(type, event.entry()));
        }
    }

    private boolean domainExists(DomainId domainId) {
        checkNotNull(domainId, "domain identifier is null");
        return domainIds.contains(domainId);
    }
}