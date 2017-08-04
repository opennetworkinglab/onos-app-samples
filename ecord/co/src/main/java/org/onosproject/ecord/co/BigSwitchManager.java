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
package org.onosproject.ecord.co;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.OchPort;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.OmsPort;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Listens for edge and port changes in the underlying data path and
 * exposes a big switch abstraction.
 */
@Component(immediate = true)
@Service
public class BigSwitchManager
        extends AbstractListenerManager<BigSwitchEvent, BigSwitchListener>
        implements BigSwitchService {


    private static final Logger log = getLogger(BigSwitchManager.class);

    // annotation on a big switch port
    public static final String REALIZED_BY = "bigswitch:realizedBy";

    private static final String PORT_MAP = "ecord-port-map";
    private static final String PORT_COUNTER = "ecord-port-counter";
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    /**
     * Holds the physical port to virtual port number mapping.
     */
    private ConsistentMap<ConnectPoint, Long> portMap;

    // Counter for virtual port numbers
    private AtomicCounter portCounter;

    // TODO listen to portMap event and populate Cache to properly deal with
    // distributed deployment.
    /**
     * History of physical port to virtual port number mapping.
     * Intended to avoid virtual port number explosion.
     */
    private LoadingCache<ConnectPoint, Long> p2vMap;

    // TODO: Add other listeners once we decide what an edge really is
    private EdgePortListener edgeListener = new InternalEdgeListener();
    private DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    public void activate() {
        deviceService = opticalView(deviceService);
        portMap = storageService.<ConnectPoint, Long>consistentMapBuilder()
                .withName(PORT_MAP)
                .withSerializer(SERIALIZER)
                .build();
        portCounter = storageService.atomicCounterBuilder()
                .withName(PORT_COUNTER)
                .withMeteringDisabled()
                .build()
                .asAtomicCounter();
        p2vMap = CacheBuilder.newBuilder()
                .build(CacheLoader.from(portCounter::getAndIncrement));

        eventDispatcher.addSink(BigSwitchEvent.class, listenerRegistry);
        portCounter.compareAndSet(0, 1);
        edgePortService.addListener(edgeListener);
        deviceService.addListener(deviceListener);
        buildPorts();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        edgePortService.removeListener(edgeListener);
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    @Override
    public List<PortDescription> getPorts() {
        return portMap.keySet().stream()
                .map(cp -> toVirtualPortDescription(cp))
                .filter(cp -> cp != null)
                .collect(Collectors.toList());
    }

    @Override
    public PortNumber getPort(ConnectPoint port) {
        // XXX error-check and seriously think about a better method definition.
        Versioned<Long> portNo = portMap.get(port);
        if (Versioned.valueOrNull(portNo) != null) {
            return PortNumber.portNumber(portNo.value());
        } else {
            return null;
        }
    }

    /**
     * Convert connect point to port description.
     *
     * @param cp connect point of physical port
     * @return port description of virtual big switch port
     */
    private PortDescription toVirtualPortDescription(ConnectPoint cp) {
        Port p = deviceService.getPort(cp.deviceId(), cp.port());
        if (p == null) {
            return null;
        }
        // copy annotation
        DefaultAnnotations.Builder annot = DefaultAnnotations.builder();
        p.annotations().keys()
                .forEach(k -> annot.set(k, p.annotations().value(k)));
        // add annotation about underlying physical connect-point
        annot.set(REALIZED_BY, String.format("%s/%s", cp.deviceId().toString(),
                                                      cp.port().toString()));
        Long vPortNo = Versioned.valueOrNull(portMap.get(cp));
        if (vPortNo == null) {
            return null;
        }
        PortNumber portNumber = PortNumber.portNumber(vPortNo);

        // FIXME remove the code specific to optical port types
        switch (p.type()) {
            case OMS:
                OmsPort oms = (OmsPort) p;
                return omsPortDescription(
                        portNumber,
                        p.isEnabled(),
                        oms.minFrequency(),
                        oms.maxFrequency(),
                        oms.grid(),
                        annot.build());
            case OCH:
                OchPort och = (OchPort) p;
                return ochPortDescription(
                        portNumber,
                        p.isEnabled(),
                        och.signalType(),
                        och.isTunable(),
                        och.lambda(),
                        annot.build());
            case ODUCLT:
                OduCltPort odu = (OduCltPort) p;
                return oduCltPortDescription(
                        portNumber,
                        p.isEnabled(),
                        odu.signalType(),
                        annot.build()
                );
            default:
                return new DefaultPortDescription(
                        portNumber,
                        p.isEnabled(),
                        p.type(),
                        p.portSpeed(),
                        annot.build()
                );
        }
    }

    private class InternalEdgeListener implements EdgePortListener {
        @Override
        public boolean isRelevant(EdgePortEvent event) {
            // Only listen for real devices
            Device d = deviceService.getDevice(event.subject().deviceId());

            return d != null && !d.type().equals(Device.Type.VIRTUAL);
        }

        @Override
        public void event(EdgePortEvent event) {
            log.debug("Edge event {} {}", event.subject(), event.type());
            BigSwitchEvent.Type bigSwitchEvent;

            PortDescription descr = null;
            switch (event.type()) {
                case EDGE_PORT_ADDED:
                    portMap.put(event.subject(), getVirtualPortNumber(event.subject()));
                    descr = toVirtualPortDescription(event.subject());
                    bigSwitchEvent = BigSwitchEvent.Type.PORT_ADDED;
                    break;
                case EDGE_PORT_REMOVED:
                    descr = toVirtualPortDescription(event.subject());
                    portMap.remove(event.subject());
                    bigSwitchEvent = BigSwitchEvent.Type.PORT_REMOVED;
                    break;
                default:
                    return;
            }
            if (descr != null) {
                post(new BigSwitchEvent(bigSwitchEvent, descr));
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public boolean isRelevant(DeviceEvent event) {
            // Only listen for real devices
            return !event.subject().type().equals(Device.Type.VIRTUAL);
        }

        @Override
        public void event(DeviceEvent event) {
            log.debug("Device event {} {} {}", event.subject(), event.port(), event.type());

            switch (event.type()) {
                // Ignore most of these for now
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                case DEVICE_UPDATED:
                case PORT_ADDED:
                case PORT_REMOVED:
                case PORT_STATS_UPDATED:
                    break;
                case PORT_UPDATED:
                    // Update if state of existing edge changed
                    ConnectPoint cp = new ConnectPoint(event.subject().id(), event.port().number());
                    if (portMap.containsKey(cp)) {
                        post(new BigSwitchEvent(BigSwitchEvent.Type.PORT_UPDATED, toVirtualPortDescription(cp)));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void buildPorts() {
        edgePortService.getEdgePoints()
                .forEach(cp -> portMap.put(cp, getVirtualPortNumber(cp)));
    }

    private Long getVirtualPortNumber(ConnectPoint cp) {
        return p2vMap.getUnchecked(cp);
    }
}
