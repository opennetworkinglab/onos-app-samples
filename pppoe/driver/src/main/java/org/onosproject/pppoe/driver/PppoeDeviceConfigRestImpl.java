/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.pppoe.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.pppoe.api.PppoeClientInfo;
import org.onosproject.pppoe.api.PppoeClientInfoCodec;
import org.onosproject.pppoe.api.PppoeDeviceInfo;
import org.onosproject.pppoe.api.PppoeDeviceInfoCodec;
import org.onosproject.pppoe.api.PppoeServerInfo;
import org.onosproject.pppoe.api.PppoeServerInfoCodec;
import org.onosproject.pppoe.api.PppoeSessionInfo;
import org.onosproject.pppoe.api.PppoeSessionInfoCodec;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * REST implementation for handling license information retrieval and other
 * operations.
 */
public class PppoeDeviceConfigRestImpl extends AbstractHandlerBehaviour implements PppoeDeviceConfig {

    private final Logger log = getLogger(getClass());

    private static final String MEDIA_TYPE = "json";
    private static final String PATH_CONFIG = "/pppoe/config";
    private static final String PATH_DEVICE = "/pppoe/device";
    private static final String PATH_INFO = "/pppoe/info";
    private static final String PATH_SESSION = "/pppoe/session";

    private final PppoeDeviceInfoCodec deviceInfoCodec = new PppoeDeviceInfoCodec();
    private final PppoeServerInfoCodec serverInfoCodec = new PppoeServerInfoCodec();
    private final PppoeClientInfoCodec clientInfoCodec = new PppoeClientInfoCodec();
    private final PppoeSessionInfoCodec sessionInfoCodec = new PppoeSessionInfoCodec();
    private final AbstractWebResource context = new AbstractWebResource();


    @Override
    public PppoeDeviceInfo getDevice() {
        PppoeDeviceInfo deviceInfo = null;
        DriverHandler handler = handler();
        DeviceId deviceId = handler.data().deviceId();
        String path = PATH_DEVICE;
        ObjectNode json = getConfigJson(path);

        if (json != null) {
            deviceInfo = deviceInfoCodec.decode(json, context);
            deviceInfo.setDeviceId(deviceId);
        }
        return deviceInfo;
    }

    @Override
    public PppoeServerInfo getServer() {
        PppoeServerInfo serverInfo = null;
        String path = PATH_INFO;
        ObjectNode json = getConfigJson(path);

        if (json != null) {
            serverInfo = serverInfoCodec.decode(json, context);
        }
        return serverInfo;
    }

    @Override
    public PppoeClientInfo getClient() {
        PppoeClientInfo clientInfo = null;
        String path = PATH_INFO;
        ObjectNode json = getConfigJson(path);

        if (json != null) {
            clientInfo = clientInfoCodec.decode(json, context);
        }
        return clientInfo;
    }

    @Override
    public PppoeSessionInfo readSessionData() {
        PppoeSessionInfo sessionInfo = null;
        String path = PATH_SESSION;
        ObjectNode json = getConfigJson(path);

        if (json != null) {
            sessionInfo = sessionInfoCodec.decode(json, context);
        }
        return sessionInfo;
    }

    @Override
    public boolean configClient(PppoeClientInfo clientInfo) {
        String path = PATH_CONFIG;
        ObjectNode configNode = clientInfoCodec.encode(clientInfo, context);
        clientInfo.setEndSession(false);

        return putConfigJson(path, configNode);
    }

    @Override
    public boolean configServer(PppoeServerInfo serverInfo) {
        String path = PATH_CONFIG;
        ObjectNode configNode = serverInfoCodec.encode(serverInfo, context);

        return putConfigJson(path, configNode);
    }

    /**
     * Get PPPoE information from rest.
     *
     * @param path target path
     * @return PPPoE information
     */
    private ObjectNode getConfigJson(String path) {
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();
        ObjectNode json = null;

        try {
            log.info("REST GET: {}{}", deviceId, path);
            InputStream stream = controller.get(deviceId, path, MEDIA_TYPE);
            if (stream == null) {
                log.warn("REST GET Failed: {}{}", deviceId, path);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                json = (ObjectNode) mapper.readTree(stream);
            }
        } catch (IOException e) {
            log.error("REST GET IOException: {}{} ", deviceId, path, e);
        }
        return json;
    }

    /**
     * Put PPPoE configuration to rest.
     *
     * @param path target path
     * @param configNode configuration
     * @return result
     */
    private boolean putConfigJson(String path, ObjectNode configNode) {
        boolean result = false;
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        log.info("REST PUT: {}{} {}", deviceId, path, configNode.toString());

        boolean success = controller.put(deviceId, path,
            new ByteArrayInputStream(configNode.toString()
            .getBytes(StandardCharsets.UTF_8)), MEDIA_TYPE);
        if (success) {
            result = true;
        } else {
            log.warn("REST PUT Failed: {}{}", deviceId, path);
        }
        return result;
    }

}
