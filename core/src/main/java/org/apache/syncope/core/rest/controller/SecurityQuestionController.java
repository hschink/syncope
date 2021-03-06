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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.beans.SecurityQuestion;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.data.SecurityQuestionDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SecurityQuestionController extends AbstractTransactionalController<SecurityQuestionTO> {

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SecurityQuestionDataBinder binder;

    @PreAuthorize("isAuthenticated()")
    public List<SecurityQuestionTO> list() {
        List<SecurityQuestionTO> result = new ArrayList<SecurityQuestionTO>();
        for (SecurityQuestion securityQuestion : securityQuestionDAO.findAll()) {
            result.add(binder.getSecurityQuestionTO(securityQuestion));
        }

        return result;
    }

    @PreAuthorize("isAuthenticated()")
    public SecurityQuestionTO read(final Long securityQuestionId) {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(securityQuestionId);
        if (securityQuestion == null) {
            LOG.error("Could not find security question '" + securityQuestionId + "'");

            throw new NotFoundException(String.valueOf(securityQuestionId));
        }

        return binder.getSecurityQuestionTO(securityQuestion);
    }

    @PreAuthorize("hasRole('SECURITY_QUESTION_CREATE')")
    public SecurityQuestionTO create(final SecurityQuestionTO securityQuestionTO) {
        return binder.getSecurityQuestionTO(securityQuestionDAO.save(binder.create(securityQuestionTO)));
    }

    @PreAuthorize("hasRole('SECURITY_QUESTION_UPDATE')")
    public SecurityQuestionTO update(final SecurityQuestionTO securityQuestionTO) {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(securityQuestionTO.getId());
        if (securityQuestion == null) {
            LOG.error("Could not find security question '" + securityQuestionTO.getId() + "'");

            throw new NotFoundException(String.valueOf(securityQuestionTO.getId()));
        }

        binder.update(securityQuestion, securityQuestionTO);
        securityQuestion = securityQuestionDAO.save(securityQuestion);

        return binder.getSecurityQuestionTO(securityQuestion);
    }

    @PreAuthorize("hasRole('SECURITY_QUESTION_DELETE')")
    public SecurityQuestionTO delete(final Long securityQuestionId) {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(securityQuestionId);
        if (securityQuestion == null) {
            LOG.error("Could not find security question '" + securityQuestionId + "'");

            throw new NotFoundException(String.valueOf(securityQuestionId));
        }

        SecurityQuestionTO deleted = binder.getSecurityQuestionTO(securityQuestion);
        securityQuestionDAO.delete(securityQuestionId);
        return deleted;
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    public SecurityQuestionTO read(final String username) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }
        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (user.getSecurityQuestion() == null) {
            LOG.error("Could not find security question for user '" + username + "'");

            throw new NotFoundException("Security question for user " + username);
        }

        return binder.getSecurityQuestionTO(user.getSecurityQuestion());
    }

    @Override
    protected SecurityQuestionTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof SecurityQuestionTO) {
                    id = ((SecurityQuestionTO) args[i]).getId();
                }
            }
        }

        if ((id != null) && !id.equals(0l)) {
            try {
                return binder.getSecurityQuestionTO(securityQuestionDAO.find(id));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
