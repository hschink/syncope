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
package org.apache.syncope.services;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.syncope.client.to.ConfigurationTO;

@Path("configurations")
public interface ConfigurationService {

	@POST
	ConfigurationTO create(final ConfigurationTO configurationTO);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	Response dbExport();

	@DELETE
	@Path("{key}")
	ConfigurationTO delete(@PathParam("key") final String key);

	@GET
	@Path("mailTemplates")
	Set<String> getMailTemplates();

	@GET
	@Path("validators")
	Set<String> getValidators();

	@GET
	List<ConfigurationTO> list();

	@GET
	@Path("{key}")
	ConfigurationTO read(@PathParam("key") final String key);

	@PUT
	@Path("{key}")
	ConfigurationTO update(@PathParam("key") final String key, final ConfigurationTO configurationTO);

}