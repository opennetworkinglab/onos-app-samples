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

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.sdxl3.SdxL3;
import org.onosproject.sdxl3.SdxL3PeerService;
import org.onosproject.sdxl3.config.SdxParticipantsConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lists the BGP peers configured in the system.
 */
@Command(scope = "onos", name = "bgp-peers",
        description = "Lists all BGP peers")
public class BgpPeersListCommand extends AbstractShellCommand {

    private static final String BASIC_FORMAT =  "ip=%s";
    private static final String DETAILS_FORMAT =
            BASIC_FORMAT + ", port=%s/%s, intfName=%s";
    private static final String NAME_FORMAT = "%s: " + DETAILS_FORMAT;
    public static final String NO_PEERS = "No peers configured";

    private static final Comparator<SdxParticipantsConfig.PeerConfig> PEER_COMPARATOR =
            Comparator.comparing(p -> p.ip());
    public static final String EMPTY = "";

    @Override
    protected void execute() {
        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        SdxL3PeerService peerService = get(SdxL3PeerService.class);

        ApplicationId routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        BgpConfig bgpConfig = configService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        ApplicationId sdxL3AppId = coreService.getAppId(SdxL3.SDX_L3_APP);
        SdxParticipantsConfig peersConfig = configService.
                getConfig(sdxL3AppId, SdxParticipantsConfig.class);

        if (bgpConfig == null && peersConfig == null) {
            print(NO_PEERS);
            return;
        }

        List<IpAddress> peeringAddresses = Lists.newArrayList();
        if (bgpConfig != null) {
            // Get all peering addresses from BGP configuration
            peeringAddresses = peerService.getPeerAddresses(bgpConfig);
        }

        List<SdxParticipantsConfig.PeerConfig> bgpPeers =
                Lists.newArrayList();
        if (peersConfig != null) {
            // Get all peers having details specified
            bgpPeers.addAll(peersConfig.bgpPeers());
        }

        bgpPeers = mergePeers(peeringAddresses, bgpPeers);

        if (bgpPeers.isEmpty()) {
            print(NO_PEERS);
            return;
        }

        bgpPeers.sort(PEER_COMPARATOR);
        bgpPeers.forEach(p -> {
            if (p.name().isPresent()) {
                print(NAME_FORMAT, p.name().get(), p.ip(),
                      p.connectPoint().deviceId(), p.connectPoint().port(),
                      p.interfaceName());
            } else if (p.connectPoint() != null) {
                print(DETAILS_FORMAT, p.ip(), p.connectPoint().deviceId(),
                      p.connectPoint().port(), p.interfaceName());
            } else {
                print(BASIC_FORMAT, p.ip());
            }
        });
    }

    private List<SdxParticipantsConfig.PeerConfig> mergePeers(
                List<IpAddress> peeringAddresses,
                List<SdxParticipantsConfig.PeerConfig> bgpPeers) {
        peeringAddresses.forEach(a -> {
            boolean exists = bgpPeers.stream()
                    .filter(p -> p.ip().equals(a))
                    .findAny().isPresent();
            if (!exists) {
                bgpPeers.add(new SdxParticipantsConfig
                        .PeerConfig(Optional.<String>empty(), a, null, EMPTY));
            }
        });
        return bgpPeers;
    }
}
