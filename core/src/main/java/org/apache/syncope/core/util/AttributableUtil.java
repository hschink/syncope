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
package org.apache.syncope.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.conf.CAttr;
import org.apache.syncope.core.persistence.beans.conf.CAttrUniqueValue;
import org.apache.syncope.core.persistence.beans.conf.CAttrValue;
import org.apache.syncope.core.persistence.beans.conf.CSchema;
import org.apache.syncope.core.persistence.beans.conf.SyncopeConf;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MAttrUniqueValue;
import org.apache.syncope.core.persistence.beans.membership.MAttrValue;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MVirSchema;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RAttrUniqueValue;
import org.apache.syncope.core.persistence.beans.role.RAttrValue;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.role.RMappingItem;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RVirSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.beans.user.UVirAttr;
import org.apache.syncope.core.persistence.beans.user.UVirSchema;
import org.apache.syncope.core.sync.SyncCorrelationRule;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public final class AttributableUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AttributableUtil.class);

    private final AttributableType type;

    public static AttributableUtil getInstance(final AttributableType type) {
        return new AttributableUtil(type);
    }

    public static AttributableUtil valueOf(final String name) {
        return new AttributableUtil(AttributableType.valueOf(name));
    }

    public static AttributableUtil getInstance(final ObjectClass objectClass) {
        AttributableType type = null;
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            type = AttributableType.USER;
        }
        if (ObjectClass.GROUP.equals(objectClass)) {
            type = AttributableType.ROLE;
        }

        if (type == null) {
            throw new IllegalArgumentException("ObjectClass not supported: " + objectClass);
        }

        return new AttributableUtil(type);
    }

    public static AttributableUtil getInstance(final AbstractAttributable attributable) {
        AttributableType type = null;
        if (attributable instanceof SyncopeUser) {
            type = AttributableType.USER;
        }
        if (attributable instanceof SyncopeRole) {
            type = AttributableType.ROLE;
        }
        if (attributable instanceof Membership) {
            type = AttributableType.MEMBERSHIP;
        }
        if (attributable instanceof SyncopeConf) {
            type = AttributableType.CONFIGURATION;
        }

        if (type == null) {
            throw new IllegalArgumentException("Attributable type not supported: " + attributable.getClass().getName());
        }

        return new AttributableUtil(type);
    }

    private AttributableUtil(final AttributableType type) {
        this.type = type;
    }

    public <T extends AbstractAttributable> Class<T> attributableClass() {
        Class result;

        switch (type) {
            case ROLE:
                result = SyncopeRole.class;
                break;

            case MEMBERSHIP:
                result = Membership.class;
                break;

            case CONFIGURATION:
                result = SyncopeConf.class;

            case USER:
            default:
                result = SyncopeUser.class;
        }

        return result;
    }

    public AttributableType getType() {
        return type;
    }

    public String getAccountLink(final ExternalResource resource) {
        String result = null;

        if (resource != null) {
            switch (type) {
                case USER:
                    if (resource.getUmapping() != null) {
                        result = resource.getUmapping().getAccountLink();
                    }
                    break;
                case ROLE:
                    if (resource.getRmapping() != null) {
                        result = resource.getRmapping().getAccountLink();
                    }
                    break;
                case MEMBERSHIP:
                case CONFIGURATION:
                default:
            }
        }

        return result;
    }

    public <T extends AbstractMappingItem> T getAccountIdItem(final ExternalResource resource) {
        T result = null;

        if (resource != null) {
            switch (type) {
                case ROLE:
                    if (resource.getRmapping() != null) {
                        result = resource.getRmapping().getAccountIdItem();
                    }
                    break;
                case MEMBERSHIP:
                case USER:
                    if (resource.getUmapping() != null) {
                        result = resource.getUmapping().getAccountIdItem();
                    }
                    break;
                default:
            }
        }

        return result;
    }

    public <T extends AbstractMappingItem> List<T> getMappingItems(
            final ExternalResource resource, final MappingPurpose purpose) {

        List<T> items = Collections.<T>emptyList();

        if (resource != null) {
            switch (type) {
                case ROLE:
                    if (resource.getRmapping() != null) {
                        items = resource.getRmapping().getItems();
                    }
                    break;
                case MEMBERSHIP:
                case USER:
                    if (resource.getUmapping() != null) {
                        items = resource.getUmapping().getItems();
                    }
                    break;
                default:
            }
        }

        final List<T> result = new ArrayList<T>();

        switch (purpose) {
            case SYNCHRONIZATION:
                for (T item : items) {
                    if (MappingPurpose.PROPAGATION != item.getPurpose()
                            && MappingPurpose.NONE != item.getPurpose()) {

                        result.add(item);
                    }
                }
                break;

            case PROPAGATION:
                for (T item : items) {
                    if (MappingPurpose.SYNCHRONIZATION != item.getPurpose()
                            && MappingPurpose.NONE != item.getPurpose()) {

                        result.add(item);
                    }
                }
                break;

            case BOTH:
                for (T item : items) {
                    if (MappingPurpose.NONE != item.getPurpose()) {
                        result.add(item);
                    }
                }
                break;

            case NONE:
                for (T item : items) {
                    if (MappingPurpose.NONE == item.getPurpose()) {
                        result.add(item);
                    }
                }
                break;
            default:
                LOG.error("You requested not existing purpose {}", purpose);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractMappingItem> List<T> getUidToMappingItems(
            final ExternalResource resource, final MappingPurpose purpose, final AttributableType type) {
        final List<T> items = getMappingItems(resource, MappingPurpose.SYNCHRONIZATION);
        final AbstractMappingItem uidItem = type == AttributableType.USER ? new UMappingItem() : new RMappingItem();
        BeanUtils.copyProperties(getAccountIdItem(resource), uidItem);
        uidItem.setExtAttrName(Uid.NAME);
        uidItem.setAccountid(false);
        items.add((T) uidItem);
        return items;
    }

    public <T extends AbstractMappingItem> Class<T> mappingItemClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UMappingItem.class;
                break;
            case ROLE:
                result = RMappingItem.class;
                break;
            case MEMBERSHIP:
                result = AbstractMappingItem.class;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public IntMappingType intMappingType() {
        IntMappingType result = null;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleSchema;
                break;
            case MEMBERSHIP:
                result = IntMappingType.MembershipSchema;
                break;
            case USER:
                result = IntMappingType.UserSchema;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public IntMappingType derIntMappingType() {
        IntMappingType result = null;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleDerivedSchema;
                break;
            case MEMBERSHIP:
                result = IntMappingType.MembershipDerivedSchema;
                break;
            case USER:
                result = IntMappingType.UserDerivedSchema;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public IntMappingType virIntMappingType() {
        IntMappingType result = null;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleVirtualSchema;
                break;
            case MEMBERSHIP:
                result = IntMappingType.MembershipVirtualSchema;
                break;
            case USER:
                result = IntMappingType.UserVirtualSchema;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractNormalSchema> Class<T> schemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = USchema.class;
                break;
            case ROLE:
                result = RSchema.class;
                break;
            case MEMBERSHIP:
                result = MSchema.class;
                break;
            case CONFIGURATION:
                result = CSchema.class;
                break;
            default:
        }

        return result;
    }

    public <T extends AbstractNormalSchema> T newSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new USchema();
                break;

            case ROLE:
                result = (T) new RSchema();
                break;

            case MEMBERSHIP:
                result = (T) new MSchema();
                break;

            case CONFIGURATION:
                result = (T) new CSchema();
                break;

            default:
        }

        return result;
    }

    public <T extends AbstractDerSchema> Class<T> derSchemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UDerSchema.class;
                break;
            case ROLE:
                result = RDerSchema.class;
                break;
            case MEMBERSHIP:
                result = MDerSchema.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractVirSchema> Class<T> virSchemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UVirSchema.class;
                break;
            case ROLE:
                result = RVirSchema.class;
                break;
            case MEMBERSHIP:
                result = MVirSchema.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractDerSchema> T newDerSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UDerSchema();
                break;
            case ROLE:
                result = (T) new RDerSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MDerSchema();
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    public <T extends AbstractAttrTemplate> Class<T> attrTemplateClass() {
        Class result = null;

        switch (type) {
            case USER:
                break;
            case ROLE:
                result = RAttrTemplate.class;
                break;
            case MEMBERSHIP:
                result = MAttrTemplate.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractAttr> Class<T> attrClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UAttr.class;
                break;
            case ROLE:
                result = RAttr.class;
                break;
            case MEMBERSHIP:
                result = MAttr.class;
                break;
            case CONFIGURATION:
                result = CAttr.class;
            default:
        }

        return result;
    }

    public <T extends AbstractAttr> T newAttr() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UAttr();
                break;
            case ROLE:
                result = (T) new RAttr();
                break;
            case MEMBERSHIP:
                result = (T) new MAttr();
                break;
            case CONFIGURATION:
                result = (T) new CAttr();
            default:
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    public <T extends AbstractAttrTemplate> Class<T> derAttrTemplateClass() {
        Class result = null;

        switch (type) {
            case USER:
                break;
            case ROLE:
                result = RDerAttrTemplate.class;
                break;
            case MEMBERSHIP:
                result = MDerAttrTemplate.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractDerAttr> Class<T> derAttrClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UDerAttr.class;
                break;
            case ROLE:
                result = RDerAttr.class;
                break;
            case MEMBERSHIP:
                result = MDerAttr.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    public <T extends AbstractAttrTemplate> Class<T> virAttrTemplateClass() {
        Class result = null;

        switch (type) {
            case USER:
                break;
            case ROLE:
                result = RVirAttrTemplate.class;
                break;
            case MEMBERSHIP:
                result = MVirAttrTemplate.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractVirAttr> Class<T> virAttrClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UVirAttr.class;
                break;
            case ROLE:
                result = RVirAttr.class;
                break;
            case MEMBERSHIP:
                result = MVirAttr.class;
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractDerAttr> T newDerAttr() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UDerAttr();
                break;
            case ROLE:
                result = (T) new RDerAttr();
                break;
            case MEMBERSHIP:
                result = (T) new MDerAttr();
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractVirAttr> T newVirAttr() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UVirAttr();
                break;
            case ROLE:
                result = (T) new RVirAttr();
                break;
            case MEMBERSHIP:
                result = (T) new MVirAttr();
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractVirSchema> T newVirSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UVirSchema();
                break;
            case ROLE:
                result = (T) new RVirSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MVirSchema();
                break;
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public <T extends AbstractAttrValue> Class<T> attrValueClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UAttrValue.class;
                break;
            case ROLE:
                result = RAttrValue.class;
                break;
            case MEMBERSHIP:
                result = MAttrValue.class;
                break;
            case CONFIGURATION:
                result = CAttrValue.class;
                break;
            default:
        }

        return result;
    }

    public <T extends AbstractAttrValue> T newAttrValue() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UAttrValue();
                break;
            case ROLE:
                result = (T) new RAttrValue();
                break;
            case MEMBERSHIP:
                result = (T) new MAttrValue();
                break;
            case CONFIGURATION:
                result = (T) new CAttrValue();
                break;
            default:
        }

        return result;
    }

    public <T extends AbstractAttrValue> Class<T> attrUniqueValueClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UAttrUniqueValue.class;
                break;
            case ROLE:
                result = RAttrUniqueValue.class;
                break;
            case MEMBERSHIP:
                result = MAttrUniqueValue.class;
                break;
            case CONFIGURATION:
                result = CAttrUniqueValue.class;
                break;
            default:
        }

        return result;
    }

    public <T extends AbstractAttrValue> T newAttrUniqueValue() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UAttrUniqueValue();
                break;
            case ROLE:
                result = (T) new RAttrUniqueValue();
                break;
            case MEMBERSHIP:
                result = (T) new MAttrUniqueValue();
                break;
            case CONFIGURATION:
                result = (T) new CAttrUniqueValue();
                break;
            default:
        }

        return result;
    }

    public List<String> getAltSearchSchemas(final SyncPolicySpec policySpec) {
        List<String> result = Collections.EMPTY_LIST;

        switch (type) {
            case USER:
                result = policySpec.getuAltSearchSchemas();
                break;
            case ROLE:
                result = policySpec.getrAltSearchSchemas();
                break;
            case MEMBERSHIP:
            case CONFIGURATION:
            default:
        }

        return result;
    }

    public SyncCorrelationRule getCorrelationRule(final SyncPolicySpec policySpec) {
        String clazz;

        switch (type) {
            case USER:
                clazz = policySpec.getUserJavaRule();
                break;
            case ROLE:
                clazz = policySpec.getRoleJavaRule();
                break;
            case MEMBERSHIP:
            case CONFIGURATION:
            default:
                clazz = null;
        }

        SyncCorrelationRule res = null;

        if (StringUtils.isNotBlank(clazz)) {
            try {
                res = (SyncCorrelationRule) Class.forName(clazz).newInstance();
            } catch (Exception e) {
                LOG.error("Failure instantiating correlation rule class '{}'", clazz, e);
            }
        }

        return res;
    }

    public <T extends AbstractAttributableTO> T newAttributableTO() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UserTO();
                break;
            case ROLE:
                result = (T) new RoleTO();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipTO();
                break;
            case CONFIGURATION:
                result = (T) new ConfTO();
                break;
            default:
        }

        return result;
    }

    public <T extends AbstractSubjectTO> T newSubjectTO() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UserTO();
                break;
            case ROLE:
                result = (T) new RoleTO();
                break;
            case MEMBERSHIP:
            case CONFIGURATION:
            default:
                break;
        }

        return result;
    }
}
