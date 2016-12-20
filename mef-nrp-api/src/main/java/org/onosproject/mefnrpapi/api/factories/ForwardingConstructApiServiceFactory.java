package org.onosproject.mefnrpapi.api.factories;

import org.onosproject.mefnrpapi.api.ForwardingConstructApiService;
import org.onosproject.mefnrpapi.api.impl.ForwardingConstructApiServiceImpl;


public class ForwardingConstructApiServiceFactory {
    private final static ForwardingConstructApiService service = new ForwardingConstructApiServiceImpl();

    public static ForwardingConstructApiService getForwardingConstructApi() {
        return service;
    }
}
