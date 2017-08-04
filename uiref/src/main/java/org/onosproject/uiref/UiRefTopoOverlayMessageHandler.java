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

package org.onosproject.uiref;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.TopoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Message handler for Topology overlay events.
 */
public class UiRefTopoOverlayMessageHandler extends UiMessageHandler {

    private static final String UI_REF_TOPOV_DISPLAY_START = "uiRefTopovDisplayStart";
    private static final String UI_REF_TOPOV_DISPLAY_UPDATE = "uiRefTopovDisplayUpdate";
    private static final String UI_REF_TOPOV_DISPLAY_STOP = "uiRefTopovDisplayStop";
    private static final String UI_REF_TOPOV_DEV_PORTS_REQ = "uiRefTopovDevicePortsReq";
    private static final String UI_REF_TOPOV_DEV_PORTS_RESP = "uiRefTopovDevicePortsResp";
    private static final String UI_REF_TOPOV_DEV_PORTS_OP = "uiRefTopovDevicePortFakeOp";

    private static final String ID = "id";

    private static final String PORTS = "ports";
    private static final String SPEED = "speed";
    private static final String TYPE = "type";
    private static final String MODE = "mode";

    private static final String DEVICE = "device";
    private static final String PORT = "port";
    private static final String FOO = "foo";
    private static final String BAR = "bar";

    private static final long UPDATE_PERIOD_MS = 1000;

    private static final Link[] EMPTY_LINK_SET = new Link[0];

    private enum Mode { IDLE, MOUSE, LINK }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;
    private HostService hostService;
    private LinkService linkService;

    private final Timer timer = new Timer("ui-ref-overlay");
    private TimerTask demoTask = null;
    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;
    private Link[] linkSet = EMPTY_LINK_SET;
    private int linkIndex;


