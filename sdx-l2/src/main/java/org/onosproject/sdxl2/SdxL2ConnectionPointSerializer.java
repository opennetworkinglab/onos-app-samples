/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.sdxl2;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.List;

/**
 * Kryo serializer for SDX-L2 connection point.
 */
public class SdxL2ConnectionPointSerializer extends Serializer<SdxL2ConnectionPoint> {

    /**
     * Serialize the object using kryo.
     *
     * @param kryo the serializer
     * @param output the output
     * @param object the object to serialize
     */
    public void write(Kryo kryo, Output output, SdxL2ConnectionPoint object) {
        kryo.writeClassAndObject(output, object.name());
        kryo.writeClassAndObject(output, object.connectPoint());
        kryo.writeClassAndObject(output, object.macAddress());
        kryo.writeClassAndObject(output, object.vlanIds());
    }

    /**
     * Create an object from one serialized using kryo.
     *
     * @param kryo the serializer
     * @param input the inpunt
     * @param type the object to create
     * @return the object
     */
    public SdxL2ConnectionPoint read(Kryo kryo, Input input, Class<SdxL2ConnectionPoint> type) {
        String name = (String) kryo.readClassAndObject(input);
        ConnectPoint cp = (ConnectPoint) kryo.readClassAndObject(input);
        MacAddress mac = (MacAddress) kryo.readClassAndObject(input);
        List<VlanId> vlans = (List<VlanId>) kryo.readClassAndObject(input);
        return new SdxL2ConnectionPoint(name, cp, vlans, mac);
    }
}
