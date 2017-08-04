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

package org.onosproject.icona.domainprovider.api;


import org.onosproject.icona.domainprovider.api.device.IconaSBDeviceListener;
import org.onosproject.icona.domainprovider.api.host.IconaSBHostListener;
import org.onosproject.icona.domainprovider.api.link.IconaSBLinkListener;

/**
 * This interface enables a single component that implements IconaSBListenerService
 * to orchestrate multiple southbound components implementing all the element-specific listeners.
 */
public interface IconaSBListener extends IconaSBDeviceListener,
        IconaSBHostListener, IconaSBLinkListener, IconaSBTopologyListener {
}
