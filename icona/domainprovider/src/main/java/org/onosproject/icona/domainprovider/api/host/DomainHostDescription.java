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

package org.onosproject.icona.domainprovider.api.host;

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onosproject.icona.domainprovider.api.DomainElement;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostDescription;

import java.util.Set;

/**
 * Description of a host that belongs to another domain.
 */
public interface DomainHostDescription extends HostDescription, DomainElement {

    /**
     * Verify if the domain host corresponds to the specified host in the local domain.
     * The domainHost will have a host location pointing to a (virtual) domain device
     * exposed to the other domains.
     *
     * @param host to compare with
     * @return true if they correspond
     */
    boolean isHost(Host host);

    /**
     * Returns the ipv4 subnets.
     *
     * @return set of ipv4 subnets
     */
    Set<Ip4Prefix> ip4Subnets();

    /**
     * Returns the ipv6 subnets.
     *
     * @return set of ipv6 subnets
     */
    Set<Ip6Prefix> ip6Subnets();
}
