package org.onosproject.mefscaapi.api.factories;

import org.onosproject.mefscaapi.api.SCAETHFPPUNINApiService;
import org.onosproject.mefscaapi.api.impl.ScaEthFppUniNApiServiceImpl;

public class SCAETHFPPUNINApiServiceFactory {
    private final static SCAETHFPPUNINApiService service = new ScaEthFppUniNApiServiceImpl();

    public static SCAETHFPPUNINApiService getSCAETHFPPUNINApi() {
        return service;
    }
}
