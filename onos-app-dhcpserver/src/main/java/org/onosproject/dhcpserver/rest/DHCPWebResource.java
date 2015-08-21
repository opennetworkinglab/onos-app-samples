/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.dhcpserver.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.dhcpserver.DHCPService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Manage DHCP address assignments.
 */
@Path("/")
public class DHCPWebResource extends AbstractWebResource {

    /**
     * Get DHCP server configuration data.
     * Shows lease, renewal and rebinding times in seconds.
     *
     * @return 200 OK
     */
    @GET
    @Path("config")
    public Response getConfigs() {
        DHCPService service = get(DHCPService.class);
        ObjectNode node = mapper().createObjectNode()
                .put("leaseTime", service.getLeaseTime())
                .put("renewalTime", service.getRenewalTime())
                .put("rebindingTime", service.getRebindingTime());
        return ok(node.toString()).build();
    }


    // POST should accept a single mac/ip binding with
    /*
        {
            "mac": "00:......00",
            "ip": "12.34.56.78"
        }
     */

    //    @POST
    //    @Path("mappings")



    // GET all mappings should look like this:
    /*
        {
            "mappings": [
                {
                    "mac": "00:......00",
                    "ip": "12.34.56.78",
                    ...
                }
            ]
        }
     */
}
