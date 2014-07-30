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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RMappingItem;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.AttrTemplateDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl implements SchemaDAO {

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private AttrTemplateDAO attrTemplateDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    public <T extends AbstractNormalSchema> T find(final String name, final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    public <T extends AbstractNormalSchema> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery("SELECT e FROM " + reference.getSimpleName() + " e", reference);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractAttr> List<T> findAttrs(final AbstractNormalSchema schema, final Class<T> reference) {
        final StringBuilder queryString =
                new StringBuilder("SELECT e FROM ").append(reference.getSimpleName()).append(" e WHERE e.");
        if (reference.equals(RAttr.class) || reference.equals(MAttr.class)) {
            queryString.append("template.");
        }
        queryString.append("schema=:schema");

        TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractNormalSchema> T save(final T schema) {
        return entityManager.merge(schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(final String name, final AttributableUtil attributableUtil) {
        AbstractNormalSchema schema = find(name, attributableUtil.schemaClass());
        if (schema == null) {
            return;
        }

        final Set<Long> attrIds = new HashSet<Long>();
        for (AbstractAttr attr : findAttrs(schema, attributableUtil.attrClass())) {
            attrIds.add(attr.getId());
        }
        for (Long attrId : attrIds) {
            attrDAO.delete(attrId, attributableUtil.attrClass());
        }

        if (attributableUtil.getType() == AttributableType.ROLE
                || attributableUtil.getType() == AttributableType.MEMBERSHIP) {

            for (Iterator<Number> it = attrTemplateDAO.
                    findBySchemaName(schema.getName(), attributableUtil.attrTemplateClass()).iterator();
                    it.hasNext();) {

                attrTemplateDAO.delete(it.next().longValue(), attributableUtil.attrTemplateClass());
            }
        }

        resourceDAO.deleteMapping(name, attributableUtil.intMappingType(), UMappingItem.class);
        resourceDAO.deleteMapping(name, attributableUtil.intMappingType(), RMappingItem.class);

        entityManager.remove(schema);
    }
}
