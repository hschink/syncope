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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.reqres.BulkAction;

import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.wrap.PropagationActionClass;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ResourceTestITCase extends AbstractTest {

    private ResourceTO buildResourceTO(final String resourceName) {
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(false);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);
        return resourceTO;
    }

    @Test
    public void getPropagationActionsClasses() {
        List<PropagationActionClass> actions = resourceService.getPropagationActionsClasses();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        String resourceName = RESOURCE_NAME_CREATE;
        ResourceTO resourceTO = buildResourceTO(resourceName);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check for existence
        actual = resourceService.read(resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createOverridingProps() {
        String resourceName = "overriding-conn-conf-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(false);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        resourceTO.setUmapping(mapping);

        ConnConfProperty p = new ConnConfProperty();
        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setType("java.lang.String");
        schema.setName("endpoint");
        schema.setRequired(true);
        p.setSchema(schema);
        p.getValues().add("http://invalidurl/");

        Set<ConnConfProperty> connectorConfigurationProperties = new HashSet<ConnConfProperty>(Arrays.asList(p));
        resourceTO.getConnConfProperties().addAll(connectorConfigurationProperties);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check the existence
        actual = resourceService.read(resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createWithSingleMappingItem() {
        String resourceName = RESOURCE_NAME_CREATE_SINGLE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO umapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setExtAttrName("userId");
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.PROPAGATION);
        umapping.setAccountIdItem(item);

        resourceTO.setUmapping(umapping);

        MappingTO rmapping = new MappingTO();

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.RoleId);
        item.setExtAttrName("roleId");
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.SYNCHRONIZATION);
        rmapping.setAccountIdItem(item);

        resourceTO.setRmapping(rmapping);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getUmapping());
        assertNotNull(actual.getUmapping().getItems());
        assertNotNull(actual.getRmapping());
        assertNotNull(actual.getRmapping().getItems());
        assertEquals(MappingPurpose.SYNCHRONIZATION, actual.getRmapping().getAccountIdItem().getPurpose());
        assertEquals(MappingPurpose.PROPAGATION, actual.getUmapping().getAccountIdItem().getPurpose());
    }

    @Test
    public void createWithInvalidMapping() {
        String resourceName = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setExtAttrName("userId");
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setExtAttrName("email");
        // missing intAttrName ...
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);

        try {
            createResource(resourceTO);
            fail("Create should not have worked");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            assertEquals("intAttrName", e.getElements().iterator().next());
        }
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithoutExtAttr() {
        String resourceName = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setExtAttrName("userId");
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setIntAttrName("usernane");
        // missing extAttrName ...
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);

        createResource(resourceTO);
    }

    @Test
    public void createWithPasswordPolicy() {
        String resourceName = "res-with-password-policy";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);
        resourceTO.setPasswordPolicy(4L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check the existence
        actual = resourceService.read(resourceName);
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void updateWithException() {
        try {
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setName("resourcenotfound");

            resourceService.update(resourceTO.getName(), resourceTO);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void update() {
        String resourceName = RESOURCE_NAME_UPDATE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(101L);

        MappingTO mapping = new MappingTO();

        // Update with an existing and already assigned mapping
        MappingItemTO item = new MappingItemTO();
        item.setId(112L);
        item.setExtAttrName("test3");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        // Update defining new mappings
        for (int i = 4; i < 6; i++) {
            item = new MappingItemTO();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("fullname");
            item.setIntMappingType(IntMappingType.UserSchema);
            item.setPurpose(MappingPurpose.BOTH);
            mapping.addItem(item);
        }
        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        resourceService.update(resourceTO.getName(), resourceTO);
        ResourceTO actual = resourceService.read(resourceTO.getName());
        assertNotNull(actual);

        // check for existence
        Collection<MappingItemTO> mapItems = actual.getUmapping().getItems();
        assertNotNull(mapItems);
        assertEquals(4, mapItems.size());
    }

    @Test
    public void deleteWithException() {
        try {
            resourceService.delete("resourcenotfound");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void updateResetSyncToken() {
        // create resource with sync token
        String resourceName = RESOURCE_NAME_RESETSYNCTOKEN + getUUIDString();
        ResourceTO pre = buildResourceTO(resourceName);
        pre.setUsyncToken("test");
        resourceService.create(pre);

        pre.setUsyncToken(null);
        resourceService.update(pre.getName(), pre);
        ResourceTO actual = resourceService.read(pre.getName());
        // check that the synctoken has been reset
        assertNull(actual.getUsyncToken());
    }

    @Test
    public void delete() {
        String resourceName = "tobedeleted";

        ResourceTO resource = buildResourceTO(resourceName);
        Response response = resourceService.create(resource);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        resourceService.delete(resourceName);

        try {
            resourceService.read(resourceName);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        List<ResourceTO> actuals = resourceService.list();
        assertNotNull(actuals);
        assertFalse(actuals.isEmpty());
        for (ResourceTO resourceTO : actuals) {
            assertNotNull(resourceTO);
        }
    }

    @Test
    public void listByType() {
        List<ResourceTO> actuals = resourceService.list(105L);
        assertNotNull(actuals);

        for (ResourceTO resourceTO : actuals) {
            assertNotNull(resourceTO);
            assertEquals(105L, resourceTO.getConnectorId().longValue());
        }
    }

    @Test
    public void read() {
        ResourceTO actual = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(actual);
    }

    @Test
    public void issueSYNCOPE323() {
        ResourceTO actual = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(actual);

        try {
            createResource(actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setName(null);
        try {
            createResource(actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void bulkAction() {
        resourceService.create(buildResourceTO("forBulk1"));
        resourceService.create(buildResourceTO("forBulk2"));

        assertNotNull(resourceService.read("forBulk1"));
        assertNotNull(resourceService.read("forBulk2"));

        final BulkAction bulkAction = new BulkAction();
        bulkAction.setOperation(BulkAction.Type.DELETE);

        bulkAction.getTargets().add(String.valueOf("forBulk1"));
        bulkAction.getTargets().add(String.valueOf("forBulk2"));

        resourceService.bulk(bulkAction);

        try {
            resourceService.read("forBulk1");
            fail();
        } catch (SyncopeClientException e) {
        }

        try {
            resourceService.read("forBulk2");
            fail();
        } catch (SyncopeClientException e) {
        }
    }

    @Test
    public void issueSYNCOPE360() {
        final String name = "SYNCOPE360-" + getUUIDString();
        resourceService.create(buildResourceTO(name));

        ResourceTO resource = resourceService.read(name);
        assertNotNull(resource);
        assertNotNull(resource.getUmapping());

        resource.setUmapping(new MappingTO());
        resourceService.update(name, resource);

        resource = resourceService.read(name);
        assertNotNull(resource);
        assertNull(resource.getUmapping());
    }

    @Test
    public void issueSYNCOPE368() {
        final String name = "SYNCOPE368-" + getUUIDString();

        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(name);
        resourceTO.setConnectorId(105L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.RoleName);
        item.setExtAttrName("cn");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);
            
        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.RoleOwnerSchema);
        item.setExtAttrName("owner");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        resourceTO.setRmapping(mapping);

        resourceTO = createResource(resourceTO);
        assertNotNull(resourceTO);
        assertEquals(2, resourceTO.getRmapping().getItems().size());
    }

    @Test
    public void issueSYNCOPE418() {
        try {
            resourceService.create(
                    buildResourceTO("http://schemas.examples.org/security/authorization/organizationUnit"));
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidExternalResource, e.getType());

            assertTrue(e.getElements().iterator().next().toString().contains(EntityViolationType.InvalidName.name()));
        }
    }

    @Test
    public void anonymous() {
        ResourceService unauthenticated = clientFactory.createAnonymous().getService(ResourceService.class);
        try {
            unauthenticated.list();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        ResourceService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).
                getService(ResourceService.class);
        assertFalse(anonymous.list().isEmpty());
    }

    @Test
    public void issueSYNCOPE493() {
        // create resource with attribute mapping set to NONE and check its propagation
        String resourceName = RESOURCE_NAME_CREATE_NONE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO umapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setExtAttrName("userId");
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.PROPAGATION);
        umapping.setAccountIdItem(item);

        MappingItemTO item2 = new MappingItemTO();
        item2.setIntMappingType(IntMappingType.UserSchema);
        item2.setAccountid(false);
        item2.setIntAttrName("gender");
        item2.setExtAttrName("gender");
        item2.setPurpose(MappingPurpose.NONE);
        umapping.addItem(item2);

        resourceTO.setUmapping(umapping);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getUmapping());
        assertNotNull(actual.getUmapping().getItems());
        assertEquals(MappingPurpose.PROPAGATION, actual.getUmapping().getAccountIdItem().getPurpose());
        for (MappingItemTO itemTO : actual.getUmapping().getItems()) {
            if ("gender".equals(itemTO.getIntAttrName())) {
                assertEquals(MappingPurpose.NONE, itemTO.getPurpose());
            }
        }
    }
}
