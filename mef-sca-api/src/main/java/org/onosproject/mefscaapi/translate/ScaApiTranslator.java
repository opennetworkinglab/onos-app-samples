/*
 * Copyright 2016 Open Networking Foundation
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
package org.onosproject.mefscaapi.translate;

import org.onlab.util.Bandwidth;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetBandwidthProfile;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetConnection;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onlab.packet.VlanId;
import org.onosproject.mefscaapi.api.model.SCAETHFDFrEC;
import org.onosproject.mefscaapi.api.model.SCAETHFPPUNIN;
import org.onosproject.mefscaapi.api.model.SCAETHFPPUNINTransportPort;
import org.onosproject.mefscaapi.api.model.SCAETHFlowPoint;
import org.onosproject.mefscaapi.api.model.SCAETHFlowPointClassOfServiceIdentifierIp;
import org.onosproject.mefscaapi.api.model.SCAETHFlowPointIngressBandwidthProfilePerClassOfServiceIdentifier;
import org.onosproject.mefscaapi.api.model.SCAETHFlowPointIngressBandwidthProfilePerEvc;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Methods for translating between SCA API swagger-generated JAVA classes and CE App classes.
 */
public final class ScaApiTranslator {

    private static DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

    private static final Logger log = getLogger(ScaApiTranslator.class);

    // Not to be called
    private ScaApiTranslator() {
    }

    public static CarrierEthernetVirtualConnection toCarrierEthernetVirtualConnection(SCAETHFDFrEC scaethfdFrEC) {

        String evcId = scaethfdFrEC.getId();
        String evcCfgId = scaethfdFrEC.getEvcCfgIdentifier();
        CarrierEthernetVirtualConnection.Type evcType =
                CarrierEthernetVirtualConnection.Type.valueOf(scaethfdFrEC.getEvcServiceType().name());
        Integer maxNumUni = scaethfdFrEC.getEvcStatusMaxNumUni();
        Set<CarrierEthernetUni> uniSet = new HashSet<>();

        CarrierEthernetUni ceUni;
        CarrierEthernetUni.Role ceUniRole = CarrierEthernetUni.Role.ROOT;
        ListIterator<SCAETHFlowPoint> it = scaethfdFrEC.getSCAETHFlowPoints().listIterator();
        while (it.hasNext()) {
            if (evcType.equals(CarrierEthernetConnection.Type.ROOT_MULTIPOINT)) {
                ceUniRole = (it.nextIndex() == 0) ? CarrierEthernetUni.Role.ROOT : CarrierEthernetUni.Role.LEAF;
            }
            SCAETHFlowPoint scaFlowPoint = it.next();
            // Create new CE UNI
            ceUni = toCarrierEthernetUni(scaFlowPoint, ceUniRole);
            if (ceUni == null) {
                return null;
            }
            uniSet.add(ceUni);
        }

        return CarrierEthernetVirtualConnection.builder()
                .id(evcId)
                .cfgId(evcCfgId)
                .type(evcType)
                .maxNumUni(maxNumUni)
                .uniSet(uniSet)
                .build();
    }

    public static SCAETHFDFrEC fromCarrierEthernetVirtualConnection(CarrierEthernetVirtualConnection ceService) {

        SCAETHFDFrEC scaethfdFrEC = new SCAETHFDFrEC();

        scaethfdFrEC.setId(ceService.id());
        scaethfdFrEC.setEvcCfgIdentifier(ceService.cfgId());
        scaethfdFrEC.setEvcServiceType(SCAETHFDFrEC.EvcServiceTypeEnum.valueOf(ceService.type().name()));
        scaethfdFrEC.setEvcStatusMaxNumUni(ceService.maxNumUni());
        scaethfdFrEC.setState(SCAETHFDFrEC.StateEnum.valueOf(ceService.state().name()));

        List<SCAETHFlowPoint> scaFlowPointList = new ArrayList<>();
        SCAETHFlowPoint scaFlowpoint;

        Iterator<CarrierEthernetUni> it = ceService.uniSet().iterator();
        while (it.hasNext()) {
            CarrierEthernetUni ceUni = it.next();
            // Create new SCA Flow Point from CE UNI (will also create an SCA UNI internally)
            scaFlowpoint = fromCarrierEthernetUni(ceUni);
            scaFlowPointList.add(scaFlowpoint);
        }

        scaethfdFrEC.setSCAETHFlowPoints(scaFlowPointList);

        return scaethfdFrEC;
    }

