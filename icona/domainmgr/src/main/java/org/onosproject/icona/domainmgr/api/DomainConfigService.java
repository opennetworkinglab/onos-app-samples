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

package org.onosproject.icona.domainmgr.api;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;

import java.util.Map;
import java.util.Set;

/**
 *  Domain configuration interface.
 *  Currently, the whole configuration is passed to ICONA through
 *  the network configuration subsystem. Cli commands with related
 *  setter methods will be added in the near future.
 */
public interface DomainConfigService {

    /**
     * Returns the local domain ID.
     * @return domain ID
     */
    DomainId localDomainId();

    /**
     * Returns the set of configured domain IDs.
     * @return set of domain IDs
     */
    Set<DomainId> remoteDomainIds();

    /**
     * Returns the map between an interlink ID and a description
     * of type and physical connect point where the link is attached to.
     *
     * @return a map between interlink ID and a pair of type and port of the link
     */
    Map<LinkId, Pair<Link.Type, ConnectPoint>> interlinkConnectPointMap();
}

