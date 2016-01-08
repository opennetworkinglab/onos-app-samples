/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.sdxl3.config;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class BgpPeersConfig extends Config<ApplicationId> {
    public static final String NAME = "name";
    public static final String IP = "ip";
    public static final String INTERFACE = "interface";

    /**
     * Gets the set of configured BGP peers.
     *
     * @return BGP peers
     */
    public Set<PeerConfig> bgpPeers() {
        Set<PeerConfig> peers = Sets.newHashSet();

        ArrayNode peersNode = array;
        peersNode.forEach(jsonNode -> {
            Optional<String> name;
            if (jsonNode.get(NAME) == null) {
                name = Optional.empty();
            } else {
                name = Optional.of(jsonNode.get(NAME).asText());
            }

            peers.add(new PeerConfig(name,
                    IpAddress.valueOf(jsonNode.path(IP).asText()),
                    jsonNode.path(INTERFACE).asText()));
        });

        return peers;
    }

    /**
     * Gets the interface name configured for a given BGP peer.
     *
     * @param peerAddress IP address of the peer
     * @return interface name
     */

    public String getInterfaceNameForPeer(IpAddress peerAddress) {
        Optional<PeerConfig> match = bgpPeers()
                .stream()
                .filter(p -> p.ip().equals(peerAddress))
                .findAny();

        if (match.isPresent()) {
            return match.get().interfaceName();
        } else {
            return null;
        }
    }


    /**
     * Configuration for a BGP peer.
     */
    public static class PeerConfig {

        private Optional<String> name;
        private IpAddress ip;
        private String interfaceName;

        public PeerConfig(Optional<String> name, IpAddress ip, String interfaceName) {
            this.name = checkNotNull(name);
            this.ip = checkNotNull(ip);
            this.interfaceName = checkNotNull(interfaceName);
        }

        public Optional<String> name() {
            return name;
        }

        public IpAddress ip() {
            return ip;
        }

        public String interfaceName() {
            return interfaceName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PeerConfig) {
                final PeerConfig that = (PeerConfig) obj;
                return Objects.equals(this.name, that.name) &&
                        Objects.equals(this.ip, that.ip) &&
                        Objects.equals(this.interfaceName, that.interfaceName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, ip, interfaceName);
        }
    }
}
