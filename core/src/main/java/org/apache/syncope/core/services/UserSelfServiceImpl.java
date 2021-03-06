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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.UserSelfService;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSelfServiceImpl extends AbstractServiceImpl implements UserSelfService {

    @Autowired
    private UserController controller;

    @Override
    public Response getOptions() {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.SELFREG_ALLOWED, controller.isSelfRegAllowed()).
                header(RESTHeaders.PWDRESET_ALLOWED, controller.isPwdResetAllowed()).
                header(RESTHeaders.PWDRESET_NEEDS_SECURITYQUESTIONS, controller.isPwdResetRequiringSecurityQuestions()).
                build();
    }

    @Override
    public Response create(final UserTO userTO, final boolean storePassword) {
        if (!controller.isSelfRegAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("Self registration forbidden by configuration");
            throw sce;
        }

        UserTO created = controller.createSelf(userTO, storePassword);
        return createResponse(created.getId(), created);
    }

    @Override
    public UserTO read() {
        return controller.readSelf();
    }

    @Override
    public Response update(final Long userId, final UserMod userMod) {
        userMod.setId(userId);
        UserTO updated = controller.updateSelf(userMod);
        return modificationResponse(updated);
    }

    @Override
    public Response delete() {
        UserTO deleted = controller.deleteSelf();
        return modificationResponse(deleted);
    }

    @Override
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (!controller.isPwdResetAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        controller.requestPasswordReset(username, securityAnswer);
    }

    @Override
    public void confirmPasswordReset(final String token, final String password) {
        if (!controller.isPwdResetAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        controller.confirmPasswordReset(token, password);
    }

}
