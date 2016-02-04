// js for UI Reference App custom view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, wss, ks;

    // constants
    var dataReq = 'uiRefCustomDataRequest',
        dataResp = 'uiRefCustomDataResponse';

    function addKeyBindings() {
        var map = {
            space: [getData, 'Fetch data from server'],

            _helpFormat: [
                ['space']
            ]
        };

        ks.keyBindings(map);
    }

    function getData() {
        wss.sendEvent(dataReq);
    }

    function respDataCb(data) {
        $scope.data = data;
        $scope.$apply();
    }


    angular.module('ovUiRefCustom', [])
        .controller('OvUiRefCustomCtrl',
        ['$log', '$scope', 'WebSocketService', 'KeyService', 'NavService',

            function (_$log_, _$scope_, _wss_, _ks_, ns) {
                $log = _$log_;
                $scope = _$scope_;
                wss = _wss_;
                ks = _ks_;

                var handlers = {};
                $scope.data = {};

                // data response handler
                handlers[dataResp] = respDataCb;
                wss.bindHandlers(handlers);

                addKeyBindings();

                // custom click handler for button
                $scope.getData = getData;

                // custom click handler for icon
                // pass straight through to nav service navTo()
                $scope.nav = ns.navTo;

                // get data the first time...
                getData();

                // cleanup
                $scope.$on('$destroy', function () {
                    wss.unbindHandlers(handlers);
                    ks.unbindKeys();
                    $log.log('OvUiRefCustomCtrl has been destroyed');
                });

                $log.log('OvUiRefCustomCtrl has been created');
            }]);

}());
