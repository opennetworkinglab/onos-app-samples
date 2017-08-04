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

package org.onosproject.pppoe.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Codec for encoding/decoding a PppoeServerInfo object to/from JSON.
 */
public final class PppoeServerInfoCodec extends JsonCodec<PppoeServerInfo> {

    private static final String RADIUS_IP = "radius-ip";
    private static final String RADIUS_KEY = "radius-key";

    @Override
    public ObjectNode encode(PppoeServerInfo info, CodecContext context) {
        checkNotNull(info, "PppoeServerInfo cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(RADIUS_IP, info.radiusIp())
                .put(RADIUS_KEY, info.radiusKey());

        return result;
    }

}
