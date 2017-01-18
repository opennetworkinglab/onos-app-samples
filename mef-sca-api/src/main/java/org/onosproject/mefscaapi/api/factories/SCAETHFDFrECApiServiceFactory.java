package org.onosproject.mefscaapi.api.factories;

import org.onosproject.mefscaapi.api.SCAETHFDFrECApiService;
import org.onosproject.mefscaapi.api.impl.ScaEthFdFrEcApiServiceImpl;

public class SCAETHFDFrECApiServiceFactory {
    private final static SCAETHFDFrECApiService service = new ScaEthFdFrEcApiServiceImpl();

    public static SCAETHFDFrECApiService getSCAETHFDFrECApi() {
        return service;
    }
}
