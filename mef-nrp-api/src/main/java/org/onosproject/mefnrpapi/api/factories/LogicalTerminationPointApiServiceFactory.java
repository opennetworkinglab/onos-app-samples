package org.onosproject.mefnrpapi.api.factories;

import org.onosproject.mefnrpapi.api.LogicalTerminationPointApiService;
import org.onosproject.mefnrpapi.api.impl.LogicalTerminationPointApiServiceImpl;


public class LogicalTerminationPointApiServiceFactory {
    private final static LogicalTerminationPointApiService service = new LogicalTerminationPointApiServiceImpl();

    public static LogicalTerminationPointApiService getLogicalTerminationPointApi() {
        return service;
    }
}
