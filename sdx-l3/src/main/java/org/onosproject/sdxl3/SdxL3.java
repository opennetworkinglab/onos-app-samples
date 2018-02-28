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

package org.onosproject.sdxl3;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.component.ComponentService;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component for the SDX-L3 application.
 */
@Component(immediate = true)
public class SdxL3 {

    public static final String SDX_L3_APP = "org.onosproject.sdxl3";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentService componentService;

    private ApplicationId appId;

    private static List<String> components = new ArrayList<>();
    static {
        components.add("org.onosproject.routing.bgp.BgpSessionManager");
        components.add(org.onosproject.sdxl3.impl.SdxL3PeerManager.class.getName());
        components.add(org.onosproject.sdxl3.impl.SdxL3NeighbourHandler.class.getName());
        components.add(SdxL3Fib.class.getName());
    }

    @Activate
    protected void activate() {
        components.forEach(name -> componentService.activate(appId, name));
        appId = coreService.registerApplication(SDX_L3_APP);

        applicationService.registerDeactivateHook(appId, () -> {
            intentSynchronizer.removeIntentsByAppId(appId);
        });

        log.info("SDX-L3 started");
    }

    @Deactivate
    protected void deactivate() {
        components.forEach(name -> componentService.deactivate(appId, name));
        log.info("SDX-L3 stopped");
    }
}
