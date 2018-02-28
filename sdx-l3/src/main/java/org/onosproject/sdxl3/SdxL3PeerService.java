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

package org.onosproject.sdxl3;

import org.onlab.packet.IpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.sdxl3.config.SdxParticipantsConfig;

import java.util.List;

/*
 * Service for managing peers connections
 */
public interface SdxL3PeerService {

    Class<SdxParticipantsConfig> CONFIG_CLASS = SdxParticipantsConfig.class;
    String CONFIG_KEY = "participants";

    /**
     * Returns the list of IP addresses of BGP peers.
     *
     * @param bgpConfig BGP configuration
     * @return list of IP addresses of peers
     */
    List<IpAddress> getPeerAddresses(BgpConfig bgpConfig);

    /**
     * Returns the interface used as connection point to peer.
     *
     * @param peerAddress IP address of peer
     * @return interface to the peer
     */
    Interface getInterfaceForPeer(IpAddress peerAddress);

    /**
     * Adds details for a BGP peer to the configuration.
     *
     * @param peerName Peer name
     * @param peerAddress Peer IP address
     * @param port Connection point with peer
     * @param interfaceName Name of the interface configured on port
     */
    void addPeerDetails(String peerName, IpAddress peerAddress,
                        ConnectPoint port, String  interfaceName);

    /**
     * Removes details for a BGP peer to the configuration.
     *
     * @param peerAddress Peer IP address
     */
    void removePeerDetails(IpAddress peerAddress);

}
