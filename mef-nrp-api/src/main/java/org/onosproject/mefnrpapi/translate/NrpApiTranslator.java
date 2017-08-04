/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.mefnrpapi.translate;

import com.google.common.annotations.Beta;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetBandwidthProfile;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetForwardingConstruct;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetInni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetNetworkInterface;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onosproject.mefnrpapi.api.model.CeConnPtAndAdaptSpec;
import org.onosproject.mefnrpapi.api.model.ConnectionPointAndAdapterSpec;
import org.onosproject.mefnrpapi.api.model.FcPort;
import org.onosproject.mefnrpapi.api.model.ForwardingConstruct;
import org.onosproject.mefnrpapi.api.model.LayerProtocol;
import org.onosproject.mefnrpapi.api.model.LogicalTerminationPoint;
import org.onosproject.mefnrpapi.api.model.LpSpec;
import org.onosproject.mefnrpapi.api.model.NRPBwpFlow;
import org.onosproject.mefnrpapi.api.model.NRPCeFcSpec;
import org.onosproject.mefnrpapi.api.model.NRPCeFcSpec.NrpConnectionTypeEnum;
import org.onosproject.mefnrpapi.api.model.NRPConnAdaptSpec;
import org.onosproject.mefnrpapi.api.model.NRPENNITerminationSpec;
import org.onosproject.mefnrpapi.api.model.NRPEdgeCeFcSpec;
import org.onosproject.mefnrpapi.api.model.NRPEndPointMap;
import org.onosproject.mefnrpapi.api.model.NRPEndPointMapFormT;
import org.onosproject.mefnrpapi.api.model.NRPEndPointMapFormU;
import org.onosproject.mefnrpapi.api.model.NRPEndPointRole;
import org.onosproject.mefnrpapi.api.model.NRPEvcEndpointConnAdaptSpec;
import org.onosproject.mefnrpapi.api.model.NRPINNITerminationSpec;
import org.onosproject.mefnrpapi.api.model.NRPIngressBwpPerCosName;
import org.onosproject.mefnrpapi.api.model.NRPIvcEndpointConnAdaptSpec;
import org.onosproject.mefnrpapi.api.model.NRPPhysicalPortReference;
import org.onosproject.mefnrpapi.api.model.NRPTerminationSpec;
import org.onosproject.mefnrpapi.api.model.NRPTransitCeFcSpec;
import org.onosproject.mefnrpapi.api.model.NRPUNITerminationSpec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Methods for translating between NRP swagger-generated Java classes and corresponding ONOS Java classes.
 */
@Beta
public final class NrpApiTranslator {

    private static DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

    private static final Logger log = getLogger(NrpApiTranslator.class);

    private NrpApiTranslator() {
    }