    public static CarrierEthernetUni toCarrierEthernetUni(SCAETHFlowPoint scaFlowPoint,
                                                   CarrierEthernetUni.Role ceUniRole) {

        // TODO: Check for null

        // Get SCA UNI from SCA Flow Point
        SCAETHFPPUNIN scaUni = scaFlowPoint.getScaEthFppUniN();

        // Get UNI id
        String ceUniCfgId = scaUni.getInterfaceCfgIdentifier();

        // Get CE UNI CE-VLAN id
        VlanId ceVlanId = ((scaUni.getCeVlanId() == null) ? null :
                VlanId.vlanId(Short.valueOf(scaUni.getCeVlanId())));

        // TODO: Allow for both ingress and egress BW profiles
        // Get CE UNI BW profile
        CarrierEthernetBandwidthProfile ceBwp = getCeBwpFromScaFlowPoint(scaFlowPoint);

        // Get CE UNI connect point
        DeviceId deviceId = DeviceId.deviceId(scaUni.getTransportPort().getHostname());
        if (deviceService.getDevice(deviceId) == null) {
            log.error("Invalid deviceId {}", deviceId);
            return null;
        }
        PortNumber portNumber = PortNumber.portNumber(scaUni.getTransportPort().getPort());
        if (deviceService.getPort(deviceId, portNumber) == null) {
            log.error("Invalid port {} for device {}", portNumber, deviceId);
            return null;
        }
        ConnectPoint ceUniCp = new ConnectPoint(deviceId, portNumber);

        // Create CE UNI
        return CarrierEthernetUni.builder()
                .cp(ceUniCp)
                .cfgId(ceUniCfgId)
                .role(ceUniRole)
                .ceVlanId(ceVlanId)
                .bwp(ceBwp)
                .build();
    }

    public static SCAETHFlowPoint fromCarrierEthernetUni(CarrierEthernetUni ceUni) {

        SCAETHFlowPoint scaFlowPoint = new SCAETHFlowPoint();
        SCAETHFPPUNIN scaUni = new SCAETHFPPUNIN();

        // Set SCA UNI Transport Port
        SCAETHFPPUNINTransportPort scaPort = new SCAETHFPPUNINTransportPort();
        scaPort.setHostname(ceUni.cp().deviceId().toString());
        scaPort.setPort(ceUni.cp().port().toString());
        scaPort.setVendor(deviceService.getDevice(ceUni.cp().deviceId()).manufacturer());
        scaUni.setTransportPort(scaPort);
        if (ceUni.ceVlanId() != null) {
            scaUni.setCeVlanId(ceUni.ceVlanId().toString());
        }

        scaUni.setInterfaceCfgIdentifier(ceUni.cfgId());

        // Add SCA UNI to SCA Flow Point
        scaFlowPoint.setScaEthFppUniN(scaUni);

        // TODO: Allow for both ingress and egress BW profiles
        // FIXME: A CE UNI may have multiple bandwidth profiles

        // Apply existing BW profile from CE UNI to SCA Flow Point (or UNI)
        applyCeUniBwpToScaFlowPoint(ceUni, scaFlowPoint);

        // TODO: Check if the UNI type needs to be specified

        return scaFlowPoint;
    }

    private static CarrierEthernetBandwidthProfile getCeBwpFromScaFlowPoint(
            SCAETHFlowPoint scaFlowPoint) {

        SCAETHFPPUNIN scaUni = scaFlowPoint.getScaEthFppUniN();
        SCAETHFlowPointIngressBandwidthProfilePerEvc scaBwp = null;
        SCAETHFlowPointIngressBandwidthProfilePerClassOfServiceIdentifier scaCosBwp = null;
        String ceBwpId = null;
        String ceBwpCfgId;
        CarrierEthernetBandwidthProfile.Type ceBwpType;

        SCAETHFlowPointIngressBandwidthProfilePerEvc ifaceBwp =
                scaUni.getInterfaceCfgIngressBwp();
        SCAETHFlowPointIngressBandwidthProfilePerEvc evcBwp =
                scaFlowPoint.getIngressBandwidthProfilePerEvc();
        SCAETHFlowPointIngressBandwidthProfilePerClassOfServiceIdentifier cosBwp =
                scaFlowPoint.getIngressBandwidthProfilePerClassOfServiceIdentifier();

        // TODO: Complete the COS part

        // FIXME: Perform following check
        /*Preconditions.checkArgument(ifaceBwp == null && evcBwp == null && cosBwp == null ||
                (ifaceBwp != null ^ evcBwp != null ^ cosBwp != null),
                                    "Only up to one BW profile can be set per UNI");*/

        if (evcBwp != null) {
            scaBwp = evcBwp;
            ceBwpCfgId = scaBwp.getBwpCfgIdentifier();
            ceBwpType = CarrierEthernetBandwidthProfile.Type.EVC;
        } else if (ifaceBwp != null) {
            scaBwp = ifaceBwp;
            // Use the UNI connect point id
            ceBwpId = scaUni.getTransportPort().getHostname() + "/" + scaUni.getTransportPort().getPort();
            ceBwpType = CarrierEthernetBandwidthProfile.Type.INTERFACE;
            ceBwpCfgId = ifaceBwp.getBwpCfgIdentifier();
            if (ceBwpCfgId == null) {
                ceBwpCfgId = ceBwpId;
            }
        } else if (cosBwp != null) {
            // FIXME: Complete and test the COS part
            scaCosBwp = cosBwp;
            SCAETHFlowPointClassOfServiceIdentifierIp scaCosIdIp =
                    scaFlowPoint.getClassOfServiceIdentifierIp();
            if (scaCosIdIp == null) {
                log.error("CoS ID is required for COS BW profile");
                return null;
            }
            ceBwpId = scaCosIdIp.getClassOfServiceName();
            ceBwpCfgId = scaCosBwp.getBwpCfgIdentifier();
            ceBwpType = CarrierEthernetBandwidthProfile.Type.COS;
        } else {
            return null;
        }

        CeBwpParams ceBwpParams = scaBwp != null ? new CeBwpParams(scaBwp) :
                new CeBwpParams(scaCosBwp);

        return CarrierEthernetBandwidthProfile.builder()
                .id(ceBwpId)
                .cfgId(ceBwpCfgId)
                .type(ceBwpType)
                .cir(ceBwpParams.cir())
                .cbs(ceBwpParams.cbs())
                .eir(ceBwpParams.eir())
                .ebs(ceBwpParams.ebs())
                .build();
    }

