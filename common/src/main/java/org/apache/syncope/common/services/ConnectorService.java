/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.services;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;

import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnIdObjectClassTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfProperty;

/**
 * REST operations for connector bundles and instances.
 */
@Path("connectors")
public interface ConnectorService extends JAXRSService {

    /**
     * Returns available connector bundles with property keys in selected language.
     *
     * @param lang language to select property keys; default language is English
     * @return available connector bundles with property keys in selected language
     */
    @GET
    @Path("bundles")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ConnBundleTO> getBundles(@QueryParam("lang") String lang);

    /**
     * Returns configuration for given connector instance.
     *
     * @param connInstanceId connector instance id to read configuration from
     * @return configuration for given connector instance
     */
    @GET
    @Path("{connInstanceId}/configuration")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ConnConfProperty> getConfigurationProperties(@NotNull @PathParam("connInstanceId") Long connInstanceId);

    /**
     * Returns schema names for connector bundle matching the given connector instance id.
     *
     * @param connInstanceId connector instance id to be used for schema lookup
     * @param connInstanceTO connector instance object to provide special configuration properties
     * @param includeSpecial if set to true, special schema names (like '__PASSWORD__') will be included;
     * default is false
     * @return schema names for connector bundle matching the given connector instance id
     */
    @POST
    @Path("{connInstanceId}/schemaNames")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<SchemaTO> getSchemaNames(@NotNull @PathParam("connInstanceId") Long connInstanceId,
            @NotNull ConnInstanceTO connInstanceTO,
            @QueryParam("includeSpecial") @DefaultValue("false") boolean includeSpecial);

    /**
     * Returns supported object classes for connector bundle matching the given connector instance id.
     *
     * @param connInstanceId connector instance id to be used for schema lookup
     * @param connInstanceTO connector instance object to provide special configuration properties
     * @return supported object classes for connector bundle matching the given connector instance id
     */
    @POST
    @Path("{connInstanceId}/supportedObjectClasses")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ConnIdObjectClassTO> getSupportedObjectClasses(
            @NotNull @PathParam("connInstanceId") Long connInstanceId,
            @NotNull ConnInstanceTO connInstanceTO);

    /**
     * Returns connector instance with matching id.
     *
     * @param connInstanceId connector instance id to be read
     * @return connector instance with matching id
     */
    @GET
    @Path("{connInstanceId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ConnInstanceTO read(@NotNull @PathParam("connInstanceId") Long connInstanceId);

    /**
     * Returns connector instance for matching resource.
     *
     * @param resourceName resource name to be used for connector lookup
     * @return connector instance for matching resource
     */
    @GET
    @Path("byResource/{resourceName}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ConnInstanceTO readByResource(@NotNull @PathParam("resourceName") String resourceName);

    /**
     * Returns a list of all connector instances with property keys in the matching language.
     *
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return list of all connector instances with property keys in the matching language
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ConnInstanceTO> list(@QueryParam("lang") String lang);

    /**
     * Creates a new connector instance.
     *
     * @param connInstanceTO connector instance to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created connector instance
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created connector instance")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Updates the connector instance matching the provided id.
     *
     * @param connInstanceId connector instance id to be updated
     * @param connInstaceTO connector instance to be stored
     */
    @PUT
    @Path("{connInstanceId}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull @PathParam("connInstanceId") Long connInstanceId, @NotNull ConnInstanceTO connInstaceTO);

    /**
     * Deletes the connector instance matching the provided id.
     *
     * @param connInstanceId connector instance id to be deleted
     */
    @DELETE
    @Path("{connInstanceId}")
    void delete(@NotNull @PathParam("connInstanceId") Long connInstanceId);

    /**
     * @param connInstaceTO connector instance to be used for connection check
     * @return true if connection could be established
     */
    @POST
    @Path("check")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    boolean check(@NotNull ConnInstanceTO connInstaceTO);

    /**
     * Reload all connector bundles and instances.
     */
    @POST
    @Path("reload")
    void reload();

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of connector instance ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}
