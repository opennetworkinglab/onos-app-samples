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
package org.onosproject.ecord.co;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enterprise CORD application for Central Office side.
 *
 * Ties together a gRPC-based big switch device provider,
 * along with a to-be-developed link provider, and gRPC-based CORD API
 * to control segment routing and vRouter.
 */
@Component(immediate = true)
public class CentralOffice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ECORD_CO_APP = "org.onosproject.ecord.co";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private DeviceProvider deviceProvider = new BigSwitchDeviceProvider();

    @Activate
    protected void activate() {
        coreService.registerApplication(ECORD_CO_APP);
        log.info("Enterprise CORD for central office started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Enterprise CORD for central office stopped");
    }
}
