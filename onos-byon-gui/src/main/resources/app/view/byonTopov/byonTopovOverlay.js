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

// UI Reference App - topology overlay - client side
//
// This is the glue that binds our business logic (in uiRefTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, byon;

    // internal state should be kept in the service module (not here)

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in ByonTopoOverlay
        overlayId: 'byon-overlay',
        glyphId: 'topo',
        tooltip: 'Virtual Networks Overlay',

        activate: function () {
            $log.debug("BYON topology overlay ACTIVATED");
            byon.start();
        },
        deactivate: function () {
            $log.debug("BYON topology overlay DEACTIVATED");
            byon.stop();
        },

        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            G: {
                cb: function () { byon.openNetworkList(); },
                tt: 'Open Networks list',
                gid: 'details'
            },

            _keyOrder: [
                'G'
            ]
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return byon.handleEscape();
            }
        }
    };

    // invoke code to register with the overlay service
    angular.module('ovByonTopov')
        .run(['$log', 'TopoOverlayService', 'ByonTopovService',

            function (_$log_, _tov_, _demo_) {
                $log = _$log_;
                tov = _tov_;
                byon = _demo_;
                tov.register(overlay);
            }]);

}());
