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

package org.onosproject.sdxl2;


import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of the SdxL2MonitoringService.
 */
public class SdxL2MonitoringManager implements SdxL2MonitoringService {

    private static Logger log = LoggerFactory.getLogger(SdxL2MonitoringManager.class);
    private ApplicationId appId;
    private final IntentService intentService;
    private final EdgePortService edgePortService;

    /**
     * It is a local cache for the Intents' events.
     */
    private ConcurrentMap<Key, IntentEvent> intentsState;

    /**
     * Last time intentsState has been updated.
     */
    private long lastIntentUpdate;

    /**
     * It is a local cache for the edge ports related events.
     */
    private ConcurrentMap<ConnectPoint, EdgePortEvent> edgeportsState;

    /**
     * Last time edgeportsState has been updated.
     */
    private long lastEdgePortUpdate;

    /**
     * A kind of timeout which causes a manual update.
     */
    private static int deltaUpdate = 60000;

    private InternalIntentListener intentListener;
    private InternalEdgePortListener edgePortListener;

    /**
     * Creates an SdxL2MonitoringManager.
     *
     * @param sdxl2id application id.
     * @param intentService reference to the Intent service.
     * @param edgePortService reference to the EdgePort service.
     */
    public SdxL2MonitoringManager(ApplicationId sdxl2id, IntentService intentService, EdgePortService edgePortService) {
        this.appId = sdxl2id;
        this.intentListener = new InternalIntentListener();
        this.edgePortListener = new InternalEdgePortListener();
        this.intentService = intentService;
        this.edgePortService = edgePortService;

        this.intentService.addListener(this.intentListener);
        this.edgePortService.addListener(this.edgePortListener);

        this.intentsState = new ConcurrentHashMap<>();
        this.edgeportsState = new ConcurrentHashMap<>();

        this.lastEdgePortUpdate = 0;
        this.lastIntentUpdate = 0;

        log.info("Started");
    }

    /**
     * Remove listeners.
     */
    public void cleanup() {
        this.intentService.removeListener(intentListener);
        this.edgePortService.removeListener(edgePortListener);
    }

    /**
     * Returns the state of the Intent that has been provided as input.
     *
     * @param intentKey key of the intent;
     * @return the last state of the intent;
     */
    @Override
    public SdxL2State getIntentState(Key intentKey) {
        synchronized (intentsState) {
            IntentEvent event = this.intentsState.get(intentKey);
            long ts = System.currentTimeMillis();
            if (event == null || (ts > lastIntentUpdate && ts - lastIntentUpdate > deltaUpdate)) {
                Intent intent = this.intentService.getIntent(intentKey);
                IntentState intentState = this.intentService.getIntentState(intentKey);
                event = IntentEvent.getEvent(intentState, intent).get();
                intentsState.put(intentKey, event);
                this.lastIntentUpdate = ts;
            }
            return this.getSdxL2State(event.type());
        }
    }

    /**
     * Updates intentsState after an Intent event.
     *
     * @param event the event just happened
     */
    private void processIntentEvent(IntentEvent event) {
        synchronized (intentsState) {
            intentsState.put(event.subject().key(), event);
            this.lastIntentUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Translates the type of the IntentEvent in SdxL2State.
     *
     * @param type the type of event
     * @return the SdxL2State
     */
    private SdxL2State getSdxL2State(IntentEvent.Type type) {
        SdxL2State state;
        switch (type) {
            case INSTALLED:
                state = SdxL2State.ONLINE;
                break;
            case FAILED:
                state = SdxL2State.OFFLINE;
                break;
            case INSTALL_REQ:
            case WITHDRAW_REQ:
            case WITHDRAWN:
            case CORRUPT:
            case PURGED:
            default:
                state = SdxL2State.CHECK;
        }
        return state;
    }

    /**
     * Implementation of listener to account for changes on intents.
     */
    private class InternalIntentListener implements IntentListener {

        /**
         * Reacts to the specified event.
         *
         * @param event event to be processed
         */
        @Override
        public void event(IntentEvent event) {
            Intent intent = event.subject();
            if (intent.appId().equals(appId)) {
                if (event.type() == IntentEvent.Type.INSTALLED ||
                        event.type() == IntentEvent.Type.FAILED ||
                        event.type() == IntentEvent.Type.WITHDRAWN) {
                    log.info("Intent {} {}", event.subject().key(), event.type());
                }
                processIntentEvent(event);
            }
        }

    }

    /**
     * Returns the state of the EdgePort that has been provided as input.
     *
     * @param edgeport the connect point representing the edge port
     * @return the last state of the edge port;
     */
    @Override
    public SdxL2State getEdgePortState(ConnectPoint edgeport) {
        synchronized (edgeportsState) {
            EdgePortEvent event = this.edgeportsState.get(edgeport);
            long ts = System.currentTimeMillis();
            if (event == null ||
                    (ts > lastEdgePortUpdate && ts - lastEdgePortUpdate > deltaUpdate)) {
                event = new EdgePortEvent(EdgePortEvent.Type.EDGE_PORT_REMOVED, edgeport);
                Iterator<ConnectPoint> cps = this.edgePortService.getEdgePoints(edgeport.deviceId()).iterator();
                while (cps.hasNext()) {
                    if (edgeport.equals(cps.next())) {
                        event = new EdgePortEvent(EdgePortEvent.Type.EDGE_PORT_ADDED, edgeport);
                        break;
                    }
                }
                edgeportsState.put(edgeport, event);
                this.lastEdgePortUpdate = ts;
            }
            return this.getSdxL2State(event.type());
        }
    }

    /**
     * Updates edgeportsState after an EdgePort event.
     *
     * @param event the event just happened
     */
    private void processEdgePortEvent(EdgePortEvent event) {
        synchronized (edgeportsState) {
            edgeportsState.put(event.subject(), event);
            this.lastEdgePortUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Translates the type of EdgePortEvent in SdxL2State.
     *
     * @param type the type of event
     * @return the SdxL2State
     */
    private SdxL2State getSdxL2State(EdgePortEvent.Type type) {
        return type == EdgePortEvent.Type.EDGE_PORT_ADDED ? SdxL2State.ONLINE : SdxL2State.OFFLINE;
    }

    /**
     * Implementation of listener to account for changes on edge ports.
     */
    private class InternalEdgePortListener implements EdgePortListener {

        /**
         * Reacts to the specified event.
         *
         * @param event event to be processed
         */
        @Override
        public void event(EdgePortEvent event) {
            ConnectPoint cp = event.subject();
            log.info("ConnectPoint {}/{} {}", cp.elementId().toString(), cp.port().toString(), event.type().toString());
            processEdgePortEvent(event);

        }

    }

}
