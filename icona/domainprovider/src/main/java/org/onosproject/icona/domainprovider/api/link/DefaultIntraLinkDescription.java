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
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.link.DefaultLinkDescription;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.Link.Type;

/**
 * Default implementation of {@link IntraLinkDescription}.
 */
public class DefaultIntraLinkDescription extends DefaultLinkDescription implements IntraLinkDescription {

    private final DomainId domainId;
    private final String id;

    /**
     * Creates an intra-link using the supplied information.
     *
     * @param src source connect point
     * @param dst destination connect point
     * @param type link type
     * @param domainId domain identifier
     * @param id link identifier
     * @param annotations more information
     */
    public DefaultIntraLinkDescription(ConnectPoint src,
                                       ConnectPoint dst, Type type,
                                       DomainId domainId, String id,
                                       SparseAnnotations... annotations) {
        super(src, dst, type, true, annotations);

        this.domainId = domainId;
        this.id = id;
    }

    @Override
    public DomainId domainId() {
        return domainId;
    }

    @Override
    public String linkId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainId, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultIntraLinkDescription) {
            final DefaultIntraLinkDescription other = (DefaultIntraLinkDescription) obj;
            return  Objects.equals(this.domainId, other.domainId()) &&
                    Objects.equals(this.id, other.linkId());

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("domain", domainId)
                .add("linkId", id)
                .toString();
    }

}