    public static ForwardingConstruct fromCarrierEthernetFwc(CarrierEthernetForwardingConstruct ceFwc) {
        // TODO: Populate IDs of all intermediate objects

        ForwardingConstruct nrpFwc = new ForwardingConstruct();

        nrpFwc.setId(ceFwc.id());
        nrpFwc.setAdminState(ForwardingConstruct.AdminStateEnum.valueOf(ceFwc.state().name()));

        List<FcPort> fcPortList = new ArrayList<>();

        ceFwc.ltpSet().forEach(ltp -> {
            // Create FCPort
            FcPort fcPort = new FcPort();
            fcPort.setLtpRefList(Arrays.asList(fromCarrierEthernetLtp(ltp)));
            // FIXME: Find out how to set role correctly
            //fcPort.setRole(FcPort.RoleEnum.valueOf(ltp.role().name()));
            fcPortList.add(fcPort);
        });

        nrpFwc.setFcPortList(fcPortList);

        NRPCeFcSpec nrpCeFcSpec = new NRPCeFcSpec();

        NRPEdgeCeFcSpec nrpEdgeCeFcSpec = new NRPEdgeCeFcSpec();

        // TODO: What to add here?
        nrpEdgeCeFcSpec.setNRPEvcEndpoint("");
        // TODO: Populate with meaningful values
        nrpEdgeCeFcSpec.setNrpCeVlanCosPreservation(true);
        nrpEdgeCeFcSpec.setNrpCeVlanCosPreservation(true);

        NRPTransitCeFcSpec nrpTransitCeFcSpec = new NRPTransitCeFcSpec();
        // TODO: What to add here?
        nrpTransitCeFcSpec.setNRPEvcEndpoint("");
        // TODO: Populate with meaningful values
        nrpTransitCeFcSpec.setNrpSVlanCosPreservation(true);
        nrpTransitCeFcSpec.setNrpSVlanIdPreservation(true);

        nrpCeFcSpec.setNrpEdgeCeFcSpec(nrpEdgeCeFcSpec);
        nrpCeFcSpec.setNrpTransitCeFcSpec(nrpTransitCeFcSpec);

        if (ceFwc.type().equals(CarrierEthernetForwardingConstruct.Type.POINT_TO_POINT)) {
            nrpCeFcSpec.setNrpConnectionType(NRPCeFcSpec.NrpConnectionTypeEnum.POINTTOPOINT);
        } else if (ceFwc.type().equals(CarrierEthernetForwardingConstruct.Type.MULTIPOINT_TO_MULTIPOINT)) {
            nrpCeFcSpec.setNrpConnectionType(NRPCeFcSpec.NrpConnectionTypeEnum.MULTIPOINTTOMULTIPOINT);
        } else if (ceFwc.type().equals(CarrierEthernetForwardingConstruct.Type.ROOT_MULTIPOINT)) {
            nrpCeFcSpec.setNrpConnectionType(NRPCeFcSpec.NrpConnectionTypeEnum.ROOTEDMULTIPOINT);
        } else {
            return null;
        }

        nrpFwc.setNrpCeFcSpec(nrpCeFcSpec);

        return nrpFwc;
    }

    public static CarrierEthernetForwardingConstruct toCarrierEthernetFwc(ForwardingConstruct nrpFwc) {

        String fcId = nrpFwc.getId();

        CarrierEthernetForwardingConstruct.Type fcType;

        NRPCeFcSpec nrpCeFcSpec = nrpFwc.getNrpCeFcSpec();

        if (nrpCeFcSpec.getNrpConnectionType().equals(NRPCeFcSpec.NrpConnectionTypeEnum.POINTTOPOINT)) {
            fcType = CarrierEthernetVirtualConnection.Type.POINT_TO_POINT;
        } else if (nrpCeFcSpec.getNrpConnectionType().equals(NrpConnectionTypeEnum.MULTIPOINTTOMULTIPOINT)) {
            fcType = CarrierEthernetVirtualConnection.Type.MULTIPOINT_TO_MULTIPOINT;
        } else if (nrpCeFcSpec.getNrpConnectionType().equals(NRPCeFcSpec.NrpConnectionTypeEnum.ROOTEDMULTIPOINT)) {
            fcType = CarrierEthernetVirtualConnection.Type.ROOT_MULTIPOINT;
        } else {
            return null;
        }

        Set<CarrierEthernetLogicalTerminationPoint> ceLtpSet = new HashSet<>();

        nrpFwc.getFcPortList().forEach(fcPort -> {
            LogicalTerminationPoint nrpLtp = fcPort.getLtpRefList().get(0);
            CarrierEthernetLogicalTerminationPoint ceLtp = toCarrierEthernetLtp(nrpLtp);
            ceLtpSet.add(ceLtp);
        });

        return CarrierEthernetForwardingConstruct.builder()
                .id(fcId)
                .cfgId(null)
                .type(fcType)
                .ltpSet(ceLtpSet)
                .maxLatency(null)
                .build();
    }

