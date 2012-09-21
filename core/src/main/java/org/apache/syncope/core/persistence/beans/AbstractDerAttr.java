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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.lang.ArrayUtils;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.JexlUtil;
import org.springframework.context.ConfigurableApplicationContext;

@MappedSuperclass
public abstract class AbstractDerAttr extends AbstractBaseBean {

    private static final long serialVersionUID = 4740924251090424771L;

    private final List<String> ignoredFields = Arrays.asList(
            new String[]{"password", "clearPassword", "serialVersionUID"});

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    public Long getId() {
        return id;
    }

    /**
     * @see http://commons.apache.org/jexl/reference/index.html
     * @param attributes the set of attributes against which evaluate this derived attribute
     * @return the value of this derived attribute
     */
    public String getValue(final Collection<? extends AbstractAttr> attributes) {

        final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        final JexlUtil jexlUtil = context.getBean(JexlUtil.class);

        // Prepare context using user attributes
        final JexlContext jexlContext = jexlUtil.addAttrsToContext(attributes, null);

        createJexlContext(jexlContext);

        // Evaluate expression using the context prepared before
        return jexlUtil.evaluate(getDerivedSchema().getExpression(), jexlContext);
    }

    private void createJexlContext(final JexlContext jexlContext) {
        AbstractAttributable instance = getOwner();
        Field[] fields = instance.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                field.setAccessible(true);
                if ((!field.isSynthetic()) && (!field.getName().startsWith("pc"))
                        && (!ArrayUtils.contains(ignoredFields.toArray(), field.getName()))
                        && (!Iterable.class.isAssignableFrom(field.getType()))
                        && (!field.getType().isArray())) {
                    if (field.getType().equals(Date.class)) {
                        jexlContext.set(field.getName(), field.get(instance) != null
                                ? ((AbstractBaseBean) instance).getDateFormatter().format(field.get(instance)) : "");
                    } else {
                        jexlContext.set(field.getName(), field.get(instance) != null ? field.get(instance) : "");
                    }
                }

            } catch (Exception ex) {
                LOG.error("Reading class attributes error", ex);
            }
        }
    }

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractDerSchema> T getDerivedSchema();

    public abstract <T extends AbstractDerSchema> void setDerivedSchema(T derivedSchema);
}
