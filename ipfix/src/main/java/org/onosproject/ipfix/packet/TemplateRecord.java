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

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * IPFIX Template Record header.
 */
public class TemplateRecord extends Record {
    private static final int HEADER_LENGTH = 4;
    private int templateID;
    private int fieldCount;
    private List<InformationElement> informationElements = new ArrayList<InformationElement>();

    /**
     * Template Record header.
     *
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | Template ID (larger than 255) |         Field Count           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     */

    public int getTemplateID() {
        return templateID;
    }

    public void setTemplateID(int templateID) {
        this.templateID = templateID;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(int fieldCount) {
        this.fieldCount = fieldCount;
    }

    public List<InformationElement> getInformationElements() {
        return informationElements;
    }

    public void setInformationElements(List<InformationElement> informationElements) {
        this.informationElements = informationElements;
        this.fieldCount = this.informationElements.size();
    }

    public int getLength() {
        return HEADER_LENGTH + (informationElements.size() * InformationElement.LENGTH);
    }

    public static TemplateRecord parse(byte[] data) throws HeaderException {
        try {
            if (data.length < HEADER_LENGTH) {
                throw new HeaderException("Data array too short.");
            }
            TemplateRecord tr = new TemplateRecord();
            // template ID
            byte[] templateID = new byte[2];
            System.arraycopy(data, 0, templateID, 0, 2);
            tr.setTemplateID(Ints.fromByteArray(templateID));
            // field count
            byte[] fieldCount = new byte[2];
            System.arraycopy(data, 2, fieldCount, 0, 2);
            tr.setFieldCount(Ints.fromByteArray(fieldCount));
            int offset = HEADER_LENGTH;
            for (int i = 0; i < tr.getFieldCount(); i++) {
                byte[] subData = new byte[InformationElement.LENGTH];
                System.arraycopy(data, offset +
                        (i * InformationElement.LENGTH), subData, 0, InformationElement.LENGTH);
                InformationElement ie = InformationElement.parse(subData);
                tr.getInformationElements().add(ie);
            }
            return tr;
        } catch (Exception e) {
            throw new HeaderException("Parse error: " + e.getMessage());
        }
    }

    public byte[] getBytes() throws HeaderException {
        try {
            int length = HEADER_LENGTH + (informationElements.size() * InformationElement.LENGTH);
            byte[] data = new byte[length];
            // template ID
            System.arraycopy(Shorts.toByteArray((short) getTemplateID()), 0, data, 0, 2);
            // field count
            System.arraycopy(Shorts.toByteArray((short) getFieldCount()), 0, data, 2, 2);
            // information elements
            int offset = HEADER_LENGTH;
            for (InformationElement ie : informationElements) {
                System.arraycopy(ie.getBytes(), 0, data, offset, InformationElement.LENGTH);
                offset += InformationElement.LENGTH;
            }
            return data;
        } catch (Exception e) {
            throw new HeaderException("Error while generating the bytes: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TemplateRecord]: ");
        sb.append("Template ID: ");
        sb.append(templateID);
        sb.append(", Field count: ");
        sb.append(fieldCount);
        sb.append(", Information elements: ");
        sb.append(informationElements.size());
        sb.append(", ");
        for (InformationElement ie : informationElements) {
            sb.append(ie);
            sb.append(", ");
        }

        return sb.toString();
    }
}