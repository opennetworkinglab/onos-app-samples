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

package org.onosproject.ovsdbrest.rest;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.ovsdbrest.OvsdbRestException;
import org.onosproject.ovsdbrest.OvsdbBridgeService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * REST APIs for create/delete a bridge and create a port.
 */

@Path("/")
public class OvsdbBridgeWebResource extends AbstractWebResource {
    private final Logger log = getLogger(getClass());

    @GET
    @Path("/test")
    public Response getTest() {
        ObjectNode responseBody = new ObjectNode(JsonNodeFactory.instance);
        responseBody.put("message", "it works!");
        return Response.status(200).entity(responseBody).build();
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addBridge(InputStream stream,
                              @PathParam("ovsdb-ip") String ovsdbIp,
                              @PathParam("bridge-name") String bridgeName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.createBridge(ovsdbAddress, bridgeName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeAlreadyExistsException ex) {
            return Response.status(Response.Status.CONFLICT).entity("A bridge with this name already exists").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{ovsdb-ip}/bridge/{bridge-name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteBridge(InputStream stream,
                                 @PathParam("ovsdb-ip") String ovsdbIp,
                                 @PathParam("bridge-name") String bridgeName) {
        try {

            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.deleteBridge(ovsdbAddress, bridgeName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addPort(InputStream stream,
                            @PathParam("ovsdb-ip") String ovsdbIp,
                            @PathParam("bridge-name") String bridgeName,
                            @PathParam("port-name") String portName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.addPort(ovsdbAddress, bridgeName, portName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deletePort(InputStream stream,
                               @PathParam("ovsdb-ip") String ovsdbIp,
                               @PathParam("bridge-name") String bridgeName,
                               @PathParam("port-name") String portName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.removePort(ovsdbAddress, bridgeName, portName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/patch_peer/{patch-peer}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createPatchPeerPort(InputStream stream,
                                        @PathParam("ovsdb-ip") String ovsdbIp,
                                        @PathParam("bridge-name") String bridgeName,
                                        @PathParam("port-name") String portName,
                                        @PathParam("patch-peer") String patchPeer) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.createPatchPeerPort(ovsdbAddress, bridgeName, portName, patchPeer);
            return Response.status(200).build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/gre/{local-ip}/{remote-ip}/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addGreTunnel(InputStream stream,
                                 @PathParam("ovsdb-ip") String ovsdbIp,
                                 @PathParam("bridge-name") String bridgeName,
                                 @PathParam("port-name") String portName,
                                 @PathParam("local-ip") String localIp,
                                 @PathParam("remote-ip") String remoteIp,
                                 @PathParam("key") String key) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            IpAddress tunnelLocalIp = IpAddress.valueOf(localIp);
            IpAddress tunnelRemoteIp = IpAddress.valueOf(remoteIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.createGreTunnel(ovsdbAddress, bridgeName, portName, tunnelLocalIp, tunnelRemoteIp, key);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/gre")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteGreTunnel(InputStream stream,
                                    @PathParam("ovsdb-ip") String ovsdbIp,
                                    @PathParam("bridge-name") String bridgeName,
                                    @PathParam("port-name") String portName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbBridgeService ovsdbBridgeService = get(OvsdbBridgeService.class);
            ovsdbBridgeService.deleteGreTunnel(ovsdbAddress, bridgeName, portName);
            return Response.status(200).build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
}
