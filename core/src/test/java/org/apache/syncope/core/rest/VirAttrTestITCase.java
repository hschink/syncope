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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.SubjectType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class VirAttrTestITCase extends AbstractTest {

    @Test
    public void issueSYNCOPE16() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("issue16@apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.getMemberships().add(membershipTO);

        // 1. create user
        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertEquals("virtualvalue", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());
        userMod.getVirAttrsToRemove().add("virtualdata");
        userMod.getVirAttrsToUpdate().add(attributeMod("virtualdata", "virtualupdated"));

        // 3. update virtual attribute
        actual = updateUser(userMod);
        assertNotNull(actual);

        // 4. check for virtual attribute value
        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE260() {
        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserTO userTO = UserTestITCase.getUniqueSampleTO("260@a.com");
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_WS2, SubjectType.USER, userTO.getId());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue", connObjectTO.getAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // update user virtual attribute and check virtual attribute value update propagation
        // ----------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        AttributeMod attrMod = new AttributeMod();
        attrMod.setSchema("virtualdata");
        attrMod.getValuesToBeRemoved().add("virtualvalue");
        attrMod.getValuesToBeAdded().add("virtualvalue2");

        userMod.getVirAttrsToUpdate().add(attrMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals("ws-target-resource-2", userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_WS2, SubjectType.USER, userTO.getId());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue2", connObjectTO.getAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // suspend/reactivate user and check virtual attribute value (unchanged)
        // ----------------------------------
        StatusMod statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.SUSPEND);
        userTO = userService.status(userTO.getId(), statusMod).readEntity(UserTO.class);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_WS2, SubjectType.USER, userTO.getId());
        assertNotNull(connObjectTO);
        assertFalse(connObjectTO.getAttrMap().get("NAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getAttrMap().get("NAME").getValues().get(0));

        statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(userTO.getId(), statusMod).readEntity(UserTO.class);
        assertEquals("active", userTO.getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_WS2, SubjectType.USER, userTO.getId());
        assertNotNull(connObjectTO);
        assertFalse(connObjectTO.getAttrMap().get("NAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // update user attribute and check virtual attribute value (unchanged)
        // ----------------------------------
        userMod = new UserMod();
        userMod.setId(userTO.getId());

        attrMod = new AttributeMod();
        attrMod.setSchema("surname");
        attrMod.getValuesToBeRemoved().add("Surname");
        attrMod.getValuesToBeAdded().add("Surname2");

        userMod.getAttrsToUpdate().add(attrMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_WS2, SubjectType.USER, userTO.getId());
        assertNotNull(connObjectTO);
        assertEquals("Surname2", connObjectTO.getAttrMap().get("SURNAME").getValues().get(0));

        // attribute "name" mapped on virtual attribute "virtualdata" shouldn't be changed
        assertFalse(connObjectTO.getAttrMap().get("NAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // remove user virtual attribute and check virtual attribute value (reset)
        // ----------------------------------
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getVirAttrsToRemove().add("virtualdata");

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertTrue(userTO.getVirAttrs().isEmpty());
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_WS2, SubjectType.USER, userTO.getId());
        assertNotNull(connObjectTO);

        // attribute "name" mapped on virtual attribute "virtualdata" should be reset
        assertTrue(connObjectTO.getAttrMap().get("NAME").getValues() == null
                || connObjectTO.getAttrMap().get("NAME").getValues().isEmpty());
        // ----------------------------------
    }

    @Test
    public void virAttrCache() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("virattrcache@apache.org");
        userTO.getVirAttrs().clear();

        AttributeTO virAttrTO = new AttributeTO();
        virAttrTO.setSchema("virtualdata");
        virAttrTO.getValues().add("virattrcache");
        userTO.getVirAttrs().add(virAttrTO);

        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // 1. create user
        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getId());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // 3. update virtual attribute directly
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getId());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testsync set USERNAME='virattrcache2' WHERE ID=?", actual.getId());

        value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getId());
        assertEquals("virattrcache2", value);

        // 4. check for cached attribute value
        actual = userService.read(actual.getId());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        AttributeMod virtualdata = new AttributeMod();
        virtualdata.setSchema("virtualdata");
        virtualdata.getValuesToBeAdded().add("virtualupdated");

        userMod.getVirAttrsToRemove().add("virtualdata");
        userMod.getVirAttrsToUpdate().add(virtualdata);

        // 5. update virtual attribute
        actual = updateUser(userMod);
        assertNotNull(actual);

        // 6. check for virtual attribute value
        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE397() {
        ResourceTO csv = resourceService.read(RESOURCE_NAME_CSV);
        final MappingTO origMapping = SerializationUtils.clone(csv.getUmapping());
        try {
            // change mapping of resource-csv
            assertNotNull(origMapping);
            for (MappingItemTO item : csv.getUmapping().getItems()) {
                if ("email".equals(item.getIntAttrName())) {
                    // unset internal attribute mail and set virtual attribute virtualdata as mapped to external email
                    item.setIntMappingType(IntMappingType.UserVirtualSchema);
                    item.setIntAttrName("virtualdata");
                    item.setPurpose(MappingPurpose.BOTH);
                    item.setExtAttrName("email");
                }
            }

            resourceService.update(csv.getName(), csv);
            csv = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(csv.getUmapping());

            boolean found = false;
            for (MappingItemTO item : csv.getUmapping().getItems()) {
                if ("email".equals(item.getExtAttrName()) && "virtualdata".equals(item.getIntAttrName())) {
                    found = true;
                }
            }

            assertTrue(found);

            // create a new user
            UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope397@syncope.apache.org");
            userTO.getResources().clear();
            userTO.getMemberships().clear();
            userTO.getDerAttrs().clear();
            userTO.getVirAttrs().clear();

            userTO.getDerAttrs().add(attributeTO("csvuserid", null));
            userTO.getDerAttrs().add(attributeTO("cn", null));
            userTO.getVirAttrs().add(attributeTO("virtualdata", "test@testone.org"));
            // assign resource-csv to user
            userTO.getResources().add(RESOURCE_NAME_CSV);
            // save user
            UserTO created = createUser(userTO);
            // make std controls about user
            assertNotNull(created);
            assertTrue(RESOURCE_NAME_CSV.equals(created.getResources().iterator().next()));
            // update user
            UserTO toBeUpdated = userService.read(created.getId());
            UserMod userMod = new UserMod();
            userMod.setId(toBeUpdated.getId());
            userMod.setPassword("password2");
            // assign new resource to user
            userMod.getResourcesToAdd().add(RESOURCE_NAME_WS2);
            //modify virtual attribute
            userMod.getVirAttrsToRemove().add("virtualdata");
            userMod.getVirAttrsToUpdate().add(attributeMod("virtualdata", "test@testoneone.com"));

            // check Syncope change password
            StatusMod pwdPropRequest = new StatusMod();
            pwdPropRequest.setOnSyncope(true);
            pwdPropRequest.getResourceNames().add(RESOURCE_NAME_WS2);
            userMod.setPwdPropRequest(pwdPropRequest);

            toBeUpdated = updateUser(userMod);
            assertNotNull(toBeUpdated);
            assertEquals("test@testoneone.com", toBeUpdated.getVirAttrs().get(0).getValues().get(0));
            // check if propagates correctly with assertEquals on size of tasks list
            assertEquals(2, toBeUpdated.getPropagationStatusTOs().size());
        } finally {
            // restore mapping of resource-csv
            csv.setUmapping(origMapping);
            resourceService.update(csv.getName(), csv);
        }
    }

    @Test
    public void issueSYNCOPE442() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope442@apache.org");
        userTO.getVirAttrs().clear();

        AttributeTO virAttrTO = new AttributeTO();
        virAttrTO.setSchema("virtualdata");
        virAttrTO.getValues().add("virattrcache");
        userTO.getVirAttrs().add(virAttrTO);

        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // 1. create user
        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getId());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------
        // 3. force cache expiring without any modification
        // ----------------------------------------
        String jdbcURL = null;
        ConnInstanceTO connInstanceBean = connectorService.readByResource(RESOURCE_NAME_DBVIRATTR);
        for (ConnConfProperty prop : connInstanceBean.getConfiguration()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                jdbcURL = prop.getValues().iterator().next().toString();
                prop.getValues().clear();
                prop.getValues().add("jdbc:h2:tcp://localhost:9092/xxx");
            }
        }

        connectorService.update(connInstanceBean.getId(), connInstanceBean);

        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        AttributeMod virtualdata = new AttributeMod();
        virtualdata.setSchema("virtualdata");
        virtualdata.getValuesToBeAdded().add("virtualupdated");

        userMod.getVirAttrsToRemove().add("virtualdata");
        userMod.getVirAttrsToUpdate().add(virtualdata);

        actual = updateUser(userMod);
        assertNotNull(actual);
        // ----------------------------------------

        // ----------------------------------------
        // 4. update virtual attribute
        // ----------------------------------------
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getId());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testsync set USERNAME='virattrcache2' WHERE ID=?", actual.getId());

        value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getId());
        assertEquals("virattrcache2", value);
        // ----------------------------------------

        actual = userService.read(actual.getId());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------
        // 5. restore connector
        // ----------------------------------------
        for (ConnConfProperty prop : connInstanceBean.getConfiguration()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                prop.getValues().clear();
                prop.getValues().add(jdbcURL);
            }
        }

        connectorService.update(connInstanceBean.getId(), connInstanceBean);
        // ----------------------------------------

        actual = userService.read(actual.getId());
        assertEquals("virattrcache2", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE436() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope436@syncope.apache.org");
        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO.getVirAttrs().add(attributeTO("virtualReadOnly", "readOnly"));
        userTO = createUser(userTO);
        //Finding no values because the virtual attribute is readonly 
        assertTrue(userTO.getVirAttrMap().get("virtualReadOnly").getValues().isEmpty());
    }

    @Test
    public void issueSYNCOPE453() {
        final String resourceName = "issueSYNCOPE453-Res-" + getUUIDString();
        final String roleName = "issueSYNCOPE453-Role-" + getUUIDString();

        // -------------------------------------------
        // Create a resource ad-hoc
        // -------------------------------------------
        final ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(107L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("aLong");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setExtAttrName(roleName);
        item.setPurpose(MappingPurpose.PROPAGATION);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("USERNAME");
        item.setIntAttrName("username");
        item.setIntMappingType(IntMappingType.Username);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.getItems().add(item);

        item = new MappingItemTO();
        item.setExtAttrName("EMAIL");
        item.setIntAttrName("rvirtualdata");
        item.setIntMappingType(IntMappingType.RoleVirtualSchema);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.getItems().add(item);

        resourceTO.setUmapping(mapping);
        assertNotNull(getObject(
                resourceService.create(resourceTO).getLocation(), ResourceService.class, ResourceTO.class));
        // -------------------------------------------

        // -------------------------------------------
        // Create a role ad-hoc
        // -------------------------------------------
        RoleTO roleTO = new RoleTO();
        roleTO.setName(roleName);
        roleTO.setParent(8L);
        roleTO.getRVirAttrTemplates().add("rvirtualdata");
        roleTO.getVirAttrs().add(attributeTO("rvirtualdata", "ml@role.it"));
        roleTO.getResources().add(RESOURCE_NAME_LDAP);
        roleTO = createRole(roleTO);
        assertEquals(1, roleTO.getVirAttrs().size());
        assertEquals("ml@role.it", roleTO.getVirAttrs().get(0).getValues().get(0));
        // -------------------------------------------

        // -------------------------------------------
        // Create new user
        // -------------------------------------------
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope453@syncope.apache.org");
        userTO.getAttrs().add(attributeTO("aLong", "123"));
        userTO.getResources().clear();
        userTO.getResources().add(resourceName);
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().clear();
        userTO.getMemberships().clear();

        final MembershipTO membership = new MembershipTO();
        membership.setRoleId(roleTO.getId());
        membership.getVirAttrs().add(attributeTO("mvirtualdata", "mvirtualvalue"));
        userTO.getMemberships().add(membership);

        userTO = createUser(userTO);
        assertEquals(2, userTO.getPropagationStatusTOs().size());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
        assertTrue(userTO.getPropagationStatusTOs().get(1).getStatus().isSuccessful());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        final Map<String, Object> actuals = jdbcTemplate.queryForMap(
                "SELECT id, surname, email FROM testsync WHERE id=?",
                new Object[] { Integer.parseInt(userTO.getAttrMap().get("aLong").getValues().get(0)) });

        assertEquals(userTO.getAttrMap().get("aLong").getValues().get(0), actuals.get("id").toString());
        assertEquals("ml@role.it", actuals.get("email"));
        // -------------------------------------------

        // -------------------------------------------
        // Delete resource and role ad-hoc
        // -------------------------------------------
        resourceService.delete(resourceName);
        roleService.delete(roleTO.getId());
        // -------------------------------------------
    }

    @Test
    public void issueSYNCOPE459() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope459@apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();

        final AttributeTO virtualReadOnly = attributeTO("virtualReadOnly", "");
        virtualReadOnly.getValues().clear();

        userTO.getVirAttrs().add(virtualReadOnly);

        userTO = createUser(userTO);

        assertNotNull(userTO.getVirAttrMap().get("virtualReadOnly"));

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        AttributeMod virtualdata = new AttributeMod();
        virtualdata.setSchema("virtualdata");

        userMod.getVirAttrsToUpdate().add(virtualdata);

        userTO = updateUser(userMod);
        assertNotNull(userTO.getVirAttrMap().get("virtualdata"));
    }

    @Test
    public void issueSYNCOPE458() {
        // -------------------------------------------
        // Create a role ad-hoc
        // -------------------------------------------
        final String roleName = "issueSYNCOPE458-Role-" + getUUIDString();
        RoleTO roleTO = new RoleTO();
        roleTO.setName(roleName);
        roleTO.setParent(2L);
        roleTO.setInheritTemplates(true);
        roleTO = createRole(roleTO);
        // -------------------------------------------

        // -------------------------------------------
        // Update resource-db-virattr mapping adding new membership virtual schema mapping
        // -------------------------------------------
        ResourceTO resourceDBVirAttr = resourceService.read(RESOURCE_NAME_DBVIRATTR);
        assertNotNull(resourceDBVirAttr);

        final MappingTO resourceUMapping = resourceDBVirAttr.getUmapping();

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("mvirtualdata");
        item.setIntMappingType(IntMappingType.MembershipVirtualSchema);
        item.setExtAttrName("EMAIL");
        item.setPurpose(MappingPurpose.BOTH);

        resourceUMapping.addItem(item);

        resourceDBVirAttr.setUmapping(resourceUMapping);

        resourceService.update(RESOURCE_NAME_DBVIRATTR, resourceDBVirAttr);
        // -------------------------------------------

        // -------------------------------------------
        // Create new user
        // -------------------------------------------
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope458@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().clear();
        userTO.getMemberships().clear();

        // add membership, with virtual attribute populated, to user
        MembershipTO membership = new MembershipTO();
        membership.setRoleId(roleTO.getId());
        membership.getVirAttrs().add(attributeTO("mvirtualdata", "syncope458@syncope.apache.org"));
        userTO.getMemberships().add(membership);

        //propagate user
        userTO = createUser(userTO);
        assertEquals(1, userTO.getPropagationStatusTOs().size());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
       // -------------------------------------------

        // 1. check if membership has virtual attribute populated
        assertNotNull(userTO.getMemberships().get(0).getVirAttrMap().get("mvirtualdata"));
        assertEquals("syncope458@syncope.apache.org",
                userTO.getMemberships().get(0).getVirAttrMap().get("mvirtualdata").getValues().get(0));
        // -------------------------------------------

        // 2. update membership virtual attribute
        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(roleTO.getId());
        membershipMod.getVirAttrsToUpdate().add(attributeMod("mvirtualdata", "syncope458_NEW@syncope.apache.org"));

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getMembershipsToRemove().add(userTO.getMemberships().iterator().next().getId());

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        // 3. check again after update if membership has virtual attribute populated with new value
        assertNotNull(userTO.getMemberships().get(0).getVirAttrMap().get("mvirtualdata"));
        assertEquals("syncope458_NEW@syncope.apache.org", userTO.getMemberships().get(0).getVirAttrMap().get(
                "mvirtualdata").getValues().get(0));

        // ----------------------------------------
        // force cache expiring without any modification
        // ----------------------------------------
        String jdbcURL = null;
        ConnInstanceTO connInstanceBean = connectorService.readByResource(RESOURCE_NAME_DBVIRATTR);
        for (ConnConfProperty prop : connInstanceBean.getConfiguration()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                jdbcURL = prop.getValues().iterator().next().toString();
                prop.getValues().clear();
                prop.getValues().add("jdbc:h2:tcp://localhost:9092/xxx");
            }
        }

        connectorService.update(connInstanceBean.getId(), connInstanceBean);

        membershipMod = new MembershipMod();
        membershipMod.setRole(roleTO.getId());
        membershipMod.getVirAttrsToUpdate().add(attributeMod("mvirtualdata", "syncope458_updated@syncope.apache.org"));

        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getMembershipsToRemove().add(userTO.getMemberships().iterator().next().getId());

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        // ----------------------------------

        // change attribute value directly on resource
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT EMAIL FROM testsync WHERE ID=?", String.class, userTO.getId());
        assertEquals("syncope458_NEW@syncope.apache.org", value);

        jdbcTemplate.update("UPDATE testsync set EMAIL='syncope458_NEW_TWO@syncope.apache.org' WHERE ID=?", userTO.
                getId());

        value = jdbcTemplate.queryForObject("SELECT EMAIL FROM testsync WHERE ID=?", String.class, userTO.getId());
        assertEquals("syncope458_NEW_TWO@syncope.apache.org", value);
        // ----------------------------------------

        // ----------------------------------------
        // restore connector
        // ----------------------------------------
        for (ConnConfProperty prop : connInstanceBean.getConfiguration()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                prop.getValues().clear();
                prop.getValues().add(jdbcURL);
            }
        }
        connectorService.update(connInstanceBean.getId(), connInstanceBean);
        // ----------------------------------------

        userTO = userService.read(userTO.getId());
        assertNotNull(userTO);
        // 4. check virtual attribute synchronization after direct update on resource
        assertEquals("syncope458_NEW_TWO@syncope.apache.org", userTO.getMemberships().get(0).getVirAttrMap().get(
                "mvirtualdata").getValues().get(0));

        // 5. remove membership virtual attribute
        membershipMod = new MembershipMod();
        membershipMod.setRole(roleTO.getId());
        membershipMod.getVirAttrsToRemove().add("mvirtualdata");

        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getMembershipsToRemove().add(userTO.getMemberships().iterator().next().getId());

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        // check again after update if membership hasn't any virtual attribute
        assertTrue(userTO.getMemberships().get(0).getVirAttrMap().isEmpty());

        // -------------------------------------------
        // Delete role ad-hoc and restore resource mapping
        // -------------------------------------------
        roleService.delete(roleTO.getId());

        resourceUMapping.removeItem(item);
        resourceDBVirAttr.setUmapping(resourceUMapping);
        resourceService.update(RESOURCE_NAME_DBVIRATTR, resourceDBVirAttr);
        // -------------------------------------------
    }

    @Test
    public void issueSYNCOPE501() {
        // PHASE 1: update only user virtual attributes

        // 1. create user and propagate him on resource-db-virattr
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope501@apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();

        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // virtualdata is mapped with username
        final AttributeTO virtualData = attributeTO("virtualdata", "syncope501@apache.org");
        userTO.getVirAttrs().add(virtualData);

        userTO = createUser(userTO);

        assertNotNull(userTO.getVirAttrMap().get("virtualdata"));
        assertEquals("syncope501@apache.org", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // 2. update virtual attribute
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        final StatusMod statusMod = new StatusMod();
        statusMod.getResourceNames().addAll(Collections.<String>emptySet());
        statusMod.setOnSyncope(false);

        userMod.setPwdPropRequest(statusMod);
        // change virtual attribute value
        final AttributeMod virtualDataMod = new AttributeMod();
        virtualDataMod.setSchema("virtualdata");
        virtualDataMod.getValuesToBeAdded().add("syncope501_updated@apache.org");
        virtualDataMod.getValuesToBeRemoved().add("syncope501@apache.org");
        userMod.getVirAttrsToUpdate().add(virtualDataMod);
        userMod.getVirAttrsToRemove().add("virtualdata");

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        // 3. check that user virtual attribute has really been updated 
        assertFalse(userTO.getVirAttrMap().get("virtualdata").getValues().isEmpty());
        assertEquals("syncope501_updated@apache.org", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------------------------
        // PHASE 2: update only membership virtual attributes
        // -------------------------------------------
        // Update resource-db-virattr mapping adding new membership virtual schema mapping
        // -------------------------------------------
        ResourceTO resourceDBVirAttr = resourceService.read(RESOURCE_NAME_DBVIRATTR);
        assertNotNull(resourceDBVirAttr);

        final MappingTO resourceUMapping = resourceDBVirAttr.getUmapping();

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("mvirtualdata");
        item.setIntMappingType(IntMappingType.MembershipVirtualSchema);
        item.setExtAttrName("EMAIL");
        item.setPurpose(MappingPurpose.BOTH);

        resourceUMapping.addItem(item);

        resourceDBVirAttr.setUmapping(resourceUMapping);

        resourceService.update(RESOURCE_NAME_DBVIRATTR, resourceDBVirAttr);
        // -------------------------------------------

        // -------------------------------------------
        // Create a role ad-hoc
        // -------------------------------------------
        final String roleName = "issueSYNCOPE501-Role-" + getUUIDString();
        RoleTO roleTO = new RoleTO();
        roleTO.setName(roleName);
        roleTO.setParent(2L);
        roleTO.setInheritTemplates(true);
        roleTO = createRole(roleTO);
        // -------------------------------------------

        // 1. add membership, with virtual attribute populated, to user
        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(roleTO.getId());
        membershipMod.getVirAttrsToUpdate().add(attributeMod("mvirtualdata", "syncope501membership@test.org"));

        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.setPwdPropRequest(statusMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertEquals("syncope501membership@test.org",
                userTO.getMemberships().get(0).getVirAttrMap().get("mvirtualdata").getValues().get(0));

        // 2. update only membership virtual attribute and propagate user
        membershipMod = new MembershipMod();
        membershipMod.setRole(roleTO.getId());
        membershipMod.getVirAttrsToUpdate().add(attributeMod("mvirtualdata",
                "syncope501membership_updated@test.org"));
        membershipMod.getVirAttrsToRemove().add("syncope501membership@test.org");

        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getMembershipsToRemove().add(userTO.getMemberships().iterator().next().getId());
        userMod.setPwdPropRequest(statusMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        // 3. check if change has been propagated
        assertEquals("syncope501membership_updated@test.org", userTO.getMemberships().get(0).getVirAttrMap().
                get("mvirtualdata").getValues().get(0));

        // 4. delete membership and check on resource attribute deletion
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToRemove().add(userTO.getMemberships().get(0).getId());
        userMod.setPwdPropRequest(statusMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertTrue(userTO.getMemberships().isEmpty());

        // read attribute value directly on resource
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        final String emailValue = jdbcTemplate.queryForObject(
                "SELECT EMAIL FROM testsync WHERE ID=?", String.class, userTO.getId());
        assertTrue(StringUtils.isBlank(emailValue));
        // ----------------------------------------

        // -------------------------------------------
        // Delete role ad-hoc and restore resource mapping
        // -------------------------------------------
        roleService.delete(roleTO.getId());

        resourceUMapping.removeItem(item);
        resourceDBVirAttr.setUmapping(resourceUMapping);
        resourceService.update(RESOURCE_NAME_DBVIRATTR, resourceDBVirAttr);
        // -------------------------------------------
    }
}
