// UI Reference App - topology overlay - client side
//
// This is the glue that binds our business logic (in uiRefTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, demo;

    // internal state should be kept in the service module (not here)

    var smiley = "M97,20.3A9.3,9.3,0,0,0,87.7,11H24.3A9.3,9.3,0,0,0,15,20.3" +
        "V64.7A9.3,9.3,0,0,0,24.3,74H87.7A9.3,9.3,0,0,0,97,64.7V20.3Z" +
        "M54.5,19.1A24.3,24.3,0,1,1,30.2,43.3,24.3,24.3,0,0,1,54.5,19.1Z" +
        "M104.7,94.9L97.6,82.8c-0.9-1.6-3.7-2.8-6.1-2.8H18.9" +
        "c-2.5,0-5.2,1.3-6.1,2.8L5.6,94.9C4.3,97.1,5.7,99,8.9,99h92.6" +
        "C104.6,99,106.1,97.1,104.7,94.9ZM54.5,56.5" +
        "c-7.3,0-17.2-8.6-13.3-7.4,13,3.9,13.7,3.9,26.5,0" +
        "C71.7,48,61.9,56.6,54.5,56.6Z" +
        "M38,33.9C38,32,40.2,31,42.1,31h7.3" +
        "a3.2,3.2,0,0,1,3.1,1.7,13.1,13.1,0,0,1,2.1-.3,12.9,12.9,0,0,1,2.1.4" +
        "A3.3,3.3,0,0,1,59.7,31H67c1.9,0,4,1,4,2.9v3.2A4.4,4.4,0,0,1,67,41" +
        "H59.7A4,4,0,0,1,56,37.1V33.9h0a4.4,4.4,0,0,0-1.6-.2l-1.5.2H53v3.2" +
        "A4,4,0,0,1,49.4,41H42.1A4.4,4.4,0,0,1,38,37.1V33.9Z";

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in UiRefTopoOverlay
        overlayId: 'ui-ref-overlay',
        glyphId: '*smiley',
        tooltip: 'UI Reference Overlay',

        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'ui-ref-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'ui-ref-overlay-'
        glyphs: {
            smiley: {
                vb: '0 0 110 110',
                d: smiley
            },
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
            demo.overlayActive(true);
        },
        deactivate: function () {
            demo.overlayActive(false);
            $log.debug("UI Ref topology overlay DEACTIVATED");
        },

        // Detail panel button definitions
        // NOTE: the callbacks needs to be wrapped in anonymous functions
        //       to defer the dereferencing of 'demo' to after injection
        //       of the business logic service API.
        buttons: {
            simpleDialog: {
                gid: '*banner',
                tt: 'Simple dialog example',
                cb: function() { demo.simpleDialog(); }
            },
            chainDialog: {
                gid: 'chain',
                tt: 'Chained dialogs example',
                cb: function () { demo.chainedDialogs(); }
            }
        },

        // Key bindings for topology overlay buttons
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
            A: {
                cb: function () { demo.listDialog(); },
                tt: 'List Dialog',
                gid: 'crown'
            },

            // defines the order in which the buttons appear on the toolbar
            _keyOrder: [
                '0', 'V', 'F', 'A'
            ]
        },

        hooks: {
            // hook for handling escape key...
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return demo.stopDisplay();
            },

            // hooks for when the selection changes...
            empty: function () {
                // selection changed to the empty set
                selectionCallback('empty');
            },
            single: function (data) {
                // selection changed to a single node
                selectionCallback('single', data);
                // NOTE: the detail buttons to show on the dialog are included
                //       in the detail data response from the server
            },
            multi: function (selectOrder) {
                // selection changed to more than one node
                selectionCallback('multi', selectOrder);
                // NOTE: we have to manually add detail button(s) for a 
                //       multi-selection
                tov.addDetailButton('simpleDialog');
            },
            
            // hooks for mouse movement over nodes (devices/hosts)...
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

    // just an example callback to log the selection to the console.
    // usually you would do something more useful.
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
