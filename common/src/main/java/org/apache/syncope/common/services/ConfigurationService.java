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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.wrap.MailTemplate;
import org.apache.syncope.common.wrap.Validator;

/**
 * REST operations for configuration.
 */
@Path("configurations")
public interface ConfigurationService extends JAXRSService {

    /**
     * Exports internal storage content as downloadable XML file.
     *
     * @return internal storage content as downloadable XML file
     */
    @GET
    @Path("stream")
    Response export();

    /**
     * Returns a list of known mail-template names.
     *
     * @return a list of known mail-template names
     */
    @GET
    @Path("mailTemplates")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<MailTemplate> getMailTemplates();

    /**
     * Returns a list of known validator names.
     *
     * @return a list of known validator names
     */
    @GET
    @Path("validators")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<Validator> getValidators();

    /**
     * Returns all configuration parameters.
     *
     * @return all configuration parameters
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ConfTO list();

    /**
     * Returns configuration parameter with matching key.
     *
     * @param key identifier of configuration to be read
     * @return configuration parameter with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    AttributeTO read(@NotNull @PathParam("key") String key);

    /**
     * Creates / updates the configuration parameter with the given key.
     *
     * @param key parameter key
     * @param value parameter value
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void set(@NotNull @PathParam("key") String key, @NotNull AttributeTO value);

    /**
     * Deletes the configuration parameter with matching key.
     *
     * @param key configuration parameter key
     */
    @DELETE
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void delete(@NotNull @PathParam("key") String key);
}
