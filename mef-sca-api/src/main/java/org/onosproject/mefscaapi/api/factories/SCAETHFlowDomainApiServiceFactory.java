package org.onosproject.mefscaapi.api.factories;

import org.onosproject.mefscaapi.api.SCAETHFlowDomainApiService;
import org.onosproject.mefscaapi.api.impl.ScaEthFlowDomainApiServiceImpl;

public class SCAETHFlowDomainApiServiceFactory {
    private final static SCAETHFlowDomainApiService service = new ScaEthFlowDomainApiServiceImpl();

    public static SCAETHFlowDomainApiService getSCAETHFlowDomainApi() {
        return service;
    }
}
