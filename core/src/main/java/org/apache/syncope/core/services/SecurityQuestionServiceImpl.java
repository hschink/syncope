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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.SecurityQuestionService;
import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.core.rest.controller.SecurityQuestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityQuestionServiceImpl extends AbstractServiceImpl implements SecurityQuestionService {

    @Autowired
    private SecurityQuestionController controller;

    @Override
    public List<SecurityQuestionTO> list() {
        return controller.list();
    }

    @Override
    public SecurityQuestionTO read(final Long securityQuestionId) {
        return controller.read(securityQuestionId);
    }

    @Override
    public Response create(final SecurityQuestionTO securityQuestionTO) {
        SecurityQuestionTO created = controller.create(securityQuestionTO);

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.getId())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, String.valueOf(created.getId())).
                build();
    }

    @Override
    public void update(final Long securityQuestionId, final SecurityQuestionTO securityQuestionTO) {
        securityQuestionTO.setId(securityQuestionId);
        controller.update(securityQuestionTO);
    }

    @Override
    public void delete(final Long securityQuestionId) {
        controller.delete(securityQuestionId);
    }

    @Override
    public SecurityQuestionTO readByUser(final String username) {
        return controller.read(username);
    }

}
