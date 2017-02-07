package org.onosproject.mefscaapi.api.impl;

import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.mefscaapi.api.ApiResponseMessage;
import org.onosproject.mefscaapi.api.NotFoundException;
import org.onosproject.mefscaapi.api.SCAETHFPPUNINApiService;
import org.onosproject.mefscaapi.api.model.SCAETHFPPUNIN;
import org.onosproject.mefscaapi.api.model.SCAETHFlowPoint;
import org.onosproject.mefscaapi.translate.ScaApiTranslator;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

import java.util.LinkedList;
import java.util.List;

public class ScaEthFppUniNApiServiceImpl extends SCAETHFPPUNINApiService {

    CarrierEthernetService ceManager = getService(CarrierEthernetService.class);

    private static final String NOT_TRANSLATED = "Could not translate UNI.";
    private static final String EXISTS = "UNI already exists.";
    private static final String NOT_EXISTS = "UNI does not exist.";
    private static final String CREATED = "UNI was created successfully.";
    private static final String REMOVED = "UNI was removed successfully.";
    private static final String NOT_REMOVED = "UNI could not be removed.";
    private static final String REFCOUNT_NOT_ZERO = " RefCount is not zero.";
    private static final String UNI_REL_URI = "SCA_ETH_FPP_UNI_N/";

    @Override
    public Response createSCAETHFPPUNIN(SCAETHFPPUNIN uni, SecurityContext securityContext) throws NotFoundException {

        // Create SCA Flow Point and add provided SCA UNI to it
        SCAETHFlowPoint scaFlowPoint = new SCAETHFlowPoint();
        scaFlowPoint.setScaEthFppUniN(uni);

        CarrierEthernetUni ceUni = ScaApiTranslator.toCarrierEthernetUni(scaFlowPoint, null);
        if (ceUni == null) {
            return response(Response.Status.BAD_REQUEST,
                            responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
        } else if (ceManager.addGlobalUni(ceUni) == null) {
            return response(Response.Status.FORBIDDEN,
                            responseMsg(ApiResponseMessage.ERROR, EXISTS));
        } else {
            return response(UNI_REL_URI + ceUni.id(),
                            responseMsg(ApiResponseMessage.INFO, CREATED));
        }
    }

    @Override
    public Response deleteSCAETHFPPUNIN(String uniID, SecurityContext securityContext) throws NotFoundException {

        if (ceManager.getUniMap().get(uniID) == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else if (ceManager.removeGlobalUni(uniID) == null) {
            return response(Response.Status.FORBIDDEN,
                            responseMsg(ApiResponseMessage.ERROR, NOT_REMOVED + REFCOUNT_NOT_ZERO));
        } else {
            return response(Response.Status.OK,
                            responseMsg(ApiResponseMessage.INFO, REMOVED));
        }
    }

    @Override
    public Response findSCAETHFPPUNIN(String fields, SecurityContext securityContext) throws NotFoundException {

        List<SCAETHFPPUNIN> scaUniList = new LinkedList<>();

        // Add all global UNIs if they are not there already
        ceManager.getUnisFromTopo(false, false).forEach(uni -> ceManager.addGlobalUni(uni));

        // Browse through all global CE UNIs and translate them to SCA ones
        for (CarrierEthernetUni ceUni : ceManager.getUniMap().values()) {
            // TODO: Match on provided fields
            SCAETHFlowPoint scaFlowPoint = ScaApiTranslator.fromCarrierEthernetUni(ceUni);
            if (scaFlowPoint != null) {
                scaUniList.add(scaFlowPoint.getScaEthFppUniN());
            }
        }

        // TODO: Indicate if some of the Flow Points could not be translated?
        return response(Response.Status.OK, scaUniList);
    }

    @Override
    public Response getSCAETHFPPUNIN(String uniID, String fields, SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Override
    public Response patchSCAETHFPPUNIN(String uniID, SCAETHFPPUNIN individual, SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
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

