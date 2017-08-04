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
import org.onosproject.net.intent.Intent;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Matches intents related with Connection Points.
 */
public class SdxL2ConnectionPointMatcher {

    private static final String MATCH_FORMAT = "%s-%s";
    protected ApplicationId appId;

    /**
     * Creates a SDX-L2 CP Matcher.
     */
    public SdxL2ConnectionPointMatcher() {
    }

    /**
     * Creates a SDX-L2 CP Matcher.
     *
     * @param sdxl2id application ID
     */
    SdxL2ConnectionPointMatcher(ApplicationId sdxl2id) {
        this.appId = sdxl2id;
    }

    /**
     * Matches an intent given a SDX-L2 Connection Point.
     *
     * @param sdxl2cplhs left-hand side of a Virtual Circuit
     * @param sdxl2cprhs left-hand side of a Virtual Circuit
     * @param intent intent to match
     * @return result of the match
     */
    public boolean matches(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs, Intent intent) {
        if (!checkSimilarAppId(intent)) {
            return false;
        }
        String key = intent.key().toString();
        String[] fields = key.split(":");
        String cps = format(MATCH_FORMAT, sdxl2cplhs.name(), sdxl2cprhs.name());
        return fields.length == 2 && fields[1].contains(cps);
    }

    /**
     * Matches an intent given a SDX-L2 Connection Point.
     *
     * @param cp hand side of a Virtual Circuit
     * @param intent intent to match
     * @return result of the match
     */
    public boolean matches(SdxL2ConnectionPoint cp, Intent intent) {
        if (!checkSimilarAppId(intent)) {
            return false;
        }
        String key = intent.key().toString();
        String[] fields = key.split(":");
        if (fields.length != 2) {
            return false;
        }
        String[] cps = fields[1].split(",");

        if (cps.length != 2) {
            return false;
        }
        String[] hss = cps[0].split("-");
        return hss.length == 2 && (hss[0].equals(cp.name()) || hss[1].equals(cp.name()));
    }

    /**
     * Matches an intent given a SDX-L2 Connection Point.
     *
     * @param sdxl2 name of SDX-L2
     * @param intent intent to match
     * @return result of the match
     */
    public boolean matches(String sdxl2, Intent intent) {
        if (!checkSimilarAppId(intent)) {
            return false;
        }
        String key = intent.key().toString();
        String[] fields = key.split(":");
        return fields.length == 2 && fields[0].equals(sdxl2);
    }

    /**
     * Determines whether the given intent corresponds to this app.
     *
     * @param intent intent to check
     * @return result indicating whether the intent belongs to this app
     */
    private boolean checkSimilarAppId(Intent intent) {
        return appId == null || Objects.equals(appId, intent.appId());
    }
}
