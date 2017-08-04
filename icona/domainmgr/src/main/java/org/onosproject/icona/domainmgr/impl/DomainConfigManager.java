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

package org.onosproject.icona.domainmgr.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.icona.domainmgr.api.DomainConfigService;
import org.onosproject.icona.domainmgr.api.DomainId;
import org.onosproject.icona.domainmgr.api.DomainService;
import org.onosproject.icona.domainmgr.api.LinkId;
import org.onosproject.icona.domainmgr.impl.config.DomainConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.basics.SubjectFactories;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onosproject.net.Link.Type;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation class of {@link org.onosproject.icona.domainmgr.api.DomainConfigService}.
 */
@Component(immediate = true)
@Service
public class DomainConfigManager implements DomainConfigService {

    private static final String APP_NAME = "org.onosproject.icona.domainmgr";
    private static DomainId localDomainId;
    private final Logger log = getLogger(getClass());
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DomainService domainService;

    private ExecutorService eventExecutor = Executors.newSingleThreadExecutor();

    private final NetworkConfigListener configListener = new InternalConfigListener();

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, DomainConfig.class, "domains") {
                @Override
                public DomainConfig createConfig() {
                    return new DomainConfig();
                }
            };

    private Set<DomainId> remoteDomainIds = Sets.newConcurrentHashSet();

    private Map<LinkId, Pair<Type, ConnectPoint>> interlinkConnectPointMap;

    @Activate
    public void activate() {
        appId = coreService.getAppId(APP_NAME);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        log.info("Stopped");
    }

    @Override
    public DomainId localDomainId() {
        return localDomainId;
    }

    @Override
    public Set<DomainId> remoteDomainIds() {
        return ImmutableSet.copyOf(remoteDomainIds);
    }

    @Override
    public Map<LinkId, Pair<Type, ConnectPoint>> interlinkConnectPointMap() {
        return ImmutableMap.copyOf(interlinkConnectPointMap);
    }



    private void readConfig() {
        log.debug("Config received");
        DomainConfig config = configRegistry.getConfig(appId, DomainConfig.class);
        localDomainId = config.getLocalId();
        domainService.registerDomainId(localDomainId);
        remoteDomainIds.addAll(config.remoteDomainIds());
        interlinkConnectPointMap = config.interlinkMap();
        remoteDomainIds.forEach(domainId -> domainService.registerDomainId(domainId));
    }

    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(DomainConfig.class)) {
                return;
            }
            log.debug("Listener called: {}", event.type());
            switch (event.type()) {
                case CONFIG_ADDED:
                    log.info("Network configuration added");
                    eventExecutor.execute(DomainConfigManager.this::readConfig);
                    break;
                case CONFIG_UPDATED:
                    log.info("Network configuration updated");
                    eventExecutor.execute(DomainConfigManager.this::readConfig);
                    break;
                default:
                    break;
            }
        }
    }

}