    public static LogicalTerminationPoint fromCarrierEthernetLtp(CarrierEthernetLogicalTerminationPoint ceLtp) {

        // TODO: Return a list of LogicalTerminationPoint? (global LTP may include multiple service-specific ones)

        LogicalTerminationPoint nrpLtp = new LogicalTerminationPoint();

        // Set LTP id
        nrpLtp.setId(ceLtp.id());

        // Populate NRPPhysicalPortReference
        NRPPhysicalPortReference nrpPortRef = new NRPPhysicalPortReference();
        nrpPortRef.setHostname(ceLtp.cp().deviceId().toString());
        nrpPortRef.setPort(ceLtp.cp().port().toString());
        nrpPortRef.setVendor(deviceService.getDevice(ceLtp.cp().deviceId()).manufacturer());
        nrpLtp.setPhysicalPortReference(nrpPortRef);

        // Create LayerProtocol and LpSpec
        LayerProtocol nrpLp = new LayerProtocol();
        List<LayerProtocol> nrpLpList = new ArrayList<>();
        // TODO: Populate the protocol name in a correct way
        nrpLp.setLayerProtocolName("ETH");
        LpSpec nrpLpSpec = new LpSpec();

        NRPTerminationSpec nrpTerminationSpec = new NRPTerminationSpec();

        ConnectionPointAndAdapterSpec connectionPointAndAdapterSpec = new ConnectionPointAndAdapterSpec();
        CeConnPtAndAdaptSpec ceConnPtAndAdaptSpec = new CeConnPtAndAdaptSpec();
        NRPConnAdaptSpec nrpConnAdaptSpec = new NRPConnAdaptSpec();

        // Depending on the type of LTP, generate a different TerminationSpec/EndpointConnAdaptSpec
        if (ceLtp.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            // TODO: What to do for the terminationSpec?
            NRPUNITerminationSpec nrpUniTerminationSpec = new NRPUNITerminationSpec();
            nrpTerminationSpec.setNrpUniTerminationSpec(nrpUniTerminationSpec);

            // Generate EndpointConnAdaptSpec
            NRPEvcEndpointConnAdaptSpec nrpEvcEndpointConnAdaptSpec = new NRPEvcEndpointConnAdaptSpec();
            CarrierEthernetUni ceUni = (CarrierEthernetUni) ceLtp.ni();
            if (ceUni.ceVlanId() != null) {
                NRPEndPointMap nrpEvcEndPointMap = new NRPEndPointMap();
                List<NRPEndPointMap> nrpEndPointMapList = new ArrayList<>();
                NRPEndPointMapFormU nrpEndPointMapFormU = new NRPEndPointMapFormU();
                List<String> nrpCeVlanIdList = new ArrayList<>();
                ceUni.ceVlanIdSet().forEach(ceVlanId -> nrpCeVlanIdList.add(ceVlanId.toString()));
                nrpEndPointMapFormU.setNrpCevlanid(nrpCeVlanIdList);
                nrpEvcEndPointMap.setNrpEndPointMapFormU(nrpEndPointMapFormU);
                nrpEndPointMapList.add(nrpEvcEndPointMap);
                nrpEvcEndpointConnAdaptSpec.setNrpEvcEndPointMap(nrpEndPointMapList);
            }
            // Set speed
            nrpEvcEndpointConnAdaptSpec.setNrpSpeed(ceLtp.ni().capacity().toString());
            // Set role
            // FIXME: Need to go through all service UNIs to get their roles?
            // FIXME: Or somehow store this info in the global UNIs?
            /*NRPEndPointRole nrpEndPointRole = new NRPEndPointRole();
            nrpEndPointRole.setRole(NRPEndPointRole.RoleEnum.valueOf(ceUni.role().name()));
            nrpEvcEndpointConnAdaptSpec.setNrpEvcEndPointRole(nrpEndPointRole);*/
            // Finalize nrpConnAdaptSpec
            nrpConnAdaptSpec.setNrpEvcEndPointConnAdaptSpect(nrpEvcEndpointConnAdaptSpec);

        } else if (ceLtp.type().equals(CarrierEthernetNetworkInterface.Type.INNI)) {
            // TODO: What to do for the terminationSpec?
            NRPINNITerminationSpec nrpInniTerminationSpec = new NRPINNITerminationSpec();
            nrpTerminationSpec.setNrpInniTerminationSpec(nrpInniTerminationSpec);

            // Generate EndpointConnAdaptSpec
            NRPIvcEndpointConnAdaptSpec nrpIvcEndpointConnAdaptSpec = new NRPIvcEndpointConnAdaptSpec();
            CarrierEthernetInni ceInni = (CarrierEthernetInni) ceLtp.ni();
            if (ceInni.sVlanId() != null) {
                NRPEndPointMap nrpIvcEndPointMap = new NRPEndPointMap();
                List<NRPEndPointMap> nrpEndPointMapList = new ArrayList<>();
                NRPEndPointMapFormT nrpEndPointMapFormT = new NRPEndPointMapFormT();
                // TODO: Do this for the TPIDs as well
                List<String> nrpStagList = new ArrayList<>();
                ceInni.sVlanIdSet().forEach(sVlanId -> nrpStagList.add(sVlanId.toString()));
                nrpEndPointMapFormT.setNrpSvlanid(nrpStagList);
                nrpIvcEndPointMap.setNrpEndPointMapFormT(nrpEndPointMapFormT);
                nrpEndPointMapList.add(nrpIvcEndPointMap);
                nrpIvcEndpointConnAdaptSpec.setNrpIvcEndPointMap(nrpEndPointMapList);
            }
            // Set speed
            nrpIvcEndpointConnAdaptSpec.setNrpSpeed(ceLtp.ni().capacity().toString());
            // Set role
            // FIXME: Need to go through all service INNIs to get their roles?
            // FIXME: Or somehow store this info in the global UNIs?
            /*NRPEndPointRole nrpEndPointRole = new NRPEndPointRole();
            nrpEndPointRole.setRole(NRPEndPointRole.RoleEnum.valueOf(ceInni.role().name()));
            nrpIvcEndpointConnAdaptSpec.setNrpIvcEndPointRole(nrpEndPointRole);*/
            // Finalize nrpConnAdaptSpec
            nrpConnAdaptSpec.setNrpIvcEndPointConnAdaptSpect(nrpIvcEndpointConnAdaptSpec);

        } else {
            // TODO: Populate this when API is ready
            NRPENNITerminationSpec nrpEnniTerminationSpec = new NRPENNITerminationSpec();
            nrpTerminationSpec.setNrpEnniTerminationSpec(nrpEnniTerminationSpec);
        }

        // Finalize terminationSpec
        nrpLpSpec.setTerminationSpec(nrpTerminationSpec);

        // Finalize ceConnPtAndAdaptSpec and connectionPointAndAdapterSpec
        ceConnPtAndAdaptSpec.setConnectionPointAndAdapterSpec(nrpConnAdaptSpec);
        connectionPointAndAdapterSpec.setConnectionPointAndAdapterSpec(ceConnPtAndAdaptSpec);

        nrpLp.setNrpLpSpec(nrpLpSpec);

        if (ceLtp.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
            // Apply BW profiles where needed (connectionPointAndAdapterSpec or terminationSpec)
            if (ceLtp.type().equals(CarrierEthernetNetworkInterface.Type.UNI)) {
                applyCeUniBwpToNrpLpSpec((CarrierEthernetUni) ceLtp.ni(), nrpLpSpec);
            }
        }

        nrpLpList.add(nrpLp);
        nrpLtp.setLpList(nrpLpList);

        return nrpLtp;
    }

