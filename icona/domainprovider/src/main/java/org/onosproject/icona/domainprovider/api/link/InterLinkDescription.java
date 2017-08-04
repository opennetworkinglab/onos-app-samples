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

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainmgr.api.LinkId;
import org.onosproject.net.link.LinkDescription;


/**
 * Link between domains.
 */
public interface InterLinkDescription extends LinkDescription {

    /**
     * Returns the source and the destination domain ids of this interlink.
     *
     * @return pair of source and destination id
     */
    Pair<DomainId, DomainId> endDomains();

    /**
     * ID of the link.
     *
     * @return string ID
     */
    LinkId linkId();
}
