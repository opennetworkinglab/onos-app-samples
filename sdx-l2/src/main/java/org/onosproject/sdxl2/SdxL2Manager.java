/*
 * Copyright 2016-present Open Networking Laboratory
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


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Implementation of the SdxL2Service.
 */
@Component(immediate = true)
@Service
public class SdxL2Manager implements SdxL2Service {

    private static final String SDXL2_APP = "org.onosproject.sdxl2";
    private static Logger log = LoggerFactory.getLogger(SdxL2Manager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SdxL2Store sdxL2Store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    protected ApplicationId appId;

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(SDXL2_APP);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    /**
     * Creates a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     */
    @Override
    public void createSdxL2(String sdxl2) {

        checkNotNull(sdxl2, "sdxl2 name cannot be null");
        checkState(!sdxl2.contains(","), "sdxl2 names cannot contain commas");
        checkState(!sdxl2.contains("|"), "sdxl2 names cannot contain pipe");
        checkState(!sdxl2.contains("-"), "sdxl2 names cannot contain dash");
        checkState(!sdxl2.contains(":"), "sdxl2 names cannot contain colon");

        try {
            this.sdxL2Store.putSdxL2(sdxl2);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Deletes a named SDX-L2.
     *
     * @param sdxl2 SDX-L2 name
     */
    @Override
    public void deleteSdxL2(String sdxl2) {

        checkNotNull(sdxl2, "sdxl2 name cannot be null");

        try {
            this.sdxL2Store.removeSdxL2(sdxl2);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Returns a set of SDX-L2 names.
     *
     * @return a set of SDX-L2 names
     */
    @Override
    public Set<String> getSdxL2s() {
        return this.sdxL2Store.getSdxL2s();
    }

    /**
     * Adds an SDX-L2 connection point to an SDX-L2.
     *
     * @param sdxl2   SDX-L2 name
     * @param sdxl2cp SDX-L2 connection point object
     */
    @Override
    public void addSdxL2ConnectionPoint(String sdxl2, SdxL2ConnectionPoint sdxl2cp) {

        checkNotNull(sdxl2, "sdxl2 name cannot be null");
        checkNotNull(sdxl2cp, "SdxL2ConnectionPoint cannot be null");

        try {
            this.sdxL2Store.addSdxL2ConnectionPoint(sdxl2, sdxl2cp);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

    /**
     * Returns all the SDX-L2 connection points names in a SDX-L2 or all the SDX-L2 connection points names.
     *
     * @param sdxl2 SDX-L2 name
     * @return a set of SDX-L2 connection points names
     */
    @Override
    public Set<String> getSdxL2ConnectionPoints(Optional<String> sdxl2) {

        try {
            return this.sdxL2Store.getSdxL2ConnectionPoints(sdxl2);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

        return Collections.emptySet();

    }

    /**
     * Removes an SDX-L2 connection point from an SDX-L2.
     *
     * @param sdxl2cp SDX-L2 connection point name
     */
    @Override
    public void removeSdxL2ConnectionPoint(String sdxl2cp) {

        checkNotNull(sdxl2cp, "SdxL2ConnectionPoint name cannot be null");

        try {
            this.sdxL2Store.removeSdxL2ConnectionPoint(sdxl2cp);
        } catch (SdxL2Exception e) {
            log.info(e.getMessage());
        }

    }

}
