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
 * Codec for encoding/decoding a PppoeSessionInfo object to/from JSON.
 */
public final class PppoeSessionInfoCodec extends JsonCodec<PppoeSessionInfo> {

    private static final String IP = "ip";
    private static final String RX_PACKETS = "rx-packets";
    private static final String TX_PACKETS = "tx-packets";
    private static final String RX_BYTES = "rx-bytes";
    private static final String TX_BYTES = "tx-bytes";

    @Override
    public PppoeSessionInfo decode(ObjectNode json, CodecContext context) {
        String ip = json.path(IP).asText();
        long rxPackets = json.path(RX_PACKETS).asLong();
        long txPackets = json.path(TX_PACKETS).asLong();
        long rxBytes = json.path(RX_BYTES).asLong();
        long txBytes = json.path(TX_BYTES).asLong();

        checkNotNull(ip);

        return new PppoeSessionInfo(ip, rxPackets, txPackets, rxBytes, txBytes);
    }

}
