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
package org.apache.syncope.core.rest.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.ConnPoolConfTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.core.connid.ConnPoolConfUtil;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConnInstanceDataBinder {

    private static final String[] IGNORE_PROPERTIES = { "id", "poolConf" };

    @Autowired
    private ConnIdBundleManager connIdBundleManager;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    /**
     * Merge connector configuration properties avoiding repetition but giving priority to primary set.
     *
     * @param primary primary set.
     * @param secondary secondary set.
     * @return merged set.
     */
    public Set<ConnConfProperty> mergeConnConfProperties(final Set<ConnConfProperty> primary,
            final Set<ConnConfProperty> secondary) {

        final Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        // to be used to control managed prop (needed by overridden mechanism)
        final Set<String> propertyNames = new HashSet<String>();

        // get overridden connector configuration properties
        for (ConnConfProperty prop : primary) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                conf.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        // get connector configuration properties
        for (ConnConfProperty prop : secondary) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                conf.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        return conf;
    }

    public ConnInstance getConnInstance(final ConnInstanceTO connInstanceTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        if (connInstanceTO.getLocation() == null) {
            sce.getElements().add("location");
        }

        if (connInstanceTO.getBundleName() == null) {
            sce.getElements().add("bundlename");
        }

        if (connInstanceTO.getVersion() == null) {
            sce.getElements().add("bundleversion");
        }

        if (connInstanceTO.getConnectorName() == null) {
            sce.getElements().add("connectorname");
        }

        if (connInstanceTO.getConfiguration() == null || connInstanceTO.getConfiguration().isEmpty()) {
            sce.getElements().add("configuration");
        }

        ConnInstance connInstance = new ConnInstance();

        BeanUtils.copyProperties(connInstanceTO, connInstance, IGNORE_PROPERTIES);
        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation());
        }
        if (connInstanceTO.getPoolConf() != null) {
            connInstance.setPoolConf(ConnPoolConfUtil.getConnPoolConf(connInstanceTO.getPoolConf()));
        }

        // Throw exception if there is at least one element set
        if (!sce.isEmpty()) {
            throw sce;
        }

        return connInstance;
    }

    public ConnInstance updateConnInstance(final long connInstanceId, final ConnInstanceTO connInstanceTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        if (connInstanceId == 0) {
            sce.getElements().add("connector id");
        }

        ConnInstance connInstance = connInstanceDAO.find(connInstanceId);
        connInstance.setCapabilities(connInstanceTO.getCapabilities());

        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation());
        }

        if (connInstanceTO.getBundleName() != null) {
            connInstance.setBundleName(connInstanceTO.getBundleName());
        }

        if (connInstanceTO.getVersion() != null) {
            connInstance.setVersion(connInstanceTO.getVersion());
        }

        if (connInstanceTO.getConnectorName() != null) {
            connInstance.setConnectorName(connInstanceTO.getConnectorName());
        }

        if (connInstanceTO.getConfiguration() != null && !connInstanceTO.getConfiguration().isEmpty()) {
            connInstance.setConfiguration(connInstanceTO.getConfiguration());
        }

        if (connInstanceTO.getDisplayName() != null) {
            connInstance.setDisplayName(connInstanceTO.getDisplayName());
        }

        if (connInstanceTO.getConnRequestTimeout() != null) {
            connInstance.setConnRequestTimeout(connInstanceTO.getConnRequestTimeout());
        }

        if (connInstanceTO.getPoolConf() == null) {
            connInstance.setPoolConf(null);
        } else {
            connInstance.setPoolConf(ConnPoolConfUtil.getConnPoolConf(connInstanceTO.getPoolConf()));
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        return connInstance;
    }

    public ConnConfPropSchema buildConnConfPropSchema(final ConfigurationProperty property) {
        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();

        connConfPropSchema.setName(property.getName());
        connConfPropSchema.setDisplayName(property.getDisplayName(property.getName()));
        connConfPropSchema.setHelpMessage(property.getHelpMessage(property.getName()));
        connConfPropSchema.setRequired(property.isRequired());
        connConfPropSchema.setType(property.getType().getName());
        connConfPropSchema.setOrder(((ConfigurationPropertyImpl) property).getOrder());
        connConfPropSchema.setConfidential(property.isConfidential());

        if (property.getValue() != null) {
            if (property.getValue().getClass().isArray()) {
                connConfPropSchema.getDefaultValues().addAll(Arrays.asList((Object[]) property.getValue()));
            } else if (property.getValue() instanceof Collection<?>) {
                connConfPropSchema.getDefaultValues().addAll((Collection<?>) property.getValue());
            } else {
                connConfPropSchema.getDefaultValues().add(property.getValue());
            }
        }

        return connConfPropSchema;
    }

    public ConnInstanceTO getConnInstanceTO(final ConnInstance connInstance) {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(connInstance.getId() == null ? 0L : connInstance.getId().longValue());

        // retrieve the ConfigurationProperties
        ConfigurationProperties properties = connIdBundleManager.getConfigurationProperties(
                connIdBundleManager.getConnectorInfo(connInstance.getLocation(),
                        connInstance.getBundleName(), connInstance.getVersion(), connInstance.getConnectorName()));

        BeanUtils.copyProperties(connInstance, connInstanceTO, IGNORE_PROPERTIES);

        final Map<String, ConnConfProperty> connInstanceToConfMap = connInstanceTO.getConfigurationMap();

        for (String propName : properties.getPropertyNames()) {
            ConnConfPropSchema schema = buildConnConfPropSchema(properties.getProperty(propName));

            ConnConfProperty property;
            if (connInstanceToConfMap.containsKey(propName)) {
                property = connInstanceToConfMap.get(propName);
            } else {
                property = new ConnConfProperty();
                connInstanceTO.getConfiguration().add(property);
            }

            property.setSchema(schema);
        }

        if (connInstance.getPoolConf() != null) {
            ConnPoolConfTO poolConf = new ConnPoolConfTO();
            BeanUtils.copyProperties(connInstance.getPoolConf(), poolConf);
            connInstanceTO.setPoolConf(poolConf);
        }

        return connInstanceTO;
    }
}
