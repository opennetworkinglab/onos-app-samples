package org.onosproject.mefnrpapi.api.impl;

import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetForwardingConstruct;
import org.onosproject.mefnrpapi.api.ApiResponseMessage;
import org.onosproject.mefnrpapi.api.ForwardingConstructApiService;
import org.onosproject.mefnrpapi.api.NotFoundException;
import org.onosproject.mefnrpapi.api.model.ForwardingConstruct;
import org.onosproject.mefnrpapi.translate.NrpApiTranslator;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

import java.util.LinkedList;
import java.util.List;

public class ForwardingConstructApiServiceImpl extends ForwardingConstructApiService {

    CarrierEthernetService ceManager = getService(CarrierEthernetService.class);

    private static final String CHECK_LOG = " Check ONOS log for details.";
    private static final String NOT_TRANSLATED = "Could not translate FC.";
    private static final String NOT_EXISTS = "FC does not exist.";
    private static final String CREATED = "FC was created successfully.";
    private static final String NOT_CREATED = "FC could not be created.";
    private static final String REMOVED = "FC was removed successfully.";
    private static final String NOT_REMOVED = "FC could not be removed.";
    private static final String REFCOUNT_NOT_ZERO = " RefCount is not zero.";
    private static final String FC_REL_URI = "ForwardingConstruct/";

    @Context
    UriInfo uri;

    @Override
    public Response createAndActivateForwardingConstruct(ForwardingConstruct forwardingConstruct,
                                                         SecurityContext securityContext)
            throws NotFoundException {

        CarrierEthernetForwardingConstruct ceFc = NrpApiTranslator.toCarrierEthernetFwc(forwardingConstruct);
        if (ceFc == null) {
            return response(Response.Status.BAD_REQUEST,
                            responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
        } else {
            ceManager.installFc(ceFc);
            if (ceFc.isActive()) {
                return response(FC_REL_URI + ceFc.id(),
                                responseMsg(ApiResponseMessage.INFO, CREATED));
            } else {
                // TODO: Find out somehow the reason for the failure
                return response(Response.Status.INTERNAL_SERVER_ERROR,
                                responseMsg(ApiResponseMessage.ERROR, NOT_CREATED + CHECK_LOG));
            }
        }
    }

    @Override
    public Response deleteForwardingConstruct(String id, SecurityContext securityContext)
            throws NotFoundException {

        if (ceManager.getFc(id) == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else if (ceManager.removeFc(id) == null) {
            String msg = ceManager.getFc(id).refCount().get() != 0 ?
                    REFCOUNT_NOT_ZERO : CHECK_LOG;
            return response(Response.Status.FORBIDDEN,
                            responseMsg(ApiResponseMessage.ERROR, NOT_REMOVED + msg));
        } else {
            return response(Response.Status.OK,
                            responseMsg(ApiResponseMessage.INFO, REMOVED));
        }
    }

    @Override
    public Response getForwardingConstructById(String id, SecurityContext securityContext)
            throws NotFoundException {

        // If id is not provided, return all FCs
        if (id == null) {
            return findForwardingConstructByState(null, null, securityContext);
        }

        CarrierEthernetForwardingConstruct fc = ceManager.getFc(id);
        if (fc == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else {
            ForwardingConstruct nrpFc = NrpApiTranslator.fromCarrierEthernetFwc(fc);
            if (nrpFc == null) {
                return response(Response.Status.INTERNAL_SERVER_ERROR,
                                responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
            } else {
                return response(Response.Status.OK, nrpFc);
            }
        }
    }

    @Override
    public Response findForwardingConstructByState(String state,
                                                   String adminState,
                                                   SecurityContext securityContext)
            throws NotFoundException {

        List<ForwardingConstruct> nrpFcList = new LinkedList<>();

            // Browse through all CE FCs and translate them to NRP ones
            for (CarrierEthernetForwardingConstruct ceFc : ceManager.fcMap().values()) {
                ForwardingConstruct nrpFc = NrpApiTranslator.fromCarrierEthernetFwc(ceFc);
                // FIXME: For some reason, calls with adminState query param trigger
                // FIXME: getForwardingConstructById instead. May be an API issue
                // If no state is provided, return all FCs
                if (nrpFc != null &&
                        (state == null && adminState == null ||
                                matchState(state, adminState, nrpFc.getAdminState().toString()))) {
                    nrpFcList.add(nrpFc);
                }
            }

        // TODO: Indicate if some of the LTPs could not be translated?
        return response(Response.Status.OK, nrpFcList);
    }

    @Override
    public Response modifyForwardingConstruct(String id,
                                              String state,
                                              SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK,
                                                           "magic!")).build();
    }

    private <T>  Response response(Response.Status status, T entity) {
        return Response
                .status(status)
                .entity(entity)
                .build();
    }

    private <T>  Response response(String uri, T entity) {
        return Response
                .created(UriBuilder.fromUri(uri).build())
                .entity(entity)
                .build();
    }

    private ApiResponseMessage responseMsg(int msgCode, String msg) {
        return new ApiResponseMessage(msgCode, msg == null ? "" : msg);
    }

    private boolean matchState(String state, String adminState, String fcState) {
        if (state != null) {
            return state.equalsIgnoreCase(fcState);
        } else if (adminState != null) {
            return adminState.equalsIgnoreCase(fcState);
        }
        return false;
    }
}
