/*
 * Copyright 2015 Open Networking Foundation
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

package org.onosproject.uiref;

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
import org.onosproject.ui.UiView.Category;
import org.onosproject.ui.UiViewHidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * UI Reference component. When activated, registers a {@link UiExtension} with
 * the {@link UiExtensionService}, so that new content is injected into the
 * ONOS Web UI. This example injects three new views (custom, extra and table)
 * as well as providing a topology view overlay.
 */
@Component(immediate = true)
public class UiRefComponent {

    private static final ClassLoader CL = UiRefComponent.class.getClassLoader();

    // There should be matching directory names under ~/resources/app/view/
    private static final String CUSTOM_VIEW_ID = "uiRefCustom";
    private static final String EXTRA_VIEW_ID = "uiRefExtra";
    private static final String TABLE_VIEW_ID = "uiRefTable";
    private static final String TOPOV_VIEW_ID = "uiRefTopov";

    // Text to appear in the UI navigation pane
    private static final String CUSTOM_VIEW_TEXT = "UI Ref Custom";
    private static final String EXTRA_VIEW_TEXT = "UI Ref Extra";
    private static final String TABLE_VIEW_TEXT = "UI Ref Table";

    // Custom Nav Icon (binding defined in uiRefTable.js)
    private static final String NAV_ICON = "nav_uiref";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiView(Category.OTHER, CUSTOM_VIEW_ID, CUSTOM_VIEW_TEXT, NAV_ICON),
            new UiView(Category.OTHER, TABLE_VIEW_ID, TABLE_VIEW_TEXT, NAV_ICON),
            new UiView(Category.OTHER, EXTRA_VIEW_ID, EXTRA_VIEW_TEXT, NAV_ICON),
            new UiViewHidden(TOPOV_VIEW_ID)
    );

    // NOTE: for now, the Extras view does not have event handlers.

    // Factory for message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new UiRefCustomViewMessageHandler(),
                    new UiRefTableViewMessageHandler(),
                    new UiRefTopoOverlayMessageHandler()
            );

    // Factory for topology overlays
    private final UiTopoOverlayFactory topoOverlayFactory =
            () -> ImmutableList.of(
                    new UiRefTopoOverlay()
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
