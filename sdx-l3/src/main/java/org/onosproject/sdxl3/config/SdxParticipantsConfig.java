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

package org.onosproject.sdxl3.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration for the Service Providers being connected to the
 * software defined internet exchange point.
 */
public class SdxParticipantsConfig extends Config<ApplicationId> {

    public static final String PEERS = "bgpPeers";
    public static final String NAME = "name";
    public static final String IP = "ip";
    public static final String CONN_POINT = "connectPoint";
    public static final String INTF_NAME = "intfName";

    /**
     * Gets the set of configured BGP peers.
     *
     * @return BGP peers
     */
    public Set<PeerConfig> bgpPeers() {
        Set<PeerConfig> peers = Sets.newHashSet();

        JsonNode peersNode = object.get(PEERS);

        if (peersNode == null) {
            return peers;
        }
        peersNode.forEach(jsonNode -> {
            Optional<String> name;
            if (jsonNode.get(NAME) == null) {
                name = Optional.empty();
            } else {
                name = Optional.of(jsonNode.get(NAME).asText());
            }

            peers.add(new PeerConfig(name,
                    IpAddress.valueOf(jsonNode.path(IP).asText()),
                    ConnectPoint.deviceConnectPoint(jsonNode.path(CONN_POINT).asText()),
                    jsonNode.path(INTF_NAME).asText()));
        });

        return peers;
    }

    /**
     * Gets the connectPoint configured for a given BGP peer.
     *
     * @param peerAddress IP address of the peer
     * @return connectPoint connection point
     */

    public ConnectPoint getPortForPeer(IpAddress peerAddress) {
        Optional<PeerConfig> match = bgpPeers()
                .stream()
                .filter(p -> p.ip().equals(peerAddress))
                .findAny();

        if (match.isPresent()) {
            return match.get().connectPoint();
        } else {
            return null;
        }
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
     * Gets the BGP peer configured for a given peer name.
     *
     * @param peerName Name of the BGP peer
     * @return Peer configuration or null
     */

    public PeerConfig getPeerForName(Optional<String> peerName) {
        Optional<PeerConfig> match = bgpPeers()
                .stream()
                .filter(p -> p.name().equals(peerName))
                .findAny();

        if (match.isPresent()) {
            return match.get();
        } else {
            return null;
        }
    }

    /**
     * Adds new BGP peer details to the configuration.
     *
     * @param peer BGP peer configuration entry
     */

    public void addPeer(PeerConfig peer) {
        ObjectNode peerNode = JsonNodeFactory.instance.objectNode();

        if (peer.name().isPresent()) {
            peerNode.put(NAME, peer.name().get());
        }
        peerNode.put(IP, peer.ip().toString());
        peerNode.put(CONN_POINT, peer.connectPoint().elementId().toString()
                + "/" + peer.connectPoint().port().toString());
        peerNode.put(INTF_NAME, peer.interfaceName());

        ArrayNode peersArray = bgpPeers().isEmpty() ?
                initPeersConfiguration() : (ArrayNode) object.get(PEERS);
        peersArray.add(peerNode);
    }

    /**
     * Gets the BGP peer configured for a given peer IP.
     *
     * @param ip Name of the BGP peer
     * @return Peer configuration or null
     */

    public PeerConfig getPeerForIp(IpAddress ip) {
        Optional<PeerConfig> match = bgpPeers()
                .stream()
                .filter(p -> p.ip().equals(ip))
                .findAny();

        if (match.isPresent()) {
            return match.get();
        } else {
            return null;
        }
    }

    /**
     * Removes BGP peer details from configuration.
     *
     * @param peerIp BGP peer IP address
     */
    public void removePeer(IpAddress peerIp) {
        ArrayNode peersArray = (ArrayNode) object.get(PEERS);

        for (int i = 0; i < peersArray.size(); i++) {
            if (peersArray.get(i).get(IP).asText().equals(peerIp.toString())) {
                peersArray.remove(i);
                return;
            }
        }
    }

    /**
     * Creates empty configuration for BGP peers details.
     *
     * @return empty array of BGP peers
     */
    private ArrayNode initPeersConfiguration() {
        return object.putArray(PEERS);
    }

    /**
     * Configuration for a BGP peer.
     */
    public static class PeerConfig {

        private Optional<String> name;
        private IpAddress ip;
        private ConnectPoint connectPoint;
        private String interfaceName;

        public PeerConfig(Optional<String> name,
                          IpAddress ip,
                          ConnectPoint connectPoint,
                          String interfaceName) {
            this.name = checkNotNull(name);
            this.ip = checkNotNull(ip);
            // It can be null only for printout entries
            this.connectPoint = connectPoint;
            this.interfaceName = checkNotNull(interfaceName);
        }

        public Optional<String> name() {
            return name;
        }

        public IpAddress ip() {
            return ip;
        }

        public ConnectPoint connectPoint() {
            return connectPoint;
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
                        Objects.equals(this.connectPoint, that.connectPoint) &&
                        Objects.equals(this.interfaceName, that.interfaceName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, ip, connectPoint, interfaceName);
        }
    }
}