    public static CarrierEthernetLogicalTerminationPoint toCarrierEthernetLtp(LogicalTerminationPoint nrpLtp) {

        // TODO: Check for null

        // Set LTP id
        String ceLtpCfgId = nrpLtp.getId();

        // Set CE LTP connect point
        DeviceId deviceId = DeviceId.deviceId(nrpLtp.getPhysicalPortReference().getHostname());
        if (deviceService.getDevice(deviceId) == null) {
            log.error("Invalid deviceId {}", deviceId);
            return null;
        }
        PortNumber portNumber = PortNumber.portNumber(nrpLtp.getPhysicalPortReference().getPort());
        if (deviceService.getPort(deviceId, portNumber) == null) {
            log.error("Invalid port {} for device {}", portNumber, deviceId);
            return null;
        }
        ConnectPoint ceLtpCp = new ConnectPoint(deviceId, portNumber);

        // Get LayerProtocol and LpSpec
        LayerProtocol nrpLp = nrpLtp.getLpList().get(0);
        LpSpec lpSpec = nrpLp.getNrpLpSpec();

        ConnectionPointAndAdapterSpec connectionPointAndAdapterSpec = lpSpec.getConnectionPointAndAdapterSpec();
        CeConnPtAndAdaptSpec ceConnPtAndAdaptSpec = connectionPointAndAdapterSpec.getConnectionPointAndAdapterSpec();
        NRPConnAdaptSpec nrpConnAdaptSpec = ceConnPtAndAdaptSpec.getConnectionPointAndAdapterSpec();

        NRPTerminationSpec nrpTerminationSpec = lpSpec.getTerminationSpec();

        // Depending on the type of LTP, get info from a different TerminationSpec/EndpointConnAdaptSpec
        if (nrpTerminationSpec.getNrpUniTerminationSpec() != null) {

            // Create a global UNI and then for each CE-VLAN ID add a new one to it
            // TODO: What to do with the BW profiles?
            CarrierEthernetUni ceGlobalUni = CarrierEthernetUni.builder()
                    .cp(ceLtpCp)
                    .cfgId(ceLtpCfgId)
                    .role(null)
                    .ceVlanId(null)
                    .bwp(null)
                    .build();

            // Get info from TerminationSpec
            // TODO: What exactly should we get?

            // Get info from EndpointConnAdaptSpec
            NRPEvcEndpointConnAdaptSpec nrpEvcEndpointConnAdaptSpec =
                    nrpConnAdaptSpec.getNrpEvcEndPointConnAdaptSpect();

            // Get CE-VLAN IDs for each service UNI
            List<NRPEndPointMap> nrpEndPointMapList = nrpEvcEndpointConnAdaptSpec.getNrpEvcEndPointMap();

            CarrierEthernetLogicalTerminationPoint.Role ceLtpRole = null;

            Iterator<NRPEndPointMap> it1 = nrpEndPointMapList.iterator();
            while (it1.hasNext()) {
                NRPEndPointMap nrpEndPointMap = it1.next();
                NRPEndPointMapFormU nrpEndPointMapFormU = nrpEndPointMap.getNrpEndPointMapFormU();
                List<String> ceVlanIdList = nrpEndPointMapFormU.getNrpCevlanid();
                Iterator<String> it2 = ceVlanIdList.iterator();
                VlanId ceVlanId = VlanId.NONE;
                if (it2.hasNext()) {
                    ceVlanId = VlanId.vlanId(it2.next());
                }

                // Get role
                // TODO: How can we get a list of roles?
                NRPEndPointRole nrpEndPointRole = nrpEvcEndpointConnAdaptSpec.getNrpEvcEndPointRole();
                CarrierEthernetUni.Role ceUniRole =
                        CarrierEthernetUni.Role.valueOf(nrpEndPointRole.getRole().name());
                ceLtpRole = CarrierEthernetLogicalTerminationPoint.Role
                        .valueOf(nrpEndPointRole.getRole().name());

                // Create service UNI
                CarrierEthernetUni ceServiceUni = CarrierEthernetUni.builder()
                        .cp(ceLtpCp)
                        .cfgId(ceLtpCfgId)
                        .role(ceUniRole)
                        .ceVlanId(ceVlanId)
                        .bwp(null)
                        .build();

                // Add service UNI to globalUni
                ceGlobalUni.addEcNi(ceServiceUni);
            }

            log.info("Created global UNI {}", ceGlobalUni);

            // Get BW profiles from wherever needed (connectionPointAndAdapterSpec or terminationSpec)
            // TODO: How can we get a list of BW profiles?
            CarrierEthernetBandwidthProfile ceBwp = getCeBwpFromNrpLogicalTerminationPoint(nrpLtp);
            if (ceBwp == null) {
                log.error("No valid BWPs found for UNI with cfgId={}", ceLtpCfgId);
                return null;
            }
            ceGlobalUni.addBandwidthProfile(ceBwp);

            // Get speed
            // FIXME: Do we really need this?
            ceGlobalUni.setCapacity(Bandwidth.bps(Double.valueOf(nrpEvcEndpointConnAdaptSpec.getNrpSpeed())));

            CarrierEthernetLogicalTerminationPoint ceLtp =
                    new CarrierEthernetLogicalTerminationPoint(nrpLtp.getId(), ceGlobalUni);
            ceLtp.setRole(ceLtpRole);
            // Create global LTP and add global UNI to it
            return ceLtp;

        } else if (nrpTerminationSpec.getNrpInniTerminationSpec() != null) {

            // Create a global INNI and then for each S-TAG add a new one to it
            CarrierEthernetInni ceGlobalInni = CarrierEthernetInni.builder()
                    .cp(ceLtpCp)
                    .cfgId(ceLtpCfgId)
                    .role(null)
                    .sVlanId(null)
                    .tpid(null)
                    .usedCapacity(null)
                    .build();

            // Get info from TerminationSpec
            // TODO: What exactly should we get?

            // Get info from EndpointConnAdaptSpec
            NRPIvcEndpointConnAdaptSpec nrpIvcEndpointConnAdaptSpec =
                    nrpConnAdaptSpec.getNrpIvcEndPointConnAdaptSpect();

            // Get CE-VLAN IDs for each service UNI
            List<NRPEndPointMap> nrpEndPointMapList = nrpIvcEndpointConnAdaptSpec.getNrpIvcEndPointMap();

            CarrierEthernetLogicalTerminationPoint.Role ceLtpRole = null;

            Iterator<NRPEndPointMap> it1 = nrpEndPointMapList.iterator();
            while (it1.hasNext()) {
                NRPEndPointMap nrpEndPointMap = it1.next();
                NRPEndPointMapFormT nrpEndPointMapFormT = nrpEndPointMap.getNrpEndPointMapFormT();
                List<String> sVlanIdList = nrpEndPointMapFormT.getNrpSvlanid();
                for (String sVlanId : sVlanIdList) {
                    // FIXME: Get the TPIDs correctly
                    String tpid = nrpEndPointMapFormT.getNrpTpid().get(0);

                    // Get role
                    // TODO: How can we get a list of roles?
                    NRPEndPointRole nrpEndPointRole = nrpIvcEndpointConnAdaptSpec.getNrpIvcEndPointRole();
                    CarrierEthernetInni.Role ceInniRole = CarrierEthernetInni.Role
                            .valueOf(nrpEndPointRole.getRole().name());
                    ceLtpRole = CarrierEthernetLogicalTerminationPoint.Role
                            .valueOf(nrpEndPointRole.getRole().name());

                    // Create service INNI
                    CarrierEthernetInni ceServiceInni = CarrierEthernetInni.builder()
                            .cp(ceLtpCp)
                            .cfgId(ceLtpCfgId)
                            .role(ceInniRole)
                            .sVlanId(VlanId.vlanId(sVlanId))
                            .tpid(tpid)
                            .usedCapacity(Bandwidth.bps((double) 0))
                            .build();

                    // Add service INNI to global INNI
                    ceGlobalInni.addEcNi(ceServiceInni);
                }
            }

            // Get speed
            // FIXME: Do we really need this?
            ceGlobalInni.setCapacity(Bandwidth.bps(Double.valueOf(nrpIvcEndpointConnAdaptSpec.getNrpSpeed())));

            CarrierEthernetLogicalTerminationPoint ceLtp =
                    new CarrierEthernetLogicalTerminationPoint(nrpLtp.getId(), ceGlobalInni);
            ceLtp.setRole(ceLtpRole);

            // Create global LTP and add global INNI to it
            return ceLtp;
        } else if (nrpTerminationSpec.getNrpEnniTerminationSpec() != null) {
            // TODO: Populate this when API is ready
            return null;
        }
        return null;
    }

