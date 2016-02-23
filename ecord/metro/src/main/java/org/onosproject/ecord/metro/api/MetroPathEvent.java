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
package org.onosproject.ecord.metro.api;

import org.onosproject.event.AbstractEvent;

/**
 * Event related to metro domain path setup.
 */
public class MetroPathEvent extends AbstractEvent<MetroPathEvent.Type, MetroConnectivityId> {
    public enum Type {
        PATH_INSTALLED,
        PATH_REMOVED
    }

    public MetroPathEvent(Type type, MetroConnectivityId subject) {
        super(type, subject);
    }

}
