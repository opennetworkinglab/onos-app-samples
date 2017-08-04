/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ecord.carrierethernet.api;

import com.google.common.annotations.Beta;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetForwardingConstruct;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetLogicalTerminationPoint;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetNetworkInterface;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetUni;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onosproject.net.ConnectPoint;

import java.util.Map;
import java.util.Set;

/**
 * Service interface for Carrier Ethernet.
 *
 * Defines interactions with CE concepts such as Ethernet Virtual Circuit (EVC), Forwarding Construct (FC),
 * Logical Termination Point (LTP), and User to Network Interface (UNI).
 */
@Beta
public interface CarrierEthernetService {
    /**
     * Returns the map of installed EVCs.
     *
     * @return map of installed EVCs
     */
    Map<String, CarrierEthernetVirtualConnection> evcMap();

    /**
     * Get an installed EVC using its id.
     *
     * @param evcId the EVC id
     * @return the EVC representation or null if the EVC doesn't exist
     */
    CarrierEthernetVirtualConnection getEvc(String evcId);

    // TODO: Add method to remove a UNI from an already installed EVC

    /**
     * Establish connectivity according to the EVC type (E-Line, E-Tree, E-LAN) and the EVC parameters.
     *
     * @param evc the EVC representation
     * @return the (potentially modified) EVC that was installed or null in case of failure
     */
    CarrierEthernetVirtualConnection installEvc(CarrierEthernetVirtualConnection evc);

    /**
     * Re-establish connectivity for an existing EVC.
     *
     * @param evc the updated EVC definition
     * @return the (potentially modified) EVC that was installed or null if EVC connectivity could not be established
     */
    CarrierEthernetVirtualConnection updateEvc(CarrierEthernetVirtualConnection evc);


    /**
     * Removes all resources associated with a specific installed EVC.
     *
     * @param evcId the EVC id
     */
    void removeEvc(String evcId);


    /**
     * Removes all installed EVCs and the associated resources.
     * <p>
     * This will be called either from the deactivate method or as a response to a CLI/REST command.
     */
    void removeAllEvcs();

    /**
     * Enable or disable EVC fragmentation into FCs.
     *
     * @param evcFragmentationEnabled true to enable fragmentation, false otherwise
     */
    void setEvcFragmentation(boolean evcFragmentationEnabled);


    /**
     * Checks the fragmentation state of the EVC.
     *
     * @return true if fragmentation is enabled, false otherwise
     */
    boolean getEvcFragmentation();

    /**
     * Set the EVC fragmentation flag to the value before its last change.
     *
     */
    void resetEvcFragmentation();

    /**
     * Get the map containing all installed FCs.
     *
     * @return the FC map
     */
    Map<String, CarrierEthernetForwardingConstruct> fcMap();

    /**
     * Get an installed FC using its id.
     *
     * @param fcId the FC id
     * @return the FC representation or null if the EVC doesn't exist
     */
    CarrierEthernetForwardingConstruct getFc(String fcId);

    /**
     * Installs all resources associated with a specific FC.
     *
     * @param fc the FC to install
     * @return the FC that was installed
     */
    CarrierEthernetForwardingConstruct installFc(CarrierEthernetForwardingConstruct fc);

    /**
     * Re-establish connectivity for an existing FC.
     *
     * @param fc the updated FC representation
     * @return the possibly modified FC that was installed or null if updated FC could not be installed
     */
    CarrierEthernetForwardingConstruct updateFc(CarrierEthernetForwardingConstruct fc);

    /**
     * Removes all resources associated with a specific FC.
     *
     * @param fcId the FC id
     * @return the FC that was removed or null if removal failed
     */
    CarrierEthernetForwardingConstruct removeFc(String fcId);

    /**
     * Removes all resources associated with the application.
     *
     * This will be called either from the deactivate method or as a response to a CLI command.
     */
    void removeAllFcs();

    /**
     * Get the map containing all global UNIs.
     *
     * @return the global UNI map
     */
    Map<String, CarrierEthernetUni> getUniMap();

    /**
     * Adds a potential UNI to the global UNI map if they are not already there.
     *
     * @param uni the potential UNI to add to global UNI map
     * @return the UNI that was added or null if UNI existed already
     */
    CarrierEthernetUni addGlobalUni(CarrierEthernetUni uni);

    /**
     * Remove an UNI from the set of global UNIs.
     *
     * @param uniId the id of the UNI to be removed
     * @return the UNI that was removed or null in case of failure (didn't exist of refCount was not 0)
     */
    CarrierEthernetUni removeGlobalUni(String uniId);

    // TODO: Add removeAllGlobalUnis method (or command only?)

    /**
     * Creates a new UNI associated with the provided connect point.
     *
     * Conditions for validating an UNI:
     * - ConnectPoint deviceId and Port are valid
     * - Port is enabled
     *
     * @param cp the connect point to be associated with the generated UNI
     * @return a new validated UNI or null if the validation failed
     */
    CarrierEthernetUni generateUni(ConnectPoint cp);

    /**
     * Returns all potential UNIs from the topology.
     *
     * @param excludeAdded indicates that UNIs already added in the UNI map should not be in the returned set
     * @param includeRemoved indicates that UNIs explicitly removed from the UNI map should be in the returned set
     * @return set of all potential UNIs in the topology
     */
    Set<CarrierEthernetUni> getUnisFromTopo(boolean excludeAdded, boolean includeRemoved);

    /**
     * Get the map containing all global LTPs.
     *
     * @return the global LTP map
     */
    Map<String, CarrierEthernetLogicalTerminationPoint> ltpMap();

    /**
     * Adds a potential LTP and its UNI or pair INNI to the global LTP/UNI maps if they are not already there.
     *
     * @param ltp the potential LTP to add to global LTP map
     * @return the LTP that was added or null if it already existed
     */
     CarrierEthernetLogicalTerminationPoint addGlobalLtp(CarrierEthernetLogicalTerminationPoint ltp);

    /**
     * Remove an LTP from the set of global LTPs, as well as the corresponding INNI LTP at the other end of the link.
     *
     * @param ltpId the id of the LTP to be removed
     * @return the LTP that was removed or null in case of failure (didn't exist of refCount was not 0)
     */
    CarrierEthernetLogicalTerminationPoint removeGlobalLtp(String ltpId);

    // TODO: Add removeAllGlobalLtps method (or command only?)

    /**
     * Creates a new LTP of the provided type and associated with the provided connect point.
     *
     * Conditions for validating an LTP:
     * - ConnectPoint deviceId and Port are valid
     * - Port is enabled
     *
     * @param cp the connect point to be associated with the generated LTP
     * @param ltpType the type of the LTP to be generated (UNI/INNI/ENNI)
     * @return a new validated LTP or null if the validation failed
     */
    CarrierEthernetLogicalTerminationPoint generateLtp(ConnectPoint cp, CarrierEthernetNetworkInterface.Type ltpType);

    /**
     * Returns all potential LTPs from the topology.
     *
     * @param excludeAdded indicates that LTPs already added in the LTP map should not be in the returned set
     * @param includeRemoved indicates that LTPs explicitly removed from the LTP map should be in the returned set
     * @return set of all potential LTPs in the topology
     */
    Set<CarrierEthernetLogicalTerminationPoint> getLtpsFromTopo(boolean excludeAdded, boolean includeRemoved);
}