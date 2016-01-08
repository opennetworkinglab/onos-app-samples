/*
 * Copyright 2014-2016 Open Networking Laboratory
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
import org.onosproject.incubator.net.intf.Interface;

/*
 * Service for managing peers connections
 */
public interface SdxL3PeerService {

    String CONFIG_KEY = "bgpPeers";

    /**
     * Returns the interface used as connection point to peer.
     *
     * @param peeringAddress IP address of peer
     * @return interface to the peer
     */
    Interface getInterfaceForPeer(IpAddress peeringAddress);
}
