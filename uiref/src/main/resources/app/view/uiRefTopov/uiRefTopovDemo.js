/*
 Sample Demo module. This contains the "business logic" for the topology
 overlay that we are implementing.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, tss, tds;

    // constants
    var pfx = 'uiRefTopov',
        displayStart = pfx + 'DisplayStart',
        displayUpdate = pfx + 'DisplayUpdate',
        displayStop = pfx + 'DisplayStop',
        portsReq = pfx + 'DevicePortsReq',
        portsResp = pfx + 'DevicePortsResp',
        portsOp = pfx + 'DevicePortFakeOp';

    // internal state
    var currentMode = null,
        ctx = {                 // chained dialog context
            device: null,
            port: -1,
            foo: false,
            bar: false
        },
        handlers = {};


    // Handle device ports response from server:
    //   This will be invoked in response to a device selected and the
    //   "chain" button pressed on the details dialog, once the response
    //   comes back from the server.
    // We are going to open a dialog and ask the user to select one
    //   of the ports for the device...
    handlers[portsResp] = function (data) {
        $log.debug('hey! Port Data from the server!', data);
        ctx.device = data.id;
        
        // invoked when the OK button is pressed on this dialog
        function dOk() {
            $log.debug('Dialog OK button pressed');
            portOptionsDialog();
        }

        tds.openDialog()
            .setTitle('Choose Port')
            .addContent(createPortChoiceContent(data.ports))
            .addCancel()
            .addOkChained(dOk)      // NOTE: we use the "chained" version here
            .bindKeys();
    };

    function createPortChoiceContent(ports) {
        var content = tds.createDiv('port-list'),
            form,
            portSelect;

        content.append('p').text('Select port of device ' + ctx.device);
        form = content.append('form');
        form.append('span').text('port number: ');

        // onchange function for selection widget
        function selectPort() {
            ctx.port = this.options[this.selectedIndex].value;
        }

        portSelect = form.append('select').on('change', selectPort);
        ports.forEach(function (p) {
            portSelect.append('option')
                .attr('value', p.id)
                .text(p.id);
        });
        
        ctx.port = -1;      // clear state from any previous invocations

        return content;
    }


    // the function that is called if OK is pressed on our ports dialog
    function portOptionsDialog() {

        // invoked when the OK button is pressed on this dialog
        function dOk() {
            $log.debug('Port Options Dialog OK button pressed');
            $log.debug('Sending event', portsOp, ctx);
            wss.sendEvent(portsOp, ctx);
        }

        tds.openDialog()
            .setTitle('Select Port Options')
            .addContent(createPortOptionsContent())
            .addCancel()
            .addOk(dOk)     // NOTE: NOT the "chained" version!
            .bindKeys();
    }

    function createPortOptionsContent() {
        var content = tds.createDiv('port-opts'),
            form;

        // helper function to add a paragraph
        function para(text) {
            content.append('p').text(text);
        }

        para('Device ' + ctx.device);
        para('Port ' + ctx.port);

        form = content.append('form');

        // helper function to add a checkbox to the form, which updates the
        //  context when the user toggles the checked state of the box.
        function cbox(name, val) {

            // onchange function for checkbox widget
            function onchange() {
                ctx[val] = this.checked;
            }

            form.append('input').attr({
                type: 'checkbox',
                name: name,
                value: val
            }).on('change', onchange);

            ctx[val] = false;   // clear state from any previous invocations

            form.append('span').text(name);
            form.append('br');
        }

        // add two checkboxes...
        cbox('Foo', 'foo');
        cbox('Bar', 'bar');

        return content;
    }

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


    function createCustomContent() {
        var content = tds.createDiv('my-div-class');
        content.append('p').text('(Some content goes here...)');
        return content;
    }

    // === ---------------------------
    // === Main API functions

    function overlayActive(active) {
        if (active) {
            wss.bindHandlers(handlers);
        } else {
            stopDisplay();
            wss.unbindHandlers(handlers);
        }
    }

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

    // this example dialog is invoked from the details panel, when one or more
    //  devices have been selected, and the "banner" button is pressed.
    function simpleDialog() {
        var sctx = tss.selectionContext();

        $log.debug('SIMPLE: device dialog invoked with context:', sctx);

        function dCancel() {
            $log.debug('Dialog CANCEL button pressed');
        }

        function dOk() {
            $log.debug('Dialog OK button pressed');
        }

        // only if at least one device was selected
        if (sctx.devices.length) {
            tds.openDialog()
                .setTitle('Process Devices')
                .addContent(createDialogContent(sctx.devices))
                .addCancel(dCancel)     // 'esc' key bound to 'Cancel' button
                .addOk(dOk)             // 'enter' key bound to 'OK' button
                .bindKeys();
        }
    }

    // this example dialog is invoked from the details panel, when a single 
    // device has been selected and the "chain" button is pressed.
    function chainedDialogs() {
        var sctx = tss.selectionContext();

        $log.debug('CHAINED: device dialog invoked with context:', sctx);

        // only if exactly one device was selected...
        if (sctx.devices.length === 1) {
            // send a request for port information about the device to server
            wss.sendEvent(portsReq, {
                id: sctx.devices[0]
            });
        }
    }

    // this example dialog invoked from the toolbar
    function listDialog() {
        $log.debug('list dialog invoked');

        function dOk() {
            $log.debug('Dialog Gotcha button pressed');
        }

        tds.openDialog()
            .setTitle('A list of stuff')
            .addContent(createCustomContent())
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
                        overlayActive: overlayActive,

                        startDisplay: startDisplay,
                        updateDisplay: updateDisplay,
                        stopDisplay: stopDisplay,

                        chainedDialogs: chainedDialogs,
                        simpleDialog: simpleDialog,
                        listDialog: listDialog
                    };
                }]);
}());
