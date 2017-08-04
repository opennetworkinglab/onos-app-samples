/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the Device.
 * You may obtain a copy of the Device at
 *
 *     http://www.apache.org/devices/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Device is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Device for the specific language governing permissions and
 * limitations under the Device.
 */

package org.onosproject.pppoe.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.pppoe.api.PppoeDeviceInfo.PppoeDeviceType;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Codec for encoding/decoding a PppoeDeviceInfo object to/from JSON.
 */
public final class PppoeDeviceInfoCodec extends JsonCodec<PppoeDeviceInfo> {

    private final Logger log = getLogger(getClass());

    private static final String TYPE = "type";


    @Override
    public PppoeDeviceInfo decode(ObjectNode json, CodecContext context) {
        String strType = json.path(TYPE).asText();

        checkNotNull(strType);
        PppoeDeviceType type = PppoeDeviceType.valueOf(strType.toUpperCase());

        if (type.equals(PppoeDeviceType.CLIENT)) {
            return new PppoeClientInfo();
        } else if (type.equals(PppoeDeviceType.SERVER)) {
            return new PppoeServerInfo();
        } else {
            log.warn("decode(): Device Type is UNKNOWN");
            return null;
        }
    }

}
