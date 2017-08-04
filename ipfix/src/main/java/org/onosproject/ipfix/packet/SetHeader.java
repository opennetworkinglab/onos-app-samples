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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * IPFIX Set Header format.
 */
public class SetHeader extends AbstractIpfixPacketEntity implements AbstractIpfixPacketInterface {
    private static final Logger LOGGER = Logger.getLogger(SetHeader.class.getName());
    private static final int HEADER_LENGTH = 4;
    private static final int TEMPLATE_SET_ID = 2;
    private static final int OPTION_SET_ID = 3;

    private int setID;
    private int length;
    private List<DataRecord> dataRecords = new ArrayList<DataRecord>();
    private List<TemplateRecord> templateRecords = new ArrayList<TemplateRecord>();
    private List<OptionTemplateRecord> optionTemplateRecords = new ArrayList<OptionTemplateRecord>();

    /**
     * Set Header format.
     *
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |          Set ID               |          Length               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     */

    /**
     * A value of 2 is reserved for Template Sets. A value of 3 is reserved
     * for Options Template Sets.  Values from 4 to 255 are reserved for
     * future use.  Values 256 and above are used for Data Sets.  The Set ID
     * values of 0 and 1 are not used, for historical reasons.
     *
     * @return Set ID
     */
    public int getSetID() {
        return setID;
    }

    public void setSetID(int setID) {
        this.setID = setID;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<DataRecord> getDataRecords() {
        return dataRecords;
    }

    public void setDataRecords(List<DataRecord> dataRecords) {
        this.dataRecords = dataRecords;
        updateLength();
    }

    public List<TemplateRecord> getTemplateRecords() {
        return templateRecords;
    }

    public void setTemplateRecords(List<TemplateRecord> templateRecords) {
        this.templateRecords = templateRecords;
        updateLength();
    }

    public List<OptionTemplateRecord> getOptionTemplateRecords() {
        return optionTemplateRecords;
    }

    public void setOptionTemplateRecords(List<OptionTemplateRecord> optionTemplateRecords) {
        this.optionTemplateRecords = optionTemplateRecords;
        updateLength();
    }

    /**
     * Updates SetHeaderlength when new records are set/added.
     */
    private void updateLength() {
        int newLength = 0;
        for (TemplateRecord template : templateRecords) {
            newLength += template.getLength();
        }
        for (OptionTemplateRecord optionTemplate : optionTemplateRecords) {
            newLength += optionTemplate.getLength();
        }
        for (DataRecord record : dataRecords) {
            newLength += record.getLength();
        }
        this.length = newLength + HEADER_LENGTH;
    }

    public static SetHeader parse(byte[] data) throws HeaderException {
        try {
            if (data.length < HEADER_LENGTH) {
                throw new HeaderException("Data array too short.");
            }
            SetHeader sh = new SetHeader();
            // set id
            byte[] setID = new byte[2];
            System.arraycopy(data, 0, setID, 0, 2);
            sh.setSetID(Ints.fromByteArray(setID));
            // length
            byte[] length = new byte[2];
            System.arraycopy(data, 2, length, 0, 2);
            sh.setLength(Ints.fromByteArray(length));
            // 2 -> template sets;
            if (sh.getSetID() == TEMPLATE_SET_ID) {
                int offset = HEADER_LENGTH;
                byte[] subData = new byte[sh.getLength() - offset];
                System.arraycopy(data, offset, subData, 0, subData.length);
                TemplateRecord tr = TemplateRecord.parse(subData);
                sh.getTemplateRecords().add(tr);
            } else if (sh.getSetID() == OPTION_SET_ID) {  // 3 -> template option sets
                int offset = HEADER_LENGTH;
                byte[] subData = new byte[sh.getLength() - offset];
                System.arraycopy(data, offset, subData, 0, subData.length);
                OptionTemplateRecord otr = OptionTemplateRecord.parse(subData);
                sh.getOptionTemplateRecords().add(otr);
            } else if (sh.getSetID() == 256) { // > 256 -> data record;
                int offset = HEADER_LENGTH;
                byte[] subData = new byte[sh.getLength() - offset];
                System.arraycopy(data, offset, subData, 0, subData.length);
                SamplingDataRecord sdr = SamplingDataRecord.parse(subData);
                sh.getDataRecords().add(sdr);
            } else {
                LOGGER.log(Level.INFO, "Set ID " + sh.getSetID() + " is unknown and not handled");
            }
            return sh;
        } catch (Exception e) {
            throw new HeaderException("Parse error: " + e.getMessage());
        }
    }

    public byte[] getBytes() throws HeaderException {
        try {
            int length = HEADER_LENGTH;
            for (DataRecord dr : dataRecords) {
                length += dr.getLength();
            }
            for (TemplateRecord tr : templateRecords) {
                length += tr.getLength();
            }
            for (OptionTemplateRecord otr : optionTemplateRecords) {
                length += otr.getLength();
            }
            byte[] data = new byte[length];
            // set id
            System.arraycopy(Shorts.toByteArray((short) getSetID()), 0, data, 0, 2);
            // length
            System.arraycopy(Shorts.toByteArray((short) getLength()), 0, data, 2, 2);
            // data record
            int offset = HEADER_LENGTH;
            for (DataRecord record : dataRecords) {
                System.arraycopy(record.getBytes(), 0, data, offset, record.getLength());
                offset += record.getLength();
            }
            for (TemplateRecord record : templateRecords) {
                byte[] recordData = record.getBytes();
                System.arraycopy(recordData, 0, data, offset, record.getLength());
                offset += recordData.length;
            }
            for (OptionTemplateRecord record : optionTemplateRecords) {
                byte[] recordData = record.getBytes();
                System.arraycopy(recordData, 0, data, offset, record.getLength());
                offset += recordData.length;
            }
            return data;
        } catch (Exception e) {
            throw new HeaderException("Error while generating the bytes: " + e.getMessage());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[SetHeader]: ");
        sb.append("Set ID: ");
        sb.append(setID);
        sb.append(", Length: ");
        sb.append(length);
        sb.append(", Data records: ");
        sb.append(dataRecords.size());
        sb.append(", ");
        for (DataRecord record : dataRecords) {
            sb.append(record);
            sb.append(", ");
        }
        sb.append("Template records: ");
        sb.append(templateRecords.size());
        sb.append(", ");
        for (TemplateRecord record : templateRecords) {
            sb.append(record);
            sb.append(", ");
        }
        sb.append("Option template records: ");
        sb.append(optionTemplateRecords.size());
        sb.append(", ");
        for (OptionTemplateRecord record : optionTemplateRecords) {
            sb.append(record);
            sb.append(", ");
        }
        return sb.toString();
    }
}