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

/*
 Sample Demo module. This contains the "business logic" for the topology
 overlay that we are implementing.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, tss, tds;

    // constants
    var dataRequest = 'byonFetchNetworksRequest',
        dataResponse = 'byonFetchNetworksResponse';

    // internal state
    var networks,
        handlers = {};


    // === ---------------------------
    // === Helper functions

    function sendDisplayStop() {
        wss.sendEvent(displayStop);
    }

    function createListContent() {
        var content = tds.createDiv('my-content-class'),
            items;
        items = content.append('div');
        networks.forEach(function (d) {
            items.append('p').text(d.name + ' (' + d.hostCount + ')');
        });
        return content;
    }

    function dClose() {
        $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function processResponse(data) {
        networks = data.networks;
        tds.openDialog()
            .setTitle('Virtual Networks')
            .addContent(createListContent())
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    function registerHandlers() {
        handlers[dataResponse] = processResponse;
        wss.bindHandlers(handlers);
    }

    function unregisterHandlers() {
        wss.unbindHandlers(handlers);
    }

    // === ---------------------------
    // === Main API functions

    // this example dialog invoked from the toolbar
    function start() {
        $log.debug('BYON start');
        registerHandlers();
        openNetworkList();
    }

    // this example dialog invoked from the toolbar
    function stop() {
        $log.debug('BYON stop');
        unregisterHandlers();
    }

    function openNetworkList() {
        wss.sendEvent(dataRequest);
    }

    function handleEscape() {
        // Placeholder.
        // If you consume an Escape key (to cancel something), return true.
        $log.debug("BYON escape");
        return false;
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovByonTopov', [])
        .factory('ByonTopovService',
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
                    start: start,
                    stop: stop,
                    openNetworkList: openNetworkList,
                    handleEscape: handleEscape
                };
            }]);
}());
