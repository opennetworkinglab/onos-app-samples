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
 * IPFIX Option Template Header.
 */
public class OptionTemplateRecord extends Record {
    private static final int HEADER_LENGTH = 6;

    private int templateID;
    private int fieldCount;
    private int scopeFieldCount;
    private List<InformationElement> informationElements = new ArrayList<InformationElement>();
    private List<InformationElement> scopeInformationElements = new ArrayList<InformationElement>();

    /**
     * Option Template Header format.
     *
     *   0                   1                   2                   3
     *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  | Template ID (larger than 255) |         Field Count           |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |      Scope Field Count        |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
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

    public int getScopeFieldCount() {
        return scopeFieldCount;
    }

    public void setScopeFieldCount(int scopeFieldCount) {
        this.scopeFieldCount = scopeFieldCount;
    }

    public List<InformationElement> getInformationElements() {
        return informationElements;
    }

    public void setInformationElements(List<InformationElement> informationElements) {
        this.informationElements = informationElements;
    }

    public List<InformationElement> getScopeInformationElements() {
        return scopeInformationElements;
    }

    public void setScopeInformationElements(List<InformationElement> scopeInformationElements) {
        this.scopeInformationElements = scopeInformationElements;
    }

    public static OptionTemplateRecord parse(byte[] data) throws HeaderException {
        try {
            if (data.length < HEADER_LENGTH) {
                throw new HeaderException("Data array too short.");
            }
            OptionTemplateRecord otr = new OptionTemplateRecord();
            // template ID
            byte[] templateID = new byte[2];
            System.arraycopy(data, 0, templateID, 0, 2);
            otr.setTemplateID(Ints.fromByteArray(templateID));
            // field count
            byte[] fieldCount = new byte[2];
            System.arraycopy(data, 2, fieldCount, 0, 2);
            otr.setFieldCount(Ints.fromByteArray(fieldCount));
            // scope field count
            byte[] scopeFieldCount = new byte[2];
            System.arraycopy(data, 4, scopeFieldCount, 0, 2);
            otr.setScopeFieldCount(Ints.fromByteArray(scopeFieldCount));
            int offset = HEADER_LENGTH;
            for (int i = 0; i < otr.getFieldCount(); i++) {
                byte[] subData = new byte[InformationElement.LENGTH];
                System.arraycopy(data, offset +
                        (i * InformationElement.LENGTH), subData, 0, InformationElement.LENGTH);
                InformationElement ie = InformationElement.parse(subData);
                if (i < otr.getScopeFieldCount()) {
                    otr.getScopeInformationElements().add(ie);
                } else {
                    otr.getInformationElements().add(ie);
                }
            }
            return otr;
        } catch (Exception e) {
            throw new HeaderException("Parse error: " + e.getMessage());
        }
    }

    public byte[] getBytes() throws HeaderException {
        try {
            int length = HEADER_LENGTH + (scopeInformationElements.size() * InformationElement.LENGTH) +
                    (informationElements.size() * InformationElement.LENGTH);
            if (length % 4 != 0) {
                length += (length % 4); // padding
            }
            byte[] data = new byte[length];
            // template ID
            System.arraycopy(Shorts.toByteArray((short) getTemplateID()), 0, data, 0, 2);
            // field count
            System.arraycopy(Shorts.toByteArray((short) getFieldCount()), 0, data, 2, 2);
            // scope field count
            System.arraycopy(Shorts.toByteArray((short) getScopeFieldCount()), 0, data, 4, 2);
            // information elements
            int offset = HEADER_LENGTH;
            for (InformationElement ie : scopeInformationElements) {
                System.arraycopy(ie.getBytes(), 0, data, offset, InformationElement.LENGTH);
                offset += InformationElement.LENGTH;
            }
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
        sb.append("[OptionTemplateRecord]: ");
        sb.append("Template ID: ");
        sb.append(templateID);
        sb.append(", Field count: ");
        sb.append(fieldCount);
        sb.append(", Scope field count: ");
        sb.append(scopeFieldCount);
        sb.append(", Information elements: ");
        sb.append(informationElements);
        sb.append(", Scope Information elements: ");
        sb.append(scopeInformationElements.size());
        sb.append(", ");
        for (InformationElement ie : informationElements) {
            sb.append(ie);
            sb.append(", ");
        }
        return sb.toString();
    }
}
