package org.onos.vpls;

import com.google.common.collect.SetMultimap;
import javafx.util.Pair;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.sdnip.IntentSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Synchronizes intents between the in-memory intent store and the
 * IntentService.
 */
public class IntentInstaller {
    private static final Logger log = LoggerFactory.getLogger(
            IntentInstaller.class);

    private static final int PRIORITY_OFFSET = 1000;

    private static final String PREFIX_BROADCAST = "brc";
    private static final String PREFIX_UNICAST = "uni";

    private final ApplicationId appId;
    private final IntentSynchronizer intentSynchronizer;

    /**
     * Class constructor.
     *
     * @param appId              the Application ID
     * @param intentSynchronizer the intent service
     */
    public IntentInstaller(ApplicationId appId, IntentSynchronizer intentSynchronizer) {
        this.appId = appId;
        this.intentSynchronizer = intentSynchronizer;
    }

    /**
     * Formats the requests for creating and submit intents.
     * Single Points to Multi Point intents are created for all the conigured
     * Connect Points. Multi Point to Single Point intents are created for
     * Connect Points configured that have hosts attached.
     *
     * @param confHostPresentCPoint A map of Connect Points with the eventual
     *                              MAC address of the host attached, by VLAN
     */
    protected void installIntents(SetMultimap<VlanId,
            Pair<ConnectPoint,
                    MacAddress>> confHostPresentCPoint) {
        List<Intent> intents = new ArrayList<>();

        confHostPresentCPoint.keys()
                .forEach(vlanId -> {
                    List<Pair<ConnectPoint, MacAddress>> cPoints =
                            confHostPresentCPoint.get(vlanId).stream().collect(Collectors.toList());

                    if (!cPoints.isEmpty()) {
                        for (int i = 0; i < cPoints.size(); i++) {
                            ConnectPoint src = cPoints.get(i).getKey();
                            Set<ConnectPoint> dsts = new HashSet<>();
                            MacAddress mac = cPoints.get(i).getValue();
                            for (int j = 0; j < cPoints.size(); j++) {
                                ConnectPoint dst = cPoints.get(j).getKey();
                                if (!dst.equals(src)) {
                                    dsts.add(dst);
                                }
                            }
                            Collection<SinglePointToMultiPointIntent> brcIntents =
                                    buildBrcIntents(src, dsts, vlanId);
                            intents.addAll(brcIntents);
                            if (mac != null) {
                                Collection<MultiPointToSinglePointIntent> uniIntents =
                                        buildUniIntents(dsts, src, vlanId, mac);
                                intents.addAll(uniIntents);
                            }
                        }
                    }
                });
        submitIntents(intents);
    }

    /**
     * Requests to install the intents passed as argument to the Intent Service.
     *
     * @param intents intents to be submitted
     */
    private void submitIntents(Collection<Intent> intents) {
        log.debug("Submitting intents to the IntentSynchronizer");

        for (Intent intent : intents) {
            intentSynchronizer.submit(intent);
        }
    }

    /**
     * Builds a set of Single Point to Multi Point intents.
     *
     * @param src  The source Connect Point
     * @param dsts The destination Connect Points
     * @return Single Point to Multi Point intents generated.
     */
    private Collection<SinglePointToMultiPointIntent> buildBrcIntents(ConnectPoint src,
                                                                      Set<ConnectPoint> dsts,
                                                                      VlanId vlanId) {
        log.debug("Building p-2-mp intent from {}", src);

        List<SinglePointToMultiPointIntent> intents = new ArrayList<>();

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();


        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .matchVlanId(vlanId);

        TrafficSelector selector = builder.build();

        Key key = buildKey(PREFIX_BROADCAST, src, vlanId);

        intents.add(SinglePointToMultiPointIntent.builder()
                            .appId(appId)
                            .key(key)
                            .selector(selector)
                            .treatment(treatment)
                            .ingressPoint(src)
                            .egressPoints(dsts)
                            .priority(PRIORITY_OFFSET)
                            .build());
        return intents;
    }

    /**
     * Builds a set of Multi Point to Single Point intents.
     *
     * @param srcs The source Connect Points
     * @param dst  The destination Connect Point
     * @return Multi Point to Single Point intents generated.
     */
    private Collection<MultiPointToSinglePointIntent> buildUniIntents(Set<ConnectPoint> srcs,
                                                                      ConnectPoint dst,
                                                                      VlanId vlanId,
                                                                      MacAddress mac) {
        log.debug("Building mp-2-p intent to {}", dst);

        List<MultiPointToSinglePointIntent> intents = new ArrayList<>();

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();


        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthDst(mac)
                .matchVlanId(vlanId);

        TrafficSelector selector = builder.build();

        Key key = buildKey(PREFIX_UNICAST, dst, vlanId);

        intents.add(MultiPointToSinglePointIntent.builder()
                            .appId(appId)
                            .key(key)
                            .selector(selector)
                            .treatment(treatment)
                            .ingressPoints(srcs)
                            .egressPoint(dst)
                            .priority(PRIORITY_OFFSET)
                            .build());
        return intents;
    }

    /**
     * Builds an intent Key for either for a Single Point to Multi Point or
     * Multi Point to Single Point intent, based on a prefix that defines
     * the type of intent, the single connection point representing the source
     * or the destination and the vlan id representing the network.
     *
     * @param cPoint the source or destination connect point
     * @param vlanId the network vlan id
     * @param prefix prefix string
     * @return
     */
    private Key buildKey(String prefix, ConnectPoint cPoint, VlanId vlanId) {
        String keyString = new StringBuilder()
                .append(prefix)
                .append("-")
                .append(cPoint.deviceId())
                .append("-")
                .append(cPoint.port())
                .append("-")
                .append(vlanId)

                .toString();

        return Key.of(keyString, appId);
    }

}
