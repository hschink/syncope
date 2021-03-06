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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.SyncopeConstants;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.util.AttributableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SchemaTest extends AbstractDAOTest {

    @Autowired
    private SchemaDAO schemaDAO;

    @Test
    public void findAll() {
        List<USchema> userList = schemaDAO.findAll(USchema.class);
        assertEquals(15, userList.size());

        List<RSchema> roleList = schemaDAO.findAll(RSchema.class);
        assertEquals(5, roleList.size());
    }

    @Test
    public void findByName() {
        USchema schema = schemaDAO.find("fullname", USchema.class);
        assertNotNull("did not find expected attribute schema", schema);
    }

    @Test
    public void findAttrs() {
        List<RSchema> schemas = schemaDAO.findAll(RSchema.class);
        assertNotNull(schemas);
        assertFalse(schemas.isEmpty());

        for (RSchema schema : schemas) {
            List<RAttr> attrs = schemaDAO.findAttrs(schema, RAttr.class);
            assertNotNull(attrs);
            assertFalse(attrs.isEmpty());
        }
    }

    @Test
    public void save() {
        USchema schema = new USchema();
        schema.setName("secondaryEmail");
        schema.setType(AttributeSchemaType.String);
        schema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);

        schemaDAO.save(schema);

        USchema actual = schemaDAO.find("secondaryEmail", USchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(schema, actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveNonValid() {
        USchema schema = new USchema();
        schema.setName("secondaryEmail");
        schema.setType(AttributeSchemaType.String);
        schema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);
        schema.setUniqueConstraint(true);

        schemaDAO.save(schema);
    }

    @Test
    public void checkForEnumType() {
        RSchema schema = new RSchema();
        schema.setType(AttributeSchemaType.Enum);
        schema.setName("color");

        Exception ex = null;
        try {
            schemaDAO.save(schema);
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);

        schema.setEnumerationValues("red" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "yellow");
        schema.setEnumerationKeys("1" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "2");

        schemaDAO.save(schema);

        RSchema actual = schemaDAO.find(schema.getName(), RSchema.class);
        assertNotNull(actual);
        assertNotNull(actual.getEnumerationKeys());
        assertFalse(actual.getEnumerationKeys().isEmpty());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidSchema() {
        USchema schema = new USchema();
        schema.setName("username");
        schemaDAO.save(schema);
    }

    @Test
    public void delete() {
        USchema fullnam = schemaDAO.find("fullname", USchema.class);

        schemaDAO.delete(fullnam.getName(), AttributableUtil.getInstance(AttributableType.USER));

        USchema actual = schemaDAO.find("fullname", USchema.class);
        assertNull("delete did not work", actual);
    }

    @Test
    public void issueSYNCOPE418() {
        USchema schema = new USchema();
        schema.setName("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            schemaDAO.save(schema);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidName));
        }
    }
}
