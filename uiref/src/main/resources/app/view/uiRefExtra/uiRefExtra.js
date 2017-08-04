/*
 * Copyright 2016 Open Networking Foundation
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

// js for UI Reference App extra view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, wss, ks, flash;

    // constants -- may be used in future
    var dataReq = 'uiRefExtraDataRequest',
        dataResp = 'uiRefExtraDataResponse';


    function doOpen() {
        flash.flash('++ OPEN ++');
    }

    function doClose() {
        flash.flash('++ CLOSE ++');
    }

    function keyFallback(token, key, code, event) {
        $log.debug('Fallback keystroke:', token, key, code, event);
    }

    function addKeyBindings() {
        var map = {
            openBracket: [doOpen, 'open function'],
            closeBracket: [doClose, 'close function'],

            _helpFormat: [
                ['openBracket', 'closeBracket']
            ]
        };

        // map explicit key bindings
        ks.keyBindings(map);

        // provide a fallback key handler for any other keys
        ks.keyBindings(keyFallback);
    }

    //function getData() {
    //    wss.sendEvent(dataReq);
    //}
    //
    //function respDataCb(data) {
    //    $scope.data = data;
    //    $scope.$apply();
    //}


    angular.module('ovUiRefExtra', [])
        .controller('OvUiRefExtraCtrl',
        ['$log', '$scope', 'WebSocketService', 'KeyService', 'FlashService',

            function (_$log_, _$scope_, _wss_, _ks_, _flash_) {
                $log = _$log_;
                $scope = _$scope_;
                wss = _wss_;
                ks = _ks_;
                flash = _flash_;

                //var handlers = {};
                //$scope.data = {};

                // data response handler
                //handlers[dataResp] = respDataCb;
                //wss.bindHandlers(handlers);

                addKeyBindings();

                // Extra click handler
                //$scope.getData = getData;

                // get data the first time...
                //getData();

                // cleanup
                $scope.$on('$destroy', function () {
                    //wss.unbindHandlers(handlers);
                    ks.unbindKeys();
                    $log.log('OvUiRefExtraCtrl has been destroyed');
                });

                $log.log('OvUiRefExtraCtrl has been created');
            }]);

}());
