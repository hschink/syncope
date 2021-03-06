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
package org.apache.syncope.common.types;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum EntityViolationType {

    Standard(""),
    InvalidAccountPolicy("org.apache.syncope.core.validation.accountpolicy"),
    InvalidConnInstanceLocation("org.apache.syncope.core.validation.conninstance.location"),
    InvalidConnPoolConf("org.apache.syncope.core.validation.conninstance.poolConf"),
    InvalidMapping("org.apache.syncope.core.validation.mapping"),
    InvalidMSchema("org.apache.syncope.core.validation.attrvalue.mSchema"),
    InvalidMDerSchema("org.apache.syncope.core.validation.attrvalue.mDerSchema"),
    InvalidMVirSchema("org.apache.syncope.core.validation.attrvalue.mVirSchema"),
    InvalidName("org.apache.syncope.core.validation.name"),
    InvalidNotification("org.apache.syncope.core.validation.notification"),
    InvalidPassword("org.apache.syncope.core.validation.syncopeuser.password"),
    InvalidPasswordPolicy("org.apache.syncope.core.validation.passwordpolicy"),
    InvalidPolicy("org.apache.syncope.core.validation.policy"),
    InvalidPropagationTask("org.apache.syncope.core.validation.propagationtask"),
    InvalidRSchema("org.apache.syncope.core.validation.attrvalue.rSchema"),
    InvalidRDerSchema("org.apache.syncope.core.validation.attrvalue.rDerSchema"),
    InvalidRVirSchema("org.apache.syncope.core.validation.attrvalue.rVirSchema"),
    InvalidReport("org.apache.syncope.core.validation.report"),
    InvalidResource("org.apache.syncope.core.validation.externalresource"),
    InvalidRoleOwner("org.apache.syncope.core.validation.syncoperole.owner"),
    InvalidSchemaEncrypted("org.apache.syncope.core.validation.schema.encrypted"),
    InvalidSchemaEnum("org.apache.syncope.core.validation.schema.enum"),
    InvalidSchemaMultivalueUnique("org.apache.syncope.core.validation.schema.multivalueUnique"),
    InvalidSchedTask("org.apache.syncope.core.validation.schedtask"),
    InvalidSyncTask("org.apache.syncope.core.validation.synctask"),
    InvalidSyncPolicy("org.apache.syncope.core.validation.syncpolicy"),
    InvalidUSchema("org.apache.syncope.core.validation.attrvalue.uSchema"),
    InvalidUDerSchema("org.apache.syncope.core.validation.attrvalue.derSchema"),
    InvalidUVirSchema("org.apache.syncope.core.validation.attrvalue.uVirSchema"),
    InvalidUsername("org.apache.syncope.core.validation.syncopeuser.username"),
    InvalidValueList("org.apache.syncope.core.validation.attr.valueList"),
    MoreThanOneNonNull("org.apache.syncope.core.validation.attrvalue.moreThanOneNonNull");

    private String message;

    EntityViolationType(final String message) {
        this.message = message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