    private static class CeBwpParams {

        private Bandwidth cir = Bandwidth.bps((long) 0);
        private Bandwidth eir = Bandwidth.bps((long) 0);
        private long cbs = (long) 0;
        private long ebs = (long) 0;

        CeBwpParams() {}

        CeBwpParams(SCAETHFlowPointIngressBandwidthProfilePerEvc scaBwp) {
            if (scaBwp.getBwpCfgCir() != null) {
                cir = Bandwidth.bps((long) scaBwp.getBwpCfgCir());
            }
            if (scaBwp.getBwpCfgEir() != null) {
                eir = Bandwidth.bps((long) scaBwp.getBwpCfgEir());
            }
            if (scaBwp.getBwpCfgCbs() != null) {
                cbs = (long) scaBwp.getBwpCfgCbs();
            }
            if (scaBwp.getBwpCfgEbs() != null) {
                ebs = (long) scaBwp.getBwpCfgEbs();
            }
        }

        CeBwpParams(SCAETHFlowPointIngressBandwidthProfilePerClassOfServiceIdentifier scaBwp) {
            if (scaBwp.getBwpCfgCir() != null) {
                cir = Bandwidth.bps((long) scaBwp.getBwpCfgCir());
            }
            if (scaBwp.getBwpCfgEir() != null) {
                eir = Bandwidth.bps((long) scaBwp.getBwpCfgEir());
            }
            if (scaBwp.getBwpCfgCbs() != null) {
                cbs = (long) scaBwp.getBwpCfgCbs();
            }
            if (scaBwp.getBwpCfgEbs() != null) {
                ebs = (long) scaBwp.getBwpCfgEbs();
            }
        }

        public Bandwidth cir() {
            return cir;
        }

        public Bandwidth eir() {
            return eir;
        }

        public long cbs() {
            return cbs;
        }

        public long ebs() {
            return ebs;
        }
    }

    private static SCAETHFlowPoint applyCeUniBwpToScaFlowPoint(
            CarrierEthernetUni ceUni, SCAETHFlowPoint scaFlowPoint) {

        CarrierEthernetBandwidthProfile ceBwp = ceUni.bwp();

        SCAETHFlowPoint newScaFlowPoint = scaFlowPoint;
        SCAETHFPPUNIN newScaUni = scaFlowPoint.getScaEthFppUniN();

        if (ceBwp != null) {
            // Prepare SCA bandwidth profile
            SCAETHFlowPointIngressBandwidthProfilePerEvc scaBwp = new SCAETHFlowPointIngressBandwidthProfilePerEvc();

            // TODO: Check for null?
            scaBwp.setBwpCfgCir((int) ceBwp.cir().bps());
            scaBwp.setBwpCfgCbs((int) ceBwp.cbs());
            scaBwp.setBwpCfgEir((int) ceBwp.eir().bps());
            scaBwp.setBwpCfgEbs((int) ceBwp.ebs());

            // TODO: Add the CoS part when it's ready from the CE app side
            if (ceBwp.type().equals(CarrierEthernetBandwidthProfile.Type.EVC)) {
                newScaUni.setEvcId(ceBwp.id());
                newScaFlowPoint.setIngressBandwidthProfilePerEvc(scaBwp);
            } else if (ceBwp.type().equals(CarrierEthernetBandwidthProfile.Type.INTERFACE)) {
                newScaUni.setInterfaceCfgIdentifier(ceUni.cfgId());
                newScaUni.setInterfaceCfgIngressBwp(scaBwp);
            } else {
                log.error("Could not add BW profile for Flow Point {}", scaFlowPoint.toString());
                return null;
            }
        }

        newScaFlowPoint.setScaEthFppUniN(newScaUni);

        return newScaFlowPoint;
    }

}
