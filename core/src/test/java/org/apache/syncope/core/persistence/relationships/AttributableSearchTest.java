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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.util.EntitlementUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:coreContext.xml",
    "classpath:persistenceContext.xml",
    "classpath:schedulingContext.xml",
    "classpath:workflowContext.xml"
})
@Transactional
public class AttributableSearchTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SubjectSearchDAO searchDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public void issueSYNCOPE95() {
        Set<SyncopeRole> roles = new HashSet<SyncopeRole>(roleDAO.findAll());
        for (SyncopeRole role : roles) {
            roleDAO.delete(role.getId());
        }
        roleDAO.flush();

        final AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        final SearchCond cond = SearchCond.getLeafCond(coolLeafCond);
        assertTrue(cond.isValid());

        final List<SyncopeUser> users =
                searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getId());
    }
}
