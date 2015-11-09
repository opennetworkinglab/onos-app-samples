// UI Reference App - topology overlay - client side
//
// This is the glue that binds our business logic (in uiRefTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, demo;

    // internal state should be kept in the service module (not here)

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in UiRefTopoOverlay
        overlayId: 'ui-ref-overlay',
        glyphId: '*star4',
        tooltip: 'UI Reference Overlay',

        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'ui-ref-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'ui-ref-overlay-'
        glyphs: {
            star4: {
                vb: '0 0 8 8',
                d: 'M1,4l2,-1l1,-2l1,2l2,1l-2,1l-1,2l-1,-2z'
            },
            banner: {
                vb: '0 0 6 6',
                d: 'M1,1v4l2,-2l2,2v-4z'
            }
        },

        activate: function () {
            $log.debug("UI Ref topology overlay ACTIVATED");
        },
        deactivate: function () {
            demo.stopDisplay();
            $log.debug("UI Ref topology overlay DEACTIVATED");
        },

        // detail panel button definitions
        buttons: {
            foo: {
                gid: 'chain',
                tt: 'A FOO action',
                cb: function() {
                    demo.deviceDialog();
                }
            },
            bar: {
                gid: '*banner',
                tt: 'A BAR action',
                cb: function (data) {
                    $log.debug('BAR action invoked with data:', data);
                }
            }
        },

        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            0: {
                cb: function () { demo.stopDisplay(); },
                tt: 'Cancel Display Mode',
                gid: 'xMark'
            },
            V: {
                cb: function () { demo.startDisplay('mouse'); },
                tt: 'Start Mouse Mode',
                gid: '*banner'
            },
            F: {
                cb: function () { demo.startDisplay('link'); },
                tt: 'Start Link Mode',
                gid: 'chain'
            },
            G: {
                cb: function () { demo.listDialog(); },
                tt: 'Uses the G key',
                gid: 'crown'
            },

            _keyOrder: [
                '0', 'V', 'F', 'G'
            ]
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return demo.stopDisplay();
            },

            // hooks for when the selection changes...
            empty: function () {
                selectionCallback('empty');
            },
            single: function (data) {
                selectionCallback('single', data);
            },
            multi: function (selectOrder) {
                selectionCallback('multi', selectOrder);
                tov.addDetailButton('foo');
                tov.addDetailButton('bar');
            },
            mouseover: function (m) {
                // m has id, class, and type properties
                $log.debug('mouseover:', m);
                demo.updateDisplay(m);
            },
            mouseout: function () {
                $log.debug('mouseout');
                demo.updateDisplay();
            }
        }
    };

    function selectionCallback(x, d) {
        $log.debug('Selection callback', x, d);
    }

    // invoke code to register with the overlay service
    angular.module('ovUiRefTopov')
        .run(['$log', 'TopoOverlayService', 'UiRefTopovDemoService',

            function (_$log_, _tov_, _demo_) {
                $log = _$log_;
                tov = _tov_;
                demo = _demo_;
                tov.register(overlay);
            }]);

}());
