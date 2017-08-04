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

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * IPFIX Information Element entity.
 */
public class InformationElement implements AbstractIpfixPacketInterface {
    public static final int LENGTH = 4;

    private int informationElementID;
    private int fieldLength;

    /**
     * @param ie Information Element construtor from Ie Enum.
     */
    public InformationElement(Ie ie) {
        this.informationElementID = ie.getId();
        this.fieldLength = ie.getLength();
    }

    /**
     * Information Element construtor from blank.
     */
    public InformationElement() {
        this.informationElementID = 0;
        this.fieldLength = 0;
    }

    public int getInformationElementID() {
        return informationElementID;
    }

    public void setInformationElementID(int informationElementID) {
        this.informationElementID = informationElementID;
    }

    public int getFieldLength() {
        return fieldLength;
    }

    public void setFieldLength(int fieldLength) {
        this.fieldLength = fieldLength;
    }

    public static InformationElement parse(byte[] data) throws HeaderException {
        try {
            if (data.length < LENGTH) {
                throw new HeaderException("Data array too short.");
            }
            InformationElement ie = new InformationElement();
            // information element ID
            byte[] informationElementID = new byte[2];
            System.arraycopy(data, 0, informationElementID, 0, 2);
            ie.setInformationElementID(Ints.fromByteArray(informationElementID));
            // field length
            byte[] fieldLength = new byte[2];
            System.arraycopy(data, 2, fieldLength, 0, 2);
            ie.setFieldLength(Ints.fromByteArray(fieldLength));
            return ie;
        } catch (Exception e) {
            throw new HeaderException("Parse error: " + e.getMessage());
        }
    }

    public byte[] getBytes() throws HeaderException {
        try {
            byte[] data = new byte[LENGTH];
            // information element ID
            System.arraycopy(Shorts.toByteArray((short) getInformationElementID()), 0, data, 0, 2);
            // field length
            System.arraycopy(Shorts.toByteArray((short) getFieldLength()), 0, data, 2, 2);
            return data;
        } catch (Exception e) {
            throw new HeaderException("Error while generating the bytes: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[InformationElement]: ");
        sb.append("ID: ");
        sb.append(informationElementID);
        sb.append(", ");
        sb.append("Field length: ");
        sb.append(fieldLength);
        return sb.toString();
    }
}
