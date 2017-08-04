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
 * Abstract entity of the IPFIX Packet entities.
 * Used for Records and Headers of variable length.
 */
public abstract class AbstractIpfixPacketEntity {

    protected int length;

    /**
     * Sets the length of the IPFIX entity.
     *
     * @param length length of the entity
     */
    protected void setLength(int length) {
        this.length = length;
    }

    /**
     * Returns the length of the IPFIX entity.
     *
     * @return length length of the entity
     */
    public int getLength() {
        return length;
    }
}
