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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.types.PolicyType;

@XmlRootElement(name = "abstractPolicy")
@XmlType
@XmlSeeAlso({ AccountPolicyTO.class, PasswordPolicyTO.class, SyncPolicyTO.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class AbstractPolicyTO extends AbstractBaseBean {

    private static final long serialVersionUID = -2903888572649721035L;

    private long id;

    private String description;

    private PolicyType type;

    private final List<String> usedByResources = new ArrayList<String>();

    private final List<Long> usedByRoles = new ArrayList<Long>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(final PolicyType type) {
        this.type = type;
    }

    @XmlElementWrapper(name = "usedByResources")
    @XmlElement(name = "resource")
    @JsonProperty("usedByResources")
    public List<String> getUsedByResources() {
        return usedByResources;
    }

    @XmlElementWrapper(name = "usedByRoles")
    @XmlElement(name = "role")
    @JsonProperty("usedByRoles")
    public List<Long> getUsedByRoles() {
        return usedByRoles;
    }

}
