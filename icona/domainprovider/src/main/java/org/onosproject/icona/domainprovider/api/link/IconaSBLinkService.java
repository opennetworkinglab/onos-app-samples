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
 * Interface used by the southbound components to advertise remote host events.
 */
public interface IconaSBLinkService {

    /**
     * Signals the domain provider to add a remote link.
     *
     * @param link link to be added
     */
    void addRemoteLink(IntraLinkDescription link);

    /**
     * Signals the domain provider to add an interlink between this and a remote domain.
     *
     * @param link link to be added
     */
    void addInterLink(InterLinkDescription link);

    /**
     * Signals the domain provider to change a remote link state.
     *
     * @param domainId domain identifier
     * @param id link identifier
     * @param state updated link state
     */
    void updateRemoteLinkState(DomainId domainId, LinkId id, State state);

    /**
     * Signals the domain provider to change a remote link metric.
     *
     * @param domainId domain identifier
     * @param id link identifier
     * @param metric updated metric
     */
    void updateRemoteLinkMetric(DomainId domainId, LinkId id, int metric);

    /**
     * Signals the domain provider to delete a remote link.
     *
     * @param domainId domain identifier
     * @param id link identifier
     */
    void removeRemoteLink(DomainId domainId, LinkId id);
}
