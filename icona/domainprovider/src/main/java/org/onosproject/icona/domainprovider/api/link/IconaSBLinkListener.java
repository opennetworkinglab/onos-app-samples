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

package org.onosproject.icona.domainprovider.api.link;

import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainmgr.api.LinkId;

import static org.onosproject.net.Link.State;

/**
 *  Methods implemented by the southbound components for the (local topology) link events.
 */
public interface IconaSBLinkListener {

    /**
     * Advertises the presence of a link between two local domain devices.
     *
     * @param domainId domain interested to this addition
     * @param link link to be added
     */
    void addLink(DomainId domainId, IntraLinkDescription link);

    /**
     * Updates a link state.
     *
     * @param domainId domain interested to this update
     * @param linkId link identifier
     * @param state updated state
     */
    void updateLinkState(DomainId domainId, LinkId linkId, State state);

    /**
     * Updates a link metric.
     *
     * @param domainId domain interested to this update
     * @param linkId link identifier
     * @param metric changed metric
     */
    void updateLinkMetric(DomainId domainId, LinkId linkId, int metric);

    /**
     * Removes a link.
     *
     * @param domainId domain interested to this update
     * @param linkId link identifier
     */
    void removeLink(DomainId domainId, LinkId linkId);
}
