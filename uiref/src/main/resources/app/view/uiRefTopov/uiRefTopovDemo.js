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
        var content = tds.createDiv('my-content-class'),
            items;
        content.append('p').text('Do something to these devices?');
        items = content.append('div');
        devs.forEach(function (d) {
            items.append('p').text(d);
        });
        return content;
    }

    function dCancel() {
        $log.debug('Dialog CANCEL button pressed');
    }

    function dOk() {
        $log.debug('Dialog OK button pressed');
    }

    function createListContent() {
        var content = tds.createDiv('my-list-class'),
            items;
        // TODO: figure out best way to inject selectable list
        content.append('p').text('(Selectable list to show here...)');
        return content;
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

    // this example dialog invoked from the details panel, when one or more
    //  devices have been selected
    function deviceDialog() {
        var ctx = tss.selectionContext();

        $log.debug('device dialog invoked with context:', ctx);

        // only if at least one device was selected
        if (ctx.devices.length) {
            tds.openDialog()
                .setTitle('Process Devices')
                .addContent(createDialogContent(ctx.devices))
                .addCancel(dCancel)     // 'esc' key bound to 'Cancel' button
                .addOk(dOk)             // 'enter' key bound to 'OK' button
                .bindKeys();
        }
    }

    // this example dialog invoked from the toolbar
    function listDialog() {
        $log.debug('list dialog invoked');

        tds.openDialog()
            .setTitle('A list of stuff')
            .addContent(createListContent())
            .addOk(dOk, 'Gotcha')  // custom text for "OK" button
            .bindKeys();
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

                    deviceDialog: deviceDialog,
                    listDialog: listDialog
                };
            }]);
}());
