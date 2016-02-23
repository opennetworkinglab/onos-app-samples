/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ecord.metro;

import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity to represent packet link realized by optical intent.
 */
public class PacketLinkRealizedByOptical {
    private final ConnectPoint src, dst;
    private final Bandwidth bandwidth;
    // TODO should be list of Intent Key?
    private final Key realizingIntentKey;
    // established=false represents that this (packet) link is expected to be
    // discovered after underlying (optical) path has been provisioned.
    private boolean established;

    public PacketLinkRealizedByOptical(ConnectPoint src, ConnectPoint dst,
                                       Key realizingIntentKey, Bandwidth bandwidth) {
        this.src = src;
        this.dst = dst;
        this.realizingIntentKey = realizingIntentKey;
        this.bandwidth = bandwidth;
        this.established = false;
    }

    public static PacketLinkRealizedByOptical create(ConnectPoint src, ConnectPoint dst,
                                                     OpticalCircuitIntent intent) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(intent);

        long rate = intent.getSignalType().bitRate();
        return new PacketLinkRealizedByOptical(src, dst, intent.key(), Bandwidth.bps(rate));
    }

    public static PacketLinkRealizedByOptical create(ConnectPoint src, ConnectPoint dst,
                                                     OpticalConnectivityIntent intent) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(intent);

        long rate = intent.getSignalType().bitRate();
        return new PacketLinkRealizedByOptical(src, dst, intent.key(), Bandwidth.bps(rate));
    }

    public ConnectPoint src() {
        return src;
    }

    public ConnectPoint dst() {
        return dst;
    }

    public Bandwidth bandwidth() {
        return bandwidth;
    }

    public Key realizingIntentKey() {
        return realizingIntentKey;
    }

    public boolean isEstablished() {
        return established;
    }

    public void setEstablished(boolean established) {
        this.established = established;
    }

    public boolean equals(ConnectPoint src, ConnectPoint dst) {
        return (this.src.equals(src) && this.dst.equals(dst));
    }

}