    // ===============-=-=-=-=-=-======================-=-=-=-=-=-=-================================


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
        hostService = directory.get(HostService.class);
        linkService = directory.get(LinkService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
                new DisplayUpdateHandler(),
                new DisplayStopHandler(),
                new DevicePortHandler(),
                new PortFakeOpHandler()
        );
    }

    // === -------------------------
    // === Handler classes

    private final class DisplayStartHandler extends RequestHandler {
        DisplayStartHandler() {
            super(UI_REF_TOPOV_DISPLAY_START);
        }

        @Override
        public void process(ObjectNode payload) {
            String mode = string(payload, MODE);

            log.debug("Start Display: mode [{}]", mode);
            clearState();
            clearForMode();

            switch (mode) {
                case "mouse":
                    currentMode = Mode.MOUSE;
                    cancelTask();
                    sendMouseData();
                    break;

                case "link":
                    currentMode = Mode.LINK;
                    scheduleTask();
                    initLinkSet();
                    sendLinkData();
                    break;

                default:
                    currentMode = Mode.IDLE;
                    cancelTask();
                    break;
            }
        }
    }

    private final class DisplayUpdateHandler extends RequestHandler {
        DisplayUpdateHandler() {
            super(UI_REF_TOPOV_DISPLAY_UPDATE);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            log.debug("Update Display: id [{}]", id);
            if (!Strings.isNullOrEmpty(id)) {
                updateForMode(id);
            } else {
                clearForMode();
            }
        }
    }

    private final class DisplayStopHandler extends RequestHandler {
        DisplayStopHandler() {
            super(UI_REF_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("Stop Display");
            cancelTask();
            clearState();
            clearForMode();
        }
    }

    private final class DevicePortHandler extends RequestHandler {
        DevicePortHandler() {
            super(UI_REF_TOPOV_DEV_PORTS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            log.debug("Request ports for device [{}]", id);
            sendPortDataForDevice(id);
        }
    }

    private final class PortFakeOpHandler extends RequestHandler {
        PortFakeOpHandler() {
            super(UI_REF_TOPOV_DEV_PORTS_OP);
        }

        @Override
        public void process(ObjectNode payload) {
            String device = string(payload, DEVICE);
            String port = string(payload, PORT);

// TODO: re-instate boolean results once onos-app-samples -> onos dep. fixed
//            boolean foo = bool(payload, FOO);
//            boolean bar = bool(payload, BAR);

            // NOTE: we won't go any further with this example, but obviously
            //       we could initiate some action on the device and port
            //       selected by the user from the Topology view in the Web UI.
            log.info("FAKE-op request device {} port {}", device, port);
//            log.info("    options FOO={}, BAR={}", foo, bar);
        }
    }

    // === ------------

    private void sendPortDataForDevice(String id) {
        try {
            DeviceId did = DeviceId.deviceId(id);
            List<Port> ports = deviceService.getPorts(did);
            log.debug("Sending port data for device {} (#ports: {})", did, ports.size());
            ArrayNode portArray = arrayNode();
            for (Port p : ports) {
                // don't bother to send logical ports
                if (p.number().isLogical()) {
                    continue;
                }
                // NOTE: we could just add the port numbers (as longs) to the
                //       array and have done, but let's demonstrate how we
                //       might build more complex record types...
                portArray.add(portData(p));
            }

            ObjectNode payload = objectNode();
            payload.set(PORTS, portArray);
            payload.put(ID, id);
            sendMessage(JsonUtils.envelope(UI_REF_TOPOV_DEV_PORTS_RESP, payload));

        } catch (Exception e) {
            log.warn("[port data] Unable to process ID [{}]", id);
        }
    }

    private ObjectNode portData(Port p) {
        ObjectNode node = objectNode();
        node.put(ID, p.number().toLong());
        node.put(SPEED, p.portSpeed());
        node.put(TYPE, p.type().name());
        return node;
    }


    private void clearState() {
        currentMode = Mode.IDLE;
        elementOfNote = null;
        linkSet = EMPTY_LINK_SET;
    }

    private void updateForMode(String id) {
        log.debug("host service: {}", hostService);
        log.debug("device service: {}", deviceService);

        try {
            HostId hid = HostId.hostId(id);
            log.debug("host id {}", hid);
            elementOfNote = hostService.getHost(hid);
            log.debug("host element {}", elementOfNote);

        } catch (Exception e) {
            try {
                DeviceId did = DeviceId.deviceId(id);
                log.debug("device id {}", did);
                elementOfNote = deviceService.getDevice(did);
                log.debug("device element {}", elementOfNote);

            } catch (Exception e2) {
                log.warn("Unable to process ID [{}]", id);
                elementOfNote = null;
            }
        }

        switch (currentMode) {
            case MOUSE:
                sendMouseData();
                break;

            case LINK:
                sendLinkData();
                break;

            default:
                break;
        }

    }

    private void clearForMode() {
        sendHighlights(new Highlights());
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }


    private void sendMouseData() {
        if (elementOfNote != null && elementOfNote instanceof Device) {
            DeviceId devId = (DeviceId) elementOfNote.id();
            Set<Link> links = linkService.getDeviceEgressLinks(devId);
            Highlights highlights = fromLinks(links, devId);
            addDeviceBadge(highlights, devId, links.size());
            sendHighlights(highlights);
        }
        // Note: could also process Host, if available
    }

    private void addDeviceBadge(Highlights h, DeviceId devId, int n) {
        DeviceHighlight dh = new DeviceHighlight(devId.toString());
        dh.setBadge(createBadge(n));
        h.add(dh);
    }

    private NodeBadge createBadge(int n) {
        NodeBadge.Status status = n > 3 ? NodeBadge.Status.ERROR : NodeBadge.Status.WARN;
        String noun = n > 3 ? "(critical)" : "(problematic)";
        String msg = "Egress links: " + n + " " + noun;
        return NodeBadge.number(status, n, msg);
    }

    private Highlights fromLinks(Set<Link> links, DeviceId devId) {
        DemoLinkMap linkMap = new DemoLinkMap();
        if (links != null) {
            log.debug("Processing {} links", links.size());
            links.forEach(linkMap::add);
        } else {
            log.debug("No egress links found for device {}", devId);
        }

        Highlights highlights = new Highlights();

        for (DemoLink dlink : linkMap.biLinks()) {
            dlink.makeImportant().setLabel("Yo!");
            highlights.add(dlink.highlight(null));
        }
        return highlights;
    }

    private void initLinkSet() {
        Set<Link> links = new HashSet<>();
        for (Link link : linkService.getActiveLinks()) {
            links.add(link);
        }
        linkSet = links.toArray(new Link[links.size()]);
        linkIndex = 0;
        log.debug("initialized link set to {}", linkSet.length);
    }

    private void sendLinkData() {
        DemoLinkMap linkMap = new DemoLinkMap();
        for (Link link : linkSet) {
            linkMap.add(link);
        }
        DemoLink dl = linkMap.add(linkSet[linkIndex]);
        dl.makeImportant().setLabel(Integer.toString(linkIndex));
        log.debug("sending link data (index {})", linkIndex);

        linkIndex += 1;
        if (linkIndex >= linkSet.length) {
            linkIndex = 0;
        }

        Highlights highlights = new Highlights();
        for (DemoLink dlink : linkMap.biLinks()) {
            highlights.add(dlink.highlight(null));
        }

        sendHighlights(highlights);
    }

    private synchronized void scheduleTask() {
        if (demoTask == null) {
            log.debug("Starting up demo task...");
            demoTask = new DisplayUpdateTask();
            timer.schedule(demoTask, UPDATE_PERIOD_MS, UPDATE_PERIOD_MS);
        } else {
            log.debug("(demo task already running");
        }
    }

    private synchronized void cancelTask() {
        if (demoTask != null) {
            demoTask.cancel();
            demoTask = null;
        }
    }


    private class DisplayUpdateTask extends TimerTask {
        @Override
        public void run() {
            try {
                switch (currentMode) {
                    case LINK:
                        sendLinkData();
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                log.warn("Unable to process demo task: {}", e.getMessage());
                log.debug("Oops", e);
            }
        }
    }
}
