/*
 Sample Demo module. This contains the "business logic" for the topology
 overlay that we are implementing.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, tss, tds;

    // constants
    var displayStart = 'uiRefTopovDisplayStart',
        displayUpdate = 'uiRefTopovDisplayUpdate',
        displayStop = 'uiRefTopovDisplayStop';

    // internal state
    var currentMode = null;


    // === ---------------------------
    // === Helper functions

    function sendDisplayStart(mode) {
        wss.sendEvent(displayStart, {
            mode: mode
        });
    }

    function sendDisplayUpdate(what) {
        wss.sendEvent(displayUpdate, {
            id: what ? what.id : ''
        });
    }

    function sendDisplayStop() {
        wss.sendEvent(displayStop);
    }

    function createDialogContent(devs) {
        var content = tds.createDiv('my-content-class');
        content.append('p').text('Do something to these devices?');
        devs.forEach(function (d) {
            content.append('p').text(d);
        });
        return content;
    }

    function dCancel() {
        $log.debug('Dialog CANCEL button pressed');
    }

    function dOk() {
        $log.debug('Dialog OK button pressed');
    }

    // === ---------------------------
    // === Main API functions

    function startDisplay(mode) {
        if (currentMode === mode) {
            $log.debug('(in mode', mode, 'already)');
        } else {
            currentMode = mode;
            sendDisplayStart(mode);
            flash.flash('Starting display mode: ' + mode);
        }
    }

    function updateDisplay(m) {
        if (currentMode) {
            sendDisplayUpdate(m);
        }
    }

    function stopDisplay() {
        if (currentMode) {
            currentMode = null;
            sendDisplayStop();
            flash.flash('Canceling display mode');
            return true;
        }
        return false;
    }

    function deviceDialog() {
        var ctx = tss.selectionContext();

        $log.debug('dialog invoked with context:', ctx);

        // only if at least one device was selected
        if (ctx.devices.length) {
            tds.openDialog()
                .addContent(createDialogContent(ctx.devices))
                .addButton('Cancel', dCancel)
                .addButton('OK', dOk);
        }
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovUiRefTopov', [])
        .factory('UiRefTopovDemoService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',
            'TopoSelectService', 'TopoDialogService',

            function (_$log_, _fs_, _flash_, _wss_, _tss_, _tds_) {
                $log = _$log_;
                fs = _fs_;
                flash = _flash_;
                wss = _wss_;
                tss = _tss_;
                tds = _tds_;

                return {
                    startDisplay: startDisplay,
                    updateDisplay: updateDisplay,
                    stopDisplay: stopDisplay,

                    deviceDialog: deviceDialog
                };
            }]);
}());
