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
package org.apache.syncope.core.persistence.beans;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.validation.entity.PropagationTaskCheck;
import org.apache.syncope.core.util.POJOHelper;
import org.identityconnectors.framework.common.objects.Attribute;

/**
 * Encapsulate all information about a propagation task.
 */
@Entity
@PropagationTaskCheck
public class PropagationTask extends Task {

    private static final long serialVersionUID = 7086054884614511210L;

    /**
     * @see PropagationMode
     */
    @Enumerated(EnumType.STRING)
    private PropagationMode propagationMode;

    /**
     * @see ResourceOperation
     */
    @Enumerated(EnumType.STRING)
    private ResourceOperation propagationOperation;

    /**
     * The accountId on the external resource.
     */
    private String accountId;

    /**
     * The (optional) former accountId on the external resource.
     */
    private String oldAccountId;

    /**
     * Attributes to be propagated.
     */
    @Lob
    private String xmlAttributes;

    private String objectClassName;

    @Enumerated(EnumType.STRING)
    private AttributableType subjectType;

    private Long subjectId;

    /**
     * ExternalResource to which the propagation happens.
     */
    @ManyToOne
    private ExternalResource resource;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(final String accountId) {
        this.accountId = accountId;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(final String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }

    public Set<Attribute> getAttributes() {
        return StringUtils.isBlank(xmlAttributes)
                ? Collections.<Attribute>emptySet()
                : new HashSet<Attribute>(
                        Arrays.asList(POJOHelper.deserialize(xmlAttributes, Attribute[].class)));
    }

    public void setAttributes(final Set<Attribute> attributes) {
        xmlAttributes = POJOHelper.serialize(attributes);
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(final PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public ResourceOperation getPropagationOperation() {
        return propagationOperation;
    }

    public void setPropagationOperation(final ResourceOperation propagationOperation) {
        this.propagationOperation = propagationOperation;
    }

    public ExternalResource getResource() {
        return resource;
    }

    public void setResource(final ExternalResource resource) {
        this.resource = resource;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(final String objectClassName) {
        this.objectClassName = objectClassName;
    }

    public AttributableType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(final AttributableType subjectType) {
        this.subjectType = subjectType;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(final Long subjectId) {
        this.subjectId = subjectId;
    }
}
