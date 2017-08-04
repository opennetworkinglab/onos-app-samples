/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.sdxl3.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.sdxl3.SdxL3PeerService;

/**
 * Command to set details for an existing BGP peer.
 */
@Command(scope = "onos", name = "peer-add-details",
        description = "Adds the details of an external BGP peer")
public class AddPeerDetailsCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ip", description = "BGP peer IP",
            required = true, multiValued = false)
    private String ip = null;

    @Argument(index = 1, name = "port",
            description = "Connect point of the BGP peer",
            required = true, multiValued = false)
    private String connectPoint = null;

    @Argument(index = 2, name = "intfName",
            description = "Name of configured interface for the BGP peer",
            required = true, multiValued = false)
    private String interfaceName = null;

    @Option(name = "-n", aliases = "--name", description = "BGP peer name",
            required = false, multiValued = false)
    private String peerName = null;

    private static final String DETAILS_ADD_SUCCESS =
            "Details successfully added to peer.";
    private static final String DETAILS_ADD_FAIL =
            "Details could not be added to peer: ";

    private IpAddress peerAddress = null;
    private ConnectPoint port = null;

    @Override
    protected void execute() {
        peerAddress = IpAddress.valueOf(ip);
        port = ConnectPoint.deviceConnectPoint(connectPoint);

        SdxL3PeerService peerService = get(SdxL3PeerService.class);
        try {
            peerService.addPeerDetails(peerName, peerAddress, port, interfaceName);
            print(DETAILS_ADD_SUCCESS);
        } catch (Exception e) {
            print(DETAILS_ADD_FAIL + e.getMessage());
        }
    }
}
