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

package org.onosproject.sdxl2;

import com.google.common.collect.Iterables;
import org.apache.commons.lang.NotImplementedException;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;


/**
 * Manages Virtual Circuits.
 * Base class extended by different types of VCs.
 */
public class SdxL2VCManager implements SdxL2VCService {

    private static Logger log = LoggerFactory.getLogger(SdxL2VCManager.class);

    protected static final String NAME_FORMAT = "%s:%s-%s";
    protected static final String SDXL2_CPS_FORMAT = "%s~%s";
    private static final String KEY_FORMAT = "%s,%s";

    protected ApplicationId appId;
    protected SdxL2Store sdxL2Store;
    protected IntentService intentService;
    private SdxL2ConnectionPointMatcher matcher;

    private static final String ERROR_INTENTS_FORWARD = "Unable to create forward Intents";
    private static final String ERROR_INTENTS_REVERSE = "Unable to create reverse Intents";
    protected static final String ERROR_CREATE_INTENTS = "Unable to create Intents for %s-%s";


    /**
     * Creates a SDX-L2 VC Manager.
     *
     * @param sdxl2id application ID
     * @param store reference to the SDX-L2 store
     * @param intentService reference to the Intent service
     */
    public SdxL2VCManager(ApplicationId sdxl2id,
                          SdxL2Store store,
                          IntentService intentService) {

        this.appId = sdxl2id;
        this.sdxL2Store = store;
        this.intentService = intentService;
        this.matcher = new SdxL2ConnectionPointMatcher(sdxl2id);
    }

    @Override
    public void addVC(String sdxl2, SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs) {
        try {
            this.sdxL2Store.addVC(sdxl2, sdxl2cplhs, sdxl2cprhs);

            Collection<Intent> intentsFW = buildIntents(sdxl2, sdxl2cplhs, sdxl2cprhs);
            Collection<Intent> intentsRV = buildIntents(sdxl2, sdxl2cprhs, sdxl2cplhs);

            if (intentsFW == null) {
                System.err.println("\u001B[0;31mError executing command: "
                                           + ERROR_INTENTS_FORWARD + "\u001B[0;49m");
                return;
            }
            if (intentsRV == null) {
                System.err.println("\u001B[0;31mError executing command: "
                                           + ERROR_INTENTS_REVERSE + "\u001B[0;49m");
                return;
            }

            List<Intent> intents = new ArrayList<>();
            intents.addAll(intentsFW);
            intents.addAll(intentsRV);
            intents.forEach(intent -> intentService.submit(intent));
        } catch (SdxL2Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void removeVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs) {
        try {
            this.sdxL2Store.removeVC(sdxl2cplhs, sdxl2cprhs);
            Iterables.filter(intentService.getIntents(), intent ->
                    (matcher.matches(sdxl2cplhs, sdxl2cprhs, intent) ||
                            (matcher.matches(sdxl2cprhs, sdxl2cplhs, intent))))
                    .forEach(intentService::withdraw);
        } catch (SdxL2Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void removeVC(SdxL2ConnectionPoint cp) {
        try {
            this.sdxL2Store.removeVC(cp);
            Iterables.filter(intentService.getIntents(), intent -> (matcher.matches(cp, intent)))
                    .forEach(intentService::withdraw);
        } catch (SdxL2Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Creates a set of Intents for the ingress and egress SDX-L2 Connection Point.
     *
     * @param sdxl2 name of SDX-L2
     * @param ingress the ingress point with the relative traffic attributes
     * @param egress  the egress point with the relative traffic attributes
     * @return a set of intent or a null set;
     */
    public Collection<Intent> buildIntents(String sdxl2, SdxL2ConnectionPoint ingress,
                                           SdxL2ConnectionPoint egress) {
        throw new NotImplementedException("buildIntents not implemented");
    }

    @Override
    public void removeVCs(String sdxl2) {
        this.sdxL2Store.removeVCs(sdxl2);
        Iterables.filter(intentService.getIntents(), intent -> (matcher.matches(sdxl2, intent)))
                .forEach(intentService::withdraw);

    }

    @Override
    public Set<String> getVCs(Optional<String> sdxl2) {
        return this.sdxL2Store.getVCs(sdxl2);
    }

    @Override
    public String getVC(SdxL2ConnectionPoint sdxl2cplhs, SdxL2ConnectionPoint sdxl2cprhs) {
        try {
            return this.sdxL2Store.getVC(sdxl2cplhs, sdxl2cprhs);
        } catch (SdxL2Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Returns Intent key from SDX-L2 and two SDX-L2 Connection Points.
     *
     * @param sdxl2 name of SDX-L2
     * @param cpOne sdxl2 connection point one
     * @param cpTwo sdxl2 connection point two
     * @param index digit used to help identify Intent
     * @return canonical intent string key
     */
    protected Key generateIntentKey(String sdxl2, SdxL2ConnectionPoint cpOne,
                                    SdxL2ConnectionPoint cpTwo, String index) {
        String cps = format(NAME_FORMAT, sdxl2, cpOne.name(), cpTwo.name());
        String key = format(KEY_FORMAT, cps, index);
        return Key.of(key, appId);
    }

    /**
     * Returns the traffic treatment, used in the definition of the intents.
     *
     * @param setVlan VLAN to set
     * @param pushVlan VLAN to push
     * @param popVlan boolean to indicate whether a popVlan action is
     *                performed (true) or not (false)
     * @return TrafficTreatment object
     */
    protected TrafficTreatment buildTreatment(VlanId setVlan,
                                            VlanId pushVlan,
                                            boolean popVlan) {
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (setVlan != null) {
            treatmentBuilder.setVlanId(setVlan);
        }
        if (pushVlan != null) {
            treatmentBuilder.pushVlan();
            treatmentBuilder.setVlanId(pushVlan);
        }
        if (popVlan) {
            treatmentBuilder.popVlan();
        }
        return treatmentBuilder.build();
    }

    /**
     * Returns the traffic selector, used in the definition of the intents.
     *
     * @param ethertype name of the Ethernet type used (e.g.  of SDX-L2
     * @param ingresstag VLAN id used at the ingress
     * @return TrafficSelector object
     */
    protected TrafficSelector buildSelector(Short ethertype, VlanId ingresstag) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        if (ethertype != null) {
            selectorBuilder.matchEthType(ethertype);
        }
        if (ingresstag != null) {
            selectorBuilder.matchVlanId(ingresstag);
        }
        return selectorBuilder.build();
    }

    /**
     * Returns constraints depending on the encapsulation used on the VC.
     *
     * @return list of constraints to be used in the intents
     */
    protected List<Constraint> buildConstraints() {
        throw new NotImplementedException("buildConstraints not implemented");
    }
}