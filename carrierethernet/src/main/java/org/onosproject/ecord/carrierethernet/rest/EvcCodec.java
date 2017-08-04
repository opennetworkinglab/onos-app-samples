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
 */package org.onosproject.ecord.carrierethernet.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetBandwidthProfile;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetNetworkInterface;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.ecord.carrierethernet.app.CarrierEthernetEvcUtils.*;
import static org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of encoder for Alarm codec.
 */
public class EvcCodec extends JsonCodec<CarrierEthernetVirtualConnection> {

    private static final String EVC_ID_REQUIRED = "EVC Id Must be specified";
    private static final String EVC_TYPE_REQUIRED = "EVC Type Must be specified";
    private static final String ARRAY_REQUIRED = "UNI array was not specified";

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(CarrierEthernetVirtualConnection evc, CodecContext context) {
        ObjectNode evcRoot = context.mapper().createObjectNode();
        evcRoot.put("evcCfgId", evc.cfgId());
        evcRoot.put("evcId", evc.id());
        ArrayNode uniList = context.mapper()
                .valueToTree(evc.uniSet().stream()
                                     .map(CarrierEthernetNetworkInterface::id)
                                     .collect(Collectors.toList()));
        evcRoot.putArray("uniList").addAll(uniList);
        evcRoot.put("maxNumUni", evc.maxNumUni());
        CarrierEthernetUni uni = evc.uniSet().iterator().next();
        evcRoot.put("vlanId", uni.ceVlanId().toString());
        CarrierEthernetBandwidthProfile bwp = uni.bwp();
        if (bwp != null) {
            evcRoot.put("cir", bwp.cir().bps());
            evcRoot.put("eir", bwp.eir().bps());
            evcRoot.put("cbs", bwp.cbs());
            evcRoot.put("ebs", bwp.ebs());
        }
        return evcRoot;
    }

    @Override
    public CarrierEthernetVirtualConnection decode(ObjectNode json, CodecContext context) {
        String argEvcCfgId = json.get("evcCfgId").asText(null);
        String argEvcId = null;
        if (json.has("evcId")) {
            argEvcId = json.get("evcId").asText();
        }
        ArrayNode uniArray = nullIsIllegal((ArrayNode) json.get("uniList"),
                                           ARRAY_REQUIRED);
        List<String> uniList = new ArrayList<>();
        uniArray.forEach(jsonNode -> uniList.add(jsonNode.asText()));
        String evcTypeString = nullIsIllegal(json.get("evcType").asText(),
                                             EVC_TYPE_REQUIRED);
        int maxNumUni = -1;
        if (json.has("maxNumUni")) {
            maxNumUni = json.get("maxNumUni").asInt(-1);
        }

        short vlanId = -1;
        if (json.has("vlanId")) {
            vlanId = json.get("vlanId").shortValue();
        }
        double cir = json.get("cir").asDouble(0.0);
        double eir = json.get("eir").asDouble(0.0);
        long cbs = json.get("cbs").asLong(0L);
        long ebs = json.get("ebs").asLong(0L);
        log.info("Received REST call with parameters: " + "evcCfgId={}, evcId={}," +
                         " uniList={}, evcType={}, maxNumUni={}, vlanId={}, cir={}, " +
                         "eir={}, cbs={}, ebs={}", argEvcCfgId, argEvcId, uniList,
                 evcTypeString, maxNumUni, vlanId, cir, eir, cbs, ebs);
        return builder().id(argEvcId).cfgId(argEvcCfgId)
                .type(generateEvcType(evcTypeString, uniList))
                .maxNumUni(generateMaxNumUni(maxNumUni, evcTypeString, uniList))
                .uniSet(generateUniSet(evcTypeString, uniList.subList(1, uniList.size()),
                                       vlanId, uniList.get(0), argEvcCfgId, cir, eir,
                                       cbs, ebs))
                .build();
    }

}
