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

package org.onosproject.ovsdbrest;

import org.onlab.packet.IpAddress;

/**
 * APIs for ovsdb driver access.
 */
public interface OvsdbBridgeService {

    /**
     * Creates a new bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     */
    void createBridge(IpAddress ovsdbAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeAlreadyExistsException;

    /**
     * Deletes a bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     */
    void deleteBridge(IpAddress ovsdbAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeNotFoundException;

    /**
     * Adds a port to a bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the port to attach to the bridge
     */
    void addPort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Removes a port from a bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the port to remove from the bridge
     */
    void removePort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Adds a patch port to a bridge setting it as peer of an other port.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the port name
     * @param patchPeer the name of the peer port
     */
    void createPatchPeerPort(IpAddress ovsdbAddress, String bridgeName, String portName, String patchPeer)
            throws OvsdbRestException.OvsdbDeviceException;

    /**
     * Creates a GRE tunnel from a bridge to a remote destination.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the new GRE port
     * @param localIp local end point of the GRE tunnel
     * @param remoteIp remote end point of GRE tunnel
     * @param key the tunnel key, should represent a 32 bit hexadecimal number
     */
    void createGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName, IpAddress localIp,
                         IpAddress remoteIp, String key)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Deletes a GRE tunnel given the port name.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the GRE
     */
    void deleteGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException;
}
