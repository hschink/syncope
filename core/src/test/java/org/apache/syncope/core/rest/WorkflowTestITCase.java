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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.junit.Assume;
import org.junit.Test;

public class WorkflowTestITCase extends AbstractTest {

    @Test
    public void isActivitiEnabled() {
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers(),
                adminClient.isActivitiEnabledFor(SubjectType.USER));
        assertEquals(ActivitiDetector.isActivitiEnabledForRoles(),
                adminClient.isActivitiEnabledFor(SubjectType.ROLE));
    }

    private void exportDefinition(final SubjectType type) throws IOException {
        Response response = workflowService.exportDefinition(type);
        assertTrue(response.getMediaType().toString().
                startsWith(clientFactory.getContentType().getMediaType().toString()));
        assertTrue(response.getEntity() instanceof InputStream);
        String definition = IOUtils.toString((InputStream) response.getEntity());
        assertNotNull(definition);
        assertFalse(definition.isEmpty());
    }

    @Test
    public void exportUserDefinition() throws IOException {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());
        exportDefinition(SubjectType.USER);
    }

    @Test
    public void getRoleDefinition() throws IOException {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForRoles());
        exportDefinition(SubjectType.ROLE);
    }

    private void importDefinition(final SubjectType type) throws IOException {
        Response response = workflowService.exportDefinition(type);
        String definition = IOUtils.toString((InputStream) response.getEntity());

        workflowService.importDefinition(type, definition);
    }

    @Test
    public void updateUserDefinition() throws IOException {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        importDefinition(SubjectType.USER);
    }

    @Test
    public void updateRoleDefinition() throws IOException {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForRoles());

        importDefinition(SubjectType.ROLE);
    }
}
