/*
 * Copyright 2014-2015 Open Networking Foundation
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

package org.onosproject.byon.gui;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * BYON GUI component. When activated, registers a {@link UiExtension} with
 * the {@link UiExtensionService}, so that new content is injected into the
 * ONOS Web UI. This example injects one new view as well as providing a
 * topology view overlay.
 */
@Component(immediate = true)
public class NetworkUiComponent {

    private static final ClassLoader CL = NetworkUiComponent.class.getClassLoader();

    // There should be matching directory names under ~/resources/app/view/
    private static final String TABLE_VIEW_ID = "byonNetworks";
    private static final String TOPOV_VIEW_ID = "byonTopov";

    // Text to appear in the UI navigation pane
    private static final String TABLE_VIEW_TEXT = "BYON Networks";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiView(UiView.Category.OTHER, TABLE_VIEW_ID, TABLE_VIEW_TEXT),
            new UiViewHidden(TOPOV_VIEW_ID)
    );

    // Factory for message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new NetworkTableViewMessageHandler(),
                    new NetworkOverlayMessageHandler()
            );

    // Factory for topology overlays
    private final UiTopoOverlayFactory topoOverlayFactory =
            () -> ImmutableList.of(
                    new NetworkTopoOverlay()
            );

    // Build our UI extension definition
    private UiExtension extension =
            new UiExtension.Builder(CL, uiViews)
                    .messageHandlerFactory(messageHandlerFactory)
                    .topoOverlayFactory(topoOverlayFactory)
                    .build();

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }

}
