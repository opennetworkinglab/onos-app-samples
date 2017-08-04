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

import org.onosproject.event.AbstractEvent;

/**
 * Domain event class. The event notifies the presence of a new domain or its disappearance.
 * Apps can listen for such events and then retrieve the domain topologies through {@link DomainService}
 */
public class DomainEvent extends AbstractEvent<DomainEvent.Type, DomainId> {

    /**
     * Domain event type.
     */
    public enum Type {

        /**
         * Indicates that a new domain ID has been added to the distributed store.
         */
        DOMAIN_ADDED,

        /**
         * Indicates that a domain ID has been removed from the store.
         */
        DOMAIN_REMOVED

        // TODO: other state-related event types
    }

    public DomainEvent(DomainEvent.Type type, DomainId domainId) {
        super(type, domainId);
    }
}
