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
import static org.onosproject.pppoe.api.PppoeClientInfo.PppoeAdminState;


/**
 * Codec for encoding/decoding a PppoeClientInfo object to/from JSON.
 */
public final class PppoeClientInfoCodec extends JsonCodec<PppoeClientInfo> {

    // JSON field names
    private static final String ADMIN_STATE = "admin-state";
    private static final String END_SESSION = "end-session";
    private static final String SVLAN = "s-vlan";
    private static final String CVLAN = "c-vlan";

    @Override
    public ObjectNode encode(PppoeClientInfo info, CodecContext context) {
        checkNotNull(info, "PppoeClientInfo cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(ADMIN_STATE, info.adminState().name().toLowerCase())
                .put(END_SESSION, info.endSession());

        return result;
    }

    @Override
    public PppoeClientInfo decode(ObjectNode json, CodecContext context) {
        String state = json.path(ADMIN_STATE).asText();
        Integer sVlan = new Integer(json.path(SVLAN).asInt());
        Integer cVlan = new Integer(json.path(CVLAN).asInt());

        checkNotNull(state);
        checkNotNull(sVlan);
        checkNotNull(cVlan);
        PppoeAdminState adminState = PppoeAdminState.valueOf(state.toUpperCase());

        return new PppoeClientInfo(adminState, sVlan.shortValue(), cVlan.shortValue());
    }

}