    private static CarrierEthernetBandwidthProfile getCeBwpFromNrpLogicalTerminationPoint(
            LogicalTerminationPoint nrpLtp) {

        String ceBwpId = null;
        String ceBwpCfgId;
        CarrierEthernetBandwidthProfile.Type ceBwpType;

        NRPConnAdaptSpec nrpConnAdaptSpec = nrpLtp.getLpList().get(0).getNrpLpSpec()
                .getConnectionPointAndAdapterSpec()
                .getConnectionPointAndAdapterSpec()
                .getConnectionPointAndAdapterSpec();
        NRPBwpFlow ingressBwpPerEvc =
                nrpConnAdaptSpec.getNrpIngressBwpFlow();
        NRPIngressBwpPerCosName ingressBwpPerCos =
                nrpConnAdaptSpec.getNrpIngressbwppercosname();
        NRPTerminationSpec nrpTerminationSpec = nrpLtp.getLpList().get(0).getNrpLpSpec()
                .getTerminationSpec();
        NRPBwpFlow interfaceIngressBwp = nrpTerminationSpec.getNrpIngressBwProfile();

        if (ingressBwpPerEvc != null) {
            if (interfaceIngressBwp != null || ingressBwpPerCos != null) {
                log.error("Only one BW profile can be set per UNI");
                return null;
            }
            ceBwpCfgId = ingressBwpPerEvc.getBwpCfgIdentifier();
            ceBwpType = CarrierEthernetBandwidthProfile.Type.EVC;

            return CarrierEthernetBandwidthProfile.builder()
                    .id(ceBwpId)
                    .cfgId(ceBwpCfgId)
                    .type(ceBwpType)
                    .cir((ingressBwpPerEvc.getBwpCfgCir() == null) ?
                            Bandwidth.bps(0) : Bandwidth.bps(ingressBwpPerEvc.getBwpCfgCir()))
                    .eir((ingressBwpPerEvc.getBwpCfgEir() == null) ?
                            Bandwidth.bps(0) : Bandwidth.bps(ingressBwpPerEvc.getBwpCfgEir()))
                    .cbs((ingressBwpPerEvc.getBwpCfgCbs() == null) ?
                            0L : (long) ingressBwpPerEvc.getBwpCfgCbs())
                    .ebs((ingressBwpPerEvc.getBwpCfgEbs() == null) ?
                            0L : (long) ingressBwpPerEvc.getBwpCfgEbs())
                    .build();
        } else if (interfaceIngressBwp != null) {
            if (ingressBwpPerCos != null) {
                log.error("Only one BW profile can be set per UNI");
                return null;
            }
            // Use the UNI connect point id
            ceBwpId = nrpLtp.getPhysicalPortReference().getHostname() + "/" +
                    nrpLtp.getPhysicalPortReference().getPort();
            ceBwpCfgId = interfaceIngressBwp.getBwpCfgIdentifier();
            ceBwpType = CarrierEthernetBandwidthProfile.Type.INTERFACE;

            return CarrierEthernetBandwidthProfile.builder()
                    .id(ceBwpId)
                    .cfgId(ceBwpCfgId)
                    .type(ceBwpType)
                    .cir((interfaceIngressBwp.getBwpCfgCir() == null) ?
                            Bandwidth.bps(0) : Bandwidth.bps(interfaceIngressBwp.getBwpCfgCir()))
                    .eir((interfaceIngressBwp.getBwpCfgEir() == null) ?
                            Bandwidth.bps(0) : Bandwidth.bps(interfaceIngressBwp.getBwpCfgEir()))
                    .cbs((interfaceIngressBwp.getBwpCfgCbs() == null) ?
                            0L : (long) interfaceIngressBwp.getBwpCfgCbs())
                    .ebs((interfaceIngressBwp.getBwpCfgEbs() == null) ?
                            0L : (long) interfaceIngressBwp.getBwpCfgEbs())
                    .build();
        } else if (ingressBwpPerCos != null) {
            ceBwpId = ingressBwpPerCos.getNrpCosName();
            if (ceBwpId == null) {
                log.error("CoS ID is required for COS BW profile");
                return null;
            }
            ceBwpCfgId = ingressBwpPerCos.getNrpCosName();
            ceBwpType = CarrierEthernetBandwidthProfile.Type.COS;
            NRPBwpFlow ingressBwpPerCosBwp = ingressBwpPerCos.getNrpBwpflow();

            return CarrierEthernetBandwidthProfile.builder()
                    .id(ceBwpId)
                    .cfgId(ceBwpCfgId)
                    .type(ceBwpType)
                    .cir((ingressBwpPerCosBwp.getBwpCfgCir() == null) ?
                            Bandwidth.bps(0) : Bandwidth.bps(ingressBwpPerCosBwp.getBwpCfgCir()))
                    .eir((ingressBwpPerCosBwp.getBwpCfgEir() == null) ?
                            Bandwidth.bps(0) : Bandwidth.bps(ingressBwpPerCosBwp.getBwpCfgEir()))
                    .cbs((ingressBwpPerCosBwp.getBwpCfgCbs() == null) ?
                            0L : (long) ingressBwpPerCosBwp.getBwpCfgCbs())
                    .ebs((ingressBwpPerCosBwp.getBwpCfgEbs() == null) ?
                            0L : (long) ingressBwpPerCosBwp.getBwpCfgEbs())
                    .build();
        } else {
            log.error("Could not find valid BW profile for LTP {}", nrpLtp.getId());
            return null;
        }

    }

