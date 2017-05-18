// js for UI Reference App table view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, wss;

    // constants
    var detailsReq = 'byonNetworkDetailsRequest',
        detailsResp = 'byonNetworkDetailsResponse',
        pName = 'ov-byon-networks-details-panel',

        propOrder = ['id', 'hostCount'],
        friendlyProps = ['Network', 'Host Count'];


    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }

        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function addHost(tbody, host) {
        var tr = tbody.append('tr');
        tr.append('td').text(host.mac)
            .append('td').text(host.ip)
            .append('td').text(host.loc);
    }

    function populatePanel(panel) {
        var title = panel.append('h3'),
            tbody = panel.append('table').append('tbody');

        title.text('Network ' + $scope.panelDetails.id);
        $scope.panelDetails.hosts.forEach(function (host, i) {
            addHost(tbody, host);
        });
    }

    function respDetailsCb(data) {
        $scope.panelDetails = data.details;
        $scope.$apply();
    }

    angular.module('ovByonNetworks', [])
        .controller('OvByonNetworksCtrl',
            ['$log', '$scope', 'TableBuilderService',
                'FnService', 'WebSocketService',

                function (_$log_, _$scope_, tbs, _fs_, _wss_) {
                    $log = _$log_;
                    $scope = _$scope_;
                    fs = _fs_;
                    wss = _wss_;

                    var handlers = {};
                    $scope.panelDetails = {};

                    // details response handler
                    handlers[detailsResp] = respDetailsCb;
                    wss.bindHandlers(handlers);

                    // custom selection callback
                    function selCb($event, row) {
                        if ($scope.selId) {
                            wss.sendEvent(detailsReq, {id: row.id});
                        } else {
                            $scope.hidePanel();
                        }
                        $log.debug('Got a click on:', row);
                    }

                    // TableBuilderService creating a table for us
                    tbs.buildTable({
                        scope: $scope,
                        tag: 'byonNetwork',
                        selCb: selCb
                    });

                    // cleanup
                    $scope.$on('$destroy', function () {
                        wss.unbindHandlers(handlers);
                        $log.log('OvByonNetworksCtrl has been destroyed');
                    });

                    $log.log('OvByonNetworksCtrl has been created');
                }])

        .directive('ovByonNetworksDetailsPanel',
            ['PanelService', 'KeyService',

            function (ps, ks) {
                return {
                    restrict: 'E',
                    link: function (scope, element, attrs) {
                        // insert details panel with PanelService
                        // create the panel
                        var panel = ps.createPanel(pName, {
                            width: 200,
                            margin: 20,
                            hideMargin: 0
                        });
                        panel.hide();
                        scope.hidePanel = function () {
                            panel.hide();
                        };

                        function closePanel() {
                            if (panel.isVisible()) {
                                $scope.selId = null;
                                panel.hide();
                                return true;
                            }
                            return false;
                        }

                        // create key bindings to handle panel
                        ks.keyBindings({
                            esc: [closePanel, 'Close the details panel'],
                            _helpFormat: ['esc']
                        });
                        ks.gestureNotes([
                            ['click', 'Select a row to show item details']
                        ]);

                        // update the panel's contents when the data is changed
                        scope.$watch('panelDetails', function () {
                            if (!fs.isEmptyObject(scope.panelDetails)) {
                                panel.empty();
                                populatePanel(panel);
                                panel.show();
                            }
                        });

                        // cleanup on destroyed scope
                        scope.$on('$destroy', function () {
                            ks.unbindKeys();
                            ps.destroyPanel(pName);
                        });
                    }
                };
            }]);
}());
