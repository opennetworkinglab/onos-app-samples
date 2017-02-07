package org.onosproject.mefnrpapi.api.impl;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;
import org.onosproject.mefnrpapi.api.ApiResponseMessage;
import org.onosproject.mefnrpapi.api.LogicalTerminationPointApiService;
import org.onosproject.mefnrpapi.api.NotFoundException;
import org.onosproject.mefnrpapi.api.model.LayerProtocol;
import org.onosproject.mefnrpapi.api.model.LogicalTerminationPoint;
import org.onosproject.mefnrpapi.api.model.LpSpec;
import org.onosproject.mefnrpapi.api.model.NRPTerminationSpec;
import org.onosproject.mefnrpapi.translate.NrpApiTranslator;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LogicalTerminationPointApiServiceImpl extends LogicalTerminationPointApiService {

    CarrierEthernetService ceManager =
            DefaultServiceDirectory.getService(CarrierEthernetService.class);

    private static final String NOT_TRANSLATED = "Could not translate LTP.";
    private static final String EXISTS = "LTP already exists.";
    private static final String NOT_EXISTS = "LTP does not exist.";
    private static final String CREATED = "LTP was created successfully.";
    private static final String REMOVED = "LTP was removed successfully.";
    private static final String NOT_REMOVED = "LTP could not be removed.";
    private static final String REFCOUNT_NOT_ZERO = " RefCount is not zero.";
    private static final String LTP_REL_URI = "LogicalTerminationPoint/";

    private static final String NRP_UNI_TERMINATIONSPEC = "NRP_UNI_TerminationSpec";
    private static final String NRP_INNI_TERMINATIONSPEC = "NRP_INNI_TerminationSpec";
    private static final String NRP_ENNI_TERMINATIONSPEC = "NRP_ENNI_TerminationSpec";

    @Override
    public Response createLogicalTerminationPoint(LogicalTerminationPoint ltp,
                                                  SecurityContext securityContext) throws NotFoundException {

        CarrierEthernetLogicalTerminationPoint ceLtp = NrpApiTranslator.toCarrierEthernetLtp(ltp);
        if (ceLtp == null) {
            return response(Response.Status.BAD_REQUEST,
                            responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
        } else if (ceManager.addGlobalLtp(ceLtp) == null) {
            return response(Response.Status.FORBIDDEN,
                            responseMsg(ApiResponseMessage.ERROR, EXISTS));
        } else {
            return response(LTP_REL_URI + ceLtp.id(),
                            responseMsg(ApiResponseMessage.INFO, CREATED));
        }
    }

    @Override
    public Response deleteLogicalTerminationPoint(String ltpId,
                                                  SecurityContext securityContext) throws NotFoundException {

        if (ceManager.ltpMap().get(ltpId) == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else if (ceManager.removeGlobalLtp(ltpId) == null) {
            return response(Response.Status.FORBIDDEN,
                            responseMsg(ApiResponseMessage.ERROR, NOT_REMOVED + REFCOUNT_NOT_ZERO));
        } else {
            return response(Response.Status.OK,
                            responseMsg(ApiResponseMessage.INFO, REMOVED));
        }
    }

    @Override
    public Response getAllLogicalTerminationPoint(String fields,
                                               SecurityContext securityContext) throws NotFoundException {
        // TODO: Indicate if some of the LTPs could not be translated?
        // TODO: Filter using provided fields
        return response(Response.Status.OK, getNrpLtpList());
    }

    @Override
    public Response getLogicalTerminationPoint(String ltpId,
                                                  String fields,
                                                  SecurityContext securityContext) throws NotFoundException {

        // When id is null, return all LTPs
        if (ltpId == null) {
            return getAllLogicalTerminationPoint(fields, securityContext);
        }

        // Add all global LTPs if they are not there already
        ceManager.getLtpsFromTopo(false, false).forEach(ltp -> ceManager.addGlobalLtp(ltp));

        // Find the LTP using the provided id
        CarrierEthernetLogicalTerminationPoint ceLtp = ceManager.ltpMap().get(ltpId);

        if (ceLtp == null) {
            return response(Response.Status.NOT_FOUND,
                            responseMsg(ApiResponseMessage.ERROR, NOT_EXISTS));
        } else {
            // TODO: Match on provided fields
            LogicalTerminationPoint nrpLtp = NrpApiTranslator.fromCarrierEthernetLtp(ceLtp);
            if (nrpLtp == null) {
                return response(Response.Status.INTERNAL_SERVER_ERROR,
                                responseMsg(ApiResponseMessage.ERROR, NOT_TRANSLATED));
            } else {
                return response(Response.Status.OK, nrpLtp);
            }
        }
    }

    @Override
    public Response patchLogicalTerminationPoint(String uniID,
                                                 LogicalTerminationPoint individual,
                                                 SecurityContext securityContext) throws NotFoundException {
        // FIXME implement
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Override
    public Response getAllLogicalTerminationPointOnNode(String id,
                                                        SecurityContext securityContext)
            throws NotFoundException {
        // TODO: Indicate if some of the LTPs could not be translated?
        // TODO: Filter using provided fields
        return response(Response.Status.OK,
                        getNrpLtpList()
                                .stream()
                                .filter(nrpLtp -> nrpLtp.getPhysicalPortReference().getHostname().equals(id))
                                .collect(Collectors.toList()));
    }

    @Override
    public Response getAllLtpsOnNodeOfLpspecType(String type,
                                                 SecurityContext securityContext)
            throws NotFoundException {
        // TODO: Indicate if some of the LTPs could not be translated?
        // TODO: Filter using provided fields
        return response(Response.Status.OK,
                        getNrpLtpList()
                                .stream()
                                .filter(nrpLtp -> hasType(nrpLtp, type))
                                .collect(Collectors.toList()));
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

    /**
     * Browses through all global CE LTPs and translates them to NRP ones.
     */
    private List<LogicalTerminationPoint> getNrpLtpList() {
        List<LogicalTerminationPoint> nrpLtpList = new LinkedList<>();
        // Browse through all global CE LTPs and translate them to NRP ones
        for (CarrierEthernetLogicalTerminationPoint ceLtp : ceManager.ltpMap().values()) {
            LogicalTerminationPoint nrpLtp = NrpApiTranslator.fromCarrierEthernetLtp(ceLtp);
            if (nrpLtp != null) {
                nrpLtpList.add(nrpLtp);
            }
        }
        return nrpLtpList;
    }

    /**
     * Indicates whether an NRP LRP includes the provided type.
     *
     * Potential types are:
     * NRP_UNI_TerminationSpec, NRP_INNI_TerminationSpec,
     * NRP_ENNI_TerminationSpec, NRP_EvcEndpoint_ConnAdaptSpec,
     * NRP_IvcEndpoint_ConnAdaptSpec, NRP_OvcEndpoint_ConnAdaptSpec
     */
    private boolean hasType(LogicalTerminationPoint nrpLtp, String type) {
        for (LayerProtocol lp : nrpLtp.getLpList()) {
            LpSpec lpSpec = lp.getNrpLpSpec();
            if (lpSpec != null) {
                NRPTerminationSpec nrpTerminationSpec = lpSpec.getTerminationSpec();
                if (nrpTerminationSpec != null) {
                    if (type.equals(NRP_UNI_TERMINATIONSPEC)) {
                        return nrpTerminationSpec.getNrpUniTerminationSpec() != null;
                    } else if (type.equals(NRP_INNI_TERMINATIONSPEC)) {
                        return nrpTerminationSpec.getNrpInniTerminationSpec() != null;
                    } else if (type.equals(NRP_ENNI_TERMINATIONSPEC)) {
                        return nrpTerminationSpec.getNrpEnniTerminationSpec() != null;
                    }
                }
            }
        }
        // TODO: Handle the NRP_EvcEndpoint_ConnAdaptSpec, NRP_IvcEndpoint_ConnAdaptSpec,
        // TODO: and NRP_OvcEndpoint_ConnAdaptSpec cases
        return false;
    }
}
