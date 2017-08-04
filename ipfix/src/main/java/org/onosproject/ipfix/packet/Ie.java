/*
 * Copyright 2015 Open Networking Foundation
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
package org.onosproject.ipfix.packet;

/**
 * IPFIX Information Elements from IANA.
 */
public enum Ie {
    octetDeltaCount(1, 8),
    packetDeltaCount(2, 8),
    protocolIdentifier(4, 1),
    ipClassOfService(5, 1),
    sourceTransportPort(7, 2),
    sourceIPv4Address(8, 4),
    ingressInterface(10, 4),
    destinationTransportPort(11, 2),
    destinationIPv4Address(12, 4),
    egressInterface(14, 4),
    sourceIPv6Address(27, 16),
    destinationIPv6Address(28, 16),
    flowLabelIPv6(31, 4),
    sourceMacAddress(56, 6),
    vlanId(58, 2),
    destinationMacAddress(80, 6),
    exporterIPv4Address(130, 4),
    exporterIPv6Address(131, 16),
    droppedOctetDeltaCount(132, 8),
    droppedPacketDeltaCount(133, 8),
    flowStartMilliseconds(152, 8),
    flowEndMilliseconds(153, 8),
    ethernetType(256, 2);

    /**
     * IPFIX Information Element IDs from IANA.
     * Available at: http://www.iana.org/assignments/ipfix/ipfix.xhtml.
     */
    private final int id;

    /**
     * IPFIX Information Element length in bytes.
     */
    private final int length;

    /**
     * IPFIX Information Element constructor.
     *
     * @param id IPFIX Information Element id
     * @param length IPFIX Information Element length in bytes
     */
    private Ie(int id, int length) {
        this.id = id;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public int getLength() {
        return length;
    }
}