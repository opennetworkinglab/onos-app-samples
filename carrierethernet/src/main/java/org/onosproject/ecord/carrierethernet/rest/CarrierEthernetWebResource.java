/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ecord.carrierethernet.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.ecord.carrierethernet.api.CarrierEthernetService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

/**
 * Carrier Ethernet web resource.
 */
@Path("carrierethernet")
public class CarrierEthernetWebResource extends AbstractWebResource {

    private static final String EVCS = "evcs";
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode evcsNode = root.putArray(EVCS);

    CarrierEthernetService ceManager = get(CarrierEthernetService.class);

    /**
     * Gets all EVC entries. Returns array of all EVCs in the system.
     *
     * @return 200 OK with a collection of Evcs
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("evc")
    public Response getEvcs() {
        ceManager.evcMap().values().stream()
                .forEach(evc -> evcsNode.add(
                        codec(CarrierEthernetVirtualConnection.class)
                                .encode(evc, this)));


        return ok(root).build();
    }

    /**
     * Gets an EVC entry by deviceId.
     * @param evcId The ID of an EVC
     * @return 200 OK with the requested Evc.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("evc/{evcId}")
    public Response getEvc(@PathParam("evcId") String evcId) {
        ObjectNode evc = codec(CarrierEthernetVirtualConnection.class)
                .encode(ceManager.getEvc(evcId), this);
        return ok(evc).build();
    }

    /**
     * Install an EVC with given parameters.
     * @param stream An input stream
     *
     * @return 200 OK if the EVC was installed
     */
    @POST
    @Path("evc")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setEvc(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            CarrierEthernetVirtualConnection evc =
                    codec(CarrierEthernetVirtualConnection.class).decode(jsonTree, this);
            ceManager.installEvc(evc);
            root.put("evcId", evc.id());
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }

    /**
     * Removes all EVCs installed in the CE app.
     *
     * @return 204 NO CONTENT
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("evc")
    public Response removeAllEvc() {
        ceManager.removeAllEvcs();
        return Response.noContent().build();
    }

    /**
     * Removes one EVCs by evcId.
     *
     * @param evcId the EVC to remove.
     * @return 204 NO CONTENT
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("evc/{evcId}")
    public Response removeEvcWithId(@PathParam("evcId") String evcId) {
        ceManager.removeEvc(evcId);
        return Response.noContent().build();
    }

}
