package org.onosproject.mefscaapi.api.impl;

import org.onosproject.mefscaapi.api.ApiResponseMessage;
import org.onosproject.mefscaapi.api.NotFoundException;
import org.onosproject.mefscaapi.api.SCAETHFlowDomainApiService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class ScaEthFlowDomainApiServiceImpl extends SCAETHFlowDomainApiService {

    @Override
    public Response getSCAETHFlowDomain(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Override
    public Response getSCAETHFlowDomainById(String id, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

}
