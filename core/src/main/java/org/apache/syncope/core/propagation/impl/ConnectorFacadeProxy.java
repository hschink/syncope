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
package org.apache.syncope.core.propagation.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.connid.ConnPoolConfUtil;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ClassUtils;

public class ConnectorFacadeProxy implements Connector {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorFacadeProxy.class);

    /**
     * Connector facade wrapped instance.
     */
    private final ConnectorFacade connector;

    /**
     * Active connector instance.
     */
    private final ConnInstance activeConnInstance;

    @Autowired
    private AsyncConnectorFacade asyncFacade;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will be used to make all wrapped calls.
     *
     * @param connInstance the connector instance configuration
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(final ConnInstance connInstance) {
        this.activeConnInstance = connInstance;

        ConnIdBundleManager connIdBundleManager =
                ApplicationContextProvider.getApplicationContext().getBean(ConnIdBundleManager.class);
        ConnectorInfo info = connIdBundleManager.getConnectorInfo(connInstance.getLocation(),
                connInstance.getBundleName(), connInstance.getVersion(), connInstance.getConnectorName());

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        // set connector configuration according to conninstance's
        ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        for (ConnConfProperty property : connInstance.getConfiguration()) {
            if (property.getValues() != null && !property.getValues().isEmpty()) {
                properties.setPropertyValue(property.getSchema().getName(),
                        getPropertyValue(property.getSchema().getType(), property.getValues()));
            }
        }

        // set pooling configuration (if supported) according to conninstance's
        if (connInstance.getPoolConf() != null) {
            if (apiConfig.isConnectorPoolingSupported()) {
                ConnPoolConfUtil.updateObjectPoolConfiguration(
                        apiConfig.getConnectorPoolConfiguration(), connInstance.getPoolConf());
            } else {
                LOG.warn("Connector pooling not supported for {}", info);
            }
        }

        // gets new connector, with the given configuration
        connector = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);
        if (connector == null) {
            throw new NotFoundException("Connector");
        }

        // make sure we have set up the Configuration properly
        connector.validate();
    }

    @Override
    public Uid authenticate(final String username, final String password, final OperationOptions options) {
        Uid result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.AUTHENTICATE)) {
            final Future<Uid> future = asyncFacade.authenticate(
                    connector, username, new GuardedString(password.toCharArray()), options);
            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("Authenticate was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public Uid create(final PropagationMode propagationMode, final ObjectClass objectClass, final Set<Attribute> attrs,
            final OperationOptions options, final Set<String> propagationAttempted) {

        Uid result = null;

        if (propagationMode == PropagationMode.ONE_PHASE
                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_CREATE)
                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_CREATE)) {

            propagationAttempted.add("create");

            final Future<Uid> future = asyncFacade.create(connector, objectClass, attrs, options);
            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("Create was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public Uid update(final PropagationMode propagationMode, final ObjectClass objectClass, final Uid uid,
            final Set<Attribute> attrs, final OperationOptions options, final Set<String> propagationAttempted) {

        Uid result = null;

        if (propagationMode == PropagationMode.ONE_PHASE
                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_UPDATE)
                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_UPDATE)) {

            propagationAttempted.add("update");

            final Future<Uid> future = asyncFacade.update(connector, objectClass, uid, attrs, options);

            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("Update for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.", uid.getUidValue(), activeConnInstance.
                    getCapabilities());
        }

        return result;
    }

    @Override
    public void delete(final PropagationMode propagationMode, final ObjectClass objectClass, final Uid uid,
            final OperationOptions options, final Set<String> propagationAttempted) {

        if (propagationMode == PropagationMode.ONE_PHASE
                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_DELETE)
                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_DELETE)) {

            propagationAttempted.add("delete");

            final Future<Uid> future = asyncFacade.delete(connector, objectClass, uid, options);

            try {
                future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("Delete for {} was attempted, although the connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), activeConnInstance.getCapabilities());
        }
    }

    @Override
    public void sync(final ObjectClass objectClass, final SyncToken token, final SyncResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            connector.sync(objectClass, token, handler, options);
        } else {
            LOG.info("Sync was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    @Override
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        SyncToken result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            final Future<SyncToken> future = asyncFacade.getLatestSyncToken(connector, objectClass);

            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("getLatestSyncToken was attempted, although the "
                    + "connector only has these capabilities: {}. No action.", activeConnInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public ConnectorObject getObject(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        return getObject(null, null, objectClass, uid, options);
    }

    @Override
    public ConnectorObject getObject(final PropagationMode propagationMode, final ResourceOperation operationType,
            final ObjectClass objectClass, final Uid uid, final OperationOptions options) {

        Future<ConnectorObject> future = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (operationType == null) {
                future = asyncFacade.getObject(connector, objectClass, uid, options);
            } else {
                switch (operationType) {
                    case CREATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.ONE_PHASE_CREATE)
                                : activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.TWO_PHASES_CREATE))) {

                            future = asyncFacade.getObject(connector, objectClass, uid, options);
                        }
                        break;
                    case UPDATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.ONE_PHASE_UPDATE)
                                : activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.TWO_PHASES_UPDATE))) {

                            future = asyncFacade.getObject(connector, objectClass, uid, options);
                        }
                        break;
                    default:
                        future = asyncFacade.getObject(connector, objectClass, uid, options);
                }
            }
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        try {
            return future == null ? null : future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public List<ConnectorObject> search(final ObjectClass objectClass, final Filter filter,
            final OperationOptions options) {

        final List<ConnectorObject> result = new ArrayList<ConnectorObject>();

        search(objectClass, filter, new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                return result.add(obj);
            }
        }, options);

        return result;
    }

    @Override
    public void getAllObjects(
            final ObjectClass objectClass, final SyncResultsHandler handler, final OperationOptions options) {

        search(objectClass, null, new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                final SyncDeltaBuilder bld = new SyncDeltaBuilder();
                bld.setObject(obj);
                bld.setUid(obj.getUid());
                bld.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
                bld.setToken(new SyncToken(""));

                return handler.handle(bld.build());
            }
        }, options);
    }

    @Override
    public Attribute getObjectAttribute(final ObjectClass objectClass, final Uid uid, final OperationOptions options,
            final String attributeName) {

        final Future<Attribute> future = asyncFacade.getObjectAttribute(connector, objectClass, uid, options,
                attributeName);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public Set<Attribute> getObjectAttributes(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {

        final Future<Set<Attribute>> future = asyncFacade.getObjectAttributes(connector, objectClass, uid, options);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public Set<String> getSchemaNames(final boolean includeSpecial) {
        final Future<Set<String>> future = asyncFacade.getSchemaNames(connector, includeSpecial);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public Set<ObjectClass> getSupportedObjectClasses() {
        final Future<Set<ObjectClass>> future = asyncFacade.getSupportedObjectClasses(connector);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public void validate() {
        final Future<String> future = asyncFacade.test(connector);
        try {
            future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public void test() {
        final Future<String> future = asyncFacade.test(connector);
        try {
            future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    private void search(
            final ObjectClass objectClass,
            final Filter filter,
            final ResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            connector.search(objectClass, filter, handler, options);
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    @Override
    public ConnInstance getActiveConnInstance() {
        return activeConnInstance;
    }

    @Override
    public OperationOptions getOperationOptions(final Collection<AbstractMappingItem> mapItems) {
        // -------------------------------------
        // Ask just for mapped attributes
        // -------------------------------------
        final OperationOptionsBuilder oob = new OperationOptionsBuilder();

        final Set<String> attrsToGet = new HashSet<String>();
        attrsToGet.add(Name.NAME);
        attrsToGet.add(Uid.NAME);
        attrsToGet.add(OperationalAttributes.ENABLE_NAME);

        for (AbstractMappingItem item : mapItems) {
            attrsToGet.add(item.getExtAttrName());
        }

        oob.setAttributesToGet(attrsToGet);
        // -------------------------------------

        return oob.build();
    }

    private Object getPropertyValue(final String propType, final List<?> values) {
        Object value = null;

        try {
            final Class<?> propertySchemaClass = ClassUtils.forName(propType, ClassUtils.getDefaultClassLoader());

            if (GuardedString.class.equals(propertySchemaClass)) {
                value = new GuardedString(values.get(0).toString().toCharArray());
            } else if (GuardedByteArray.class.equals(propertySchemaClass)) {
                value = new GuardedByteArray((byte[]) values.get(0));
            } else if (Character.class.equals(propertySchemaClass) || Character.TYPE.equals(propertySchemaClass)) {
                value = values.get(0) == null || values.get(0).toString().isEmpty()
                        ? null : values.get(0).toString().charAt(0);
            } else if (Integer.class.equals(propertySchemaClass) || Integer.TYPE.equals(propertySchemaClass)) {
                value = Integer.parseInt(values.get(0).toString());
            } else if (Long.class.equals(propertySchemaClass) || Long.TYPE.equals(propertySchemaClass)) {
                value = Long.parseLong(values.get(0).toString());
            } else if (Float.class.equals(propertySchemaClass) || Float.TYPE.equals(propertySchemaClass)) {
                value = Float.parseFloat(values.get(0).toString());
            } else if (Double.class.equals(propertySchemaClass) || Double.TYPE.equals(propertySchemaClass)) {
                value = Double.parseDouble(values.get(0).toString());
            } else if (Boolean.class.equals(propertySchemaClass) || Boolean.TYPE.equals(propertySchemaClass)) {
                value = Boolean.parseBoolean(values.get(0).toString());
            } else if (URI.class.equals(propertySchemaClass)) {
                value = URI.create(values.get(0).toString());
            } else if (File.class.equals(propertySchemaClass)) {
                value = new File(values.get(0).toString());
            } else if (String[].class.equals(propertySchemaClass)) {
                value = values.toArray(new String[] {});
            } else {
                value = values.get(0) == null ? null : values.get(0).toString();
            }
        } catch (Exception e) {
            LOG.error("Invalid ConnConfProperty specified: {} {}", propType, values, e);
        }

        return value;
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector + "\n" + "capabitilies=" + activeConnInstance.getCapabilities() + '}';
    }
}