    private static void applyCeUniBwpToNrpLpSpec(CarrierEthernetUni ceUni, LpSpec nrpLpSpec) {

        CarrierEthernetBandwidthProfile ceBwp = ceUni.bwp();

        if (ceBwp != null) {
            if (ceBwp.type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                NRPConnAdaptSpec nrpConnAdaptSpec = nrpLpSpec
                        .getConnectionPointAndAdapterSpec()
                        .getConnectionPointAndAdapterSpec()
                        .getConnectionPointAndAdapterSpec();
                NRPBwpFlow nrpBwp =
                        new NRPBwpFlow();
                nrpBwp.setBwpCfgCir((int) ceBwp.cir().bps());
                nrpBwp.setBwpCfgCbs((int) ceBwp.cbs());
                nrpBwp.setBwpCfgEir((int) ceBwp.eir().bps());
                nrpBwp.setBwpCfgEbs((int) ceBwp.ebs());
                nrpConnAdaptSpec.setNrpIngressBwpFlow(nrpBwp);
            } else if (ceBwp.type().equals(CarrierEthernetBandwidthProfile.Type.COS)) {
                // TODO: Populate this at a later point
            } else if (ceBwp.type().equals(CarrierEthernetBandwidthProfile.Type.INTERFACE)) {
                NRPTerminationSpec nrpTerminationSpec = nrpLpSpec.getTerminationSpec();
                NRPBwpFlow nrpBwp = new NRPBwpFlow();
                nrpBwp.setBwpCfgCir((int) ceBwp.cir().bps());
                nrpBwp.setBwpCfgCbs((int) ceBwp.cbs());
                nrpBwp.setBwpCfgEir((int) ceBwp.eir().bps());
                nrpBwp.setBwpCfgEbs((int) ceBwp.ebs());
                nrpTerminationSpec.setNrpIngressBwProfile(nrpBwp);
            } else {
                log.error("Could not add BW profile {} from UNI {}", ceBwp.id(), ceUni.id());
            }
        }
    }

}
