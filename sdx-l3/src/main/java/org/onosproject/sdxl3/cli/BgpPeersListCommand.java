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
import org.onosproject.sdxl3.config.BgpPeersConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lists the BGP peers configured in the system.
 */
@Command(scope = "onos", name = "bgp-peers",
        description = "Lists all BGP peers")
public class BgpPeersListCommand extends AbstractShellCommand {

    private static final String FORMAT = "ip=%s, interface=%s";
    private static final String NAME_FORMAT = "%s: " + FORMAT;
    private static final String AUTO_SELECTION = "auto";
    public static final String NO_PEERS = "No peers configured";

    private static final Comparator<BgpPeersConfig.PeerConfig> PEER_COMPARATOR =
            Comparator.comparing(p -> p.ip());
    public static final String EMPTY = "";

    @Override
    protected void execute() {
        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);

        ApplicationId routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        BgpConfig bgpConfig = configService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        ApplicationId sdxL3AppId = coreService.getAppId(SdxL3.SDX_L3_APP);
        BgpPeersConfig peersConfig = configService.
                getConfig(sdxL3AppId, BgpPeersConfig.class);

        if (bgpConfig == null && peersConfig == null) {
            print(NO_PEERS);
            return;
        }

        List<IpAddress> peeringAddresses = Lists.newArrayList();
        if (bgpConfig != null) {
            peeringAddresses = getPeeringAddresses(bgpConfig);
        }

        List<BgpPeersConfig.PeerConfig> bgpPeers =
                Lists.newArrayList();
        if (peersConfig != null) {
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
                if (p.interfaceName() != EMPTY) {
                    print(NAME_FORMAT, p.name().get(), p.ip(), p.interfaceName());
                } else {
                    print(NAME_FORMAT, p.name().get(), p.ip(), AUTO_SELECTION);
                }
            } else if (p.interfaceName() != EMPTY) {
                print(FORMAT, p.ip(), p.interfaceName());
            } else {
                print(FORMAT, p.ip(), AUTO_SELECTION);
            }
        });
    }

    private List<IpAddress> getPeeringAddresses(BgpConfig bgpConfig) {
        List<IpAddress> peeringAddresses = Lists.newArrayList();

        List<BgpConfig.BgpSpeakerConfig> bgpSpeakers =
                Lists.newArrayList(bgpConfig.bgpSpeakers());
        bgpSpeakers.forEach(
                s -> peeringAddresses.addAll(s.peers()));

        return peeringAddresses;
    }

    private List<BgpPeersConfig.PeerConfig> mergePeers(
            List<IpAddress> peeringAddresses,
            List<BgpPeersConfig.PeerConfig> bgpPeers) {
        peeringAddresses.forEach(a -> {
            boolean exists = bgpPeers.stream()
                    .filter(p -> p.ip().equals(a))
                    .findAny().isPresent();
            if (!exists) {
                bgpPeers.add(new BgpPeersConfig
                        .PeerConfig(Optional.<String>empty(), a, EMPTY));
            }
        });

        return bgpPeers;
    }
}
