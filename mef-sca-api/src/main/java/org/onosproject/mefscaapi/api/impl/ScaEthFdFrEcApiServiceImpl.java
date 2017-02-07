package org.onosproject.mefscaapi.api.impl;

import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onosproject.mefscaapi.api.ApiResponseMessage;
import org.onosproject.mefscaapi.api.SCAETHFDFrECApiService;
import org.onosproject.mefscaapi.api.model.SCAETHFDFrEC;
import org.onosproject.mefscaapi.translate.ScaApiTranslator;
import org.slf4j.Logger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.slf4j.LoggerFactory.getLogger;

public class ScaEthFdFrEcApiServiceImpl extends SCAETHFDFrECApiService {

    CarrierEthernetService ceManager = getService(CarrierEthernetService.class);

    private static final Logger log = getLogger(ScaEthFdFrEcApiServiceImpl.class);

    private static final String CHECK_LOG = " Check ONOS log for details.";
    private static final String NOT_TRANSLATED = "Could not translate EVC.";
    private static final String NOT_EXISTS = "EVC does not exist.";
    private static final String CREATED = "EVC was created successfully.";
    private static final String UPDATED = "EVC was updated successfully.";
    private static final String NOT_CREATED = "EVC could not be created.";
    private static final String REMOVED = "EVC was removed successfully.";
    private static final String NOT_REMOVED = "EVC could not be removed.";
    private static final String REFCOUNT_NOT_ZERO = " RefCount is not zero.";
    private static final String EVC_REL_URI = "SCA_ETH_FDFr_EC/";

    @Override
    public Response deleteSCAETHFDFrEC(String id, SecurityContext securityContext)
            throws NotFoundException {

        if (ceManager.getEvc(id) == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else {
            // Remove the EVC
            ceManager.removeEvc(id);
            return response(Response.Status.OK,
                            responseMsg(ApiResponseMessage.INFO, REMOVED));
        }
    }

    @Override
    public Response findSCAETHFDFrECById(String id, SecurityContext securityContext)
            throws NotFoundException {

        CarrierEthernetVirtualConnection evc = ceManager.getEvc(id);

        if (evc == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else {
            SCAETHFDFrEC scaEvc = ScaApiTranslator.fromCarrierEthernetVirtualConnection(evc);
            if (scaEvc == null) {
                return response(Response.Status.INTERNAL_SERVER_ERROR,
                                responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
            } else {
                return response(Response.Status.OK,
                                scaEvc);
            }
        }
    }

    @Override
    public Response findSCAETHFDFrECByState(String state, SecurityContext securityContext)
            throws NotFoundException {

        List<SCAETHFDFrEC> scaEvcList = new LinkedList<>();
        // Browse through all CE FCs and translate them to NRP ones
        for (CarrierEthernetVirtualConnection ceEvc : ceManager.evcMap().values()) {
            SCAETHFDFrEC scaEvc =
                    ScaApiTranslator.fromCarrierEthernetVirtualConnection(ceEvc);
            // If no state is provided, return all EVCs
            if (scaEvc != null &&
                    (state == null || state.equalsIgnoreCase(scaEvc.getState().toString()))) {
                scaEvcList.add(scaEvc);
            }
        }

        // TODO: Indicate if some of the EVCs could not be translated?
        return response(Response.Status.OK, scaEvcList);
    }

    @Override
    public Response sCAETHFDFrECIdPatch(String id, String state, SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Override
    public Response sCAETHFDFrECPost(SCAETHFDFrEC scaEthFdFrEc, SecurityContext securityContext)
            throws NotFoundException {

        log.trace("input: {}", scaEthFdFrEc);

        CarrierEthernetVirtualConnection evc =
                ScaApiTranslator.toCarrierEthernetVirtualConnection(scaEthFdFrEc);

        if (evc == null) {
            return response(Response.Status.BAD_REQUEST,
                            responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
        } else {
            boolean serviceExisted =
                    evc.id() != null && ceManager.evcMap().containsKey(evc.id());

            ceManager.installEvc(evc);

            if (evc.isActive()) {
                String message = serviceExisted ? UPDATED : CREATED;
                return response(EVC_REL_URI + evc.id(),
                                responseMsg(ApiResponseMessage.INFO, message));
            } else {
                // TODO: Find out somehow the reason for the failure
                return response(Response.Status.INTERNAL_SERVER_ERROR,
                                responseMsg(ApiResponseMessage.ERROR, NOT_CREATED + CHECK_LOG));
            }
        }
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
}

