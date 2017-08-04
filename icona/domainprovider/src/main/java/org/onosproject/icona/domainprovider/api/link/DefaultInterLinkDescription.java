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
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.link.DefaultLinkDescription;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.Link.Type;

/**
 * Default implementation of {@link InterLinkDescription}.
 */
public class DefaultInterLinkDescription extends DefaultLinkDescription
        implements InterLinkDescription {

    private final Pair<DomainId, DomainId> endDomains;
    private final LinkId id;

    /**
     * Creates an interlink using the supplied information.
     *
     * @param src source connect point
     * @param dst destination connect point
     * @param type link type
     * @param endDomains pair of domains: source left, destination right
     * @param id link identifier
     * @param annotations more information
     */
    public DefaultInterLinkDescription(ConnectPoint src, ConnectPoint dst, Type type,
                                       Pair<DomainId, DomainId> endDomains, LinkId id,
                                       SparseAnnotations... annotations) {
        super(src, dst, type, true, annotations);

        this.id = id;
        this.endDomains = endDomains;
    }

    @Override
    public Pair<DomainId, DomainId> endDomains() {
        return endDomains;
    }

    @Override
    public LinkId linkId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(endDomains, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultInterLinkDescription) {
            final DefaultInterLinkDescription other = (DefaultInterLinkDescription) obj;
            return  Objects.equals(this.endDomains, other.endDomains()) &&
                    Objects.equals(this.id, other.linkId());

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("endDomains", endDomains.toString())
                .add("linkId", id)
                .toString();
    }

}
