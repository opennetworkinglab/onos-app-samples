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
import com.google.common.primitives.Longs;

/**
 * IPFIX Sampling Data Record.
 */
public class SamplingDataRecord extends DataRecord {
    private static final int HEADER_LENGTH = 16; // 2 bytes padding

    private long observationDomainId;
    private int selectorAlgorithm;
    private long samplingPacketInterval;
    private long samplingPacketSpace;

    public long getObservationDomainId() {
        return observationDomainId;
    }

    public void setObservationDomainId(long observationDomainId) {
        this.observationDomainId = observationDomainId;
    }

    public int getSelectorAlgorithm() {
        return selectorAlgorithm;
    }

    public void setSelectorAlgorithm(int selectorAlgorithm) {
        this.selectorAlgorithm = selectorAlgorithm;
    }

    public long getSamplingPacketInterval() {
        return samplingPacketInterval;
    }

    public void setSamplingPacketInterval(long samplingPacketInterval) {
        this.samplingPacketInterval = samplingPacketInterval;
    }

    public long getSamplingPacketSpace() {
        return samplingPacketSpace;
    }

    public void setSamplingPacketSpace(long samplingPacketSpace) {
        this.samplingPacketSpace = samplingPacketSpace;
    }

    public static SamplingDataRecord parse(byte[] data) throws HeaderException {
        try {
            if (data.length < HEADER_LENGTH) {
                throw new HeaderException("Data array too short.");
            }
            SamplingDataRecord sdr = new SamplingDataRecord();
            // observationDomainId
            byte[] observationDomainId = new byte[4];
            System.arraycopy(data, 0, observationDomainId, 0, 4);
            sdr.setObservationDomainId(Longs.fromByteArray(observationDomainId));
            // selectorAlgorithm
            byte[] selectorAlgorithm = new byte[2];
            System.arraycopy(data, 4, selectorAlgorithm, 0, 2);
            sdr.setSelectorAlgorithm(Ints.fromByteArray(selectorAlgorithm));
            // samplingPacketInterval
            byte[] samplingPacketInterval = new byte[4];
            System.arraycopy(data, 6, samplingPacketInterval, 0, 4);
            sdr.setSamplingPacketInterval(Longs.fromByteArray(samplingPacketInterval));
            // samplingPacketSpace
            byte[] samplingPacketSpace = new byte[4];
            System.arraycopy(data, 10, samplingPacketSpace, 0, 4);
            sdr.setSamplingPacketSpace(Longs.fromByteArray(samplingPacketSpace));
            return sdr;
        } catch (Exception e) {
            throw new HeaderException("Parse error: " + e.getMessage());
        }
    }

    public byte[] getBytes() throws HeaderException {
        try {
            byte[] data = new byte[HEADER_LENGTH];
            // observationDomainId
            System.arraycopy(Ints.toByteArray((int) getObservationDomainId()), 0, data, 0, 4);
            // selectorAlgorithm
            System.arraycopy(Ints.toByteArray((int) getSelectorAlgorithm()), 0, data, 4, 2);
            // samplingPacketInterval
            System.arraycopy(Ints.toByteArray((int) getSamplingPacketInterval()), 0, data, 6, 4);
            // samplingPacketSpace
            System.arraycopy(Ints.toByteArray((int) getSamplingPacketSpace()), 0, data, 10, 4);
            return data;
        } catch (Exception e) {
            throw new HeaderException("Error while generating the bytes: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[SamplingDataRecord]: ");
        sb.append(" Observation domain ID: ");
        sb.append(observationDomainId);
        sb.append(", Selector algorithm: ");
        sb.append(selectorAlgorithm);
        sb.append(", Sampling packet interval: ");
        sb.append(samplingPacketInterval);
        sb.append(", Sampling packet space: ");
        sb.append(samplingPacketSpace);
        return sb.toString();
    }
}
