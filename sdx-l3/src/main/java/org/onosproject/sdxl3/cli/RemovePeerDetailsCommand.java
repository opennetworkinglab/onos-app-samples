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
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdxl3.SdxL3PeerService;

/**
 * Command to remove details for a peer from configuration.
 */
@Command(scope = "onos", name = "peer-remove-details",
        description = "Removes the details of an external BGP peer")
public class RemovePeerDetailsCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ip",
            description = "BGP peer IP",
            required = true, multiValued = false)
    private String ip = null;

    private static final String DETAILS_REMOVE_SUCCESS =
            "Details successfully removed from peer.";
    private static final String DETAILS_REMOVE_FAIL =
            "Details could not be added to peer: ";

    private IpAddress peerAddress = null;

    @Override
    protected void execute() {
        peerAddress = IpAddress.valueOf(ip);

        SdxL3PeerService peerService = get(SdxL3PeerService.class);
        try {
            peerService.removePeerDetails(peerAddress);
            print(DETAILS_REMOVE_SUCCESS);
        } catch (Exception e) {
            print(DETAILS_REMOVE_FAIL + e.getMessage());
        }
    }
}
