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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.propagation.PropagationActions;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.ExceptionUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractPropagationTaskExecutor implements PropagationTaskExecutor {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractPropagationTaskExecutor.class);

    /**
     * Connector factory.
     */
    @Autowired
    protected ConnectorFactory connFactory;

    /**
     * ConnObjectUtil.
     */
    @Autowired
    protected ConnObjectUtil connObjectUtil;

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected UserDataBinder userDataBinder;

    @Autowired
    protected RoleDataBinder roleDataBinder;

    /**
     * Notification Manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit Manager.
     */
    @Autowired
    protected AuditManager auditManager;

    @Override
    public TaskExec execute(final PropagationTask task) {
        return execute(task, null);
    }

    protected List<PropagationActions> getPropagationActions(final ExternalResource resource) {
        List<PropagationActions> result = new ArrayList<PropagationActions>();

        if (!resource.getPropagationActionsClassNames().isEmpty()) {
            for (String className : resource.getPropagationActionsClassNames()) {
                try {
                    Class<?> actionsClass = Class.forName(className);
                    result.add((PropagationActions) ApplicationContextProvider.getBeanFactory().
                            createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true));
                } catch (ClassNotFoundException e) {
                    LOG.error("Invalid PropagationAction class name '{}' for resource {}", resource, className, e);
                }
            }
        }

        return result;
    }

    public static void createOrUpdate(
            final ObjectClass oclass,
            final String accountId,
            final Set<Attribute> attrs,
            final String resource,
            final PropagationMode propagationMode,
            final ConnectorObject beforeObj,
            final Connector connector,
            final Set<String> propagationAttempted,
            final ConnObjectUtil connObjectUtil) {

        // set of attributes to be propagated
        final Set<Attribute> attributes = new HashSet<Attribute>(attrs);

        // check if there is any missing or null / empty mandatory attribute
        List<Object> mandatoryAttrNames = new ArrayList<Object>();
        Attribute mandatoryMissing = AttributeUtil.find(MANDATORY_MISSING_ATTR_NAME, attrs);
        if (mandatoryMissing != null) {
            attributes.remove(mandatoryMissing);

            if (beforeObj == null) {
                mandatoryAttrNames.addAll(mandatoryMissing.getValue());
            }
        }
        Attribute mandatoryNullOrEmpty = AttributeUtil.find(MANDATORY_NULL_OR_EMPTY_ATTR_NAME, attrs);
        if (mandatoryNullOrEmpty != null) {
            attributes.remove(mandatoryNullOrEmpty);

            mandatoryAttrNames.addAll(mandatoryNullOrEmpty.getValue());
        }
        if (!mandatoryAttrNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Not attempted because there are mandatory attributes without value(s): " + mandatoryAttrNames);
        }

        if (beforeObj == null) {
            LOG.debug("Create {} on {}", attributes, resource);
            connector.create(propagationMode, oclass, attributes, null, propagationAttempted);
        } else {
            // 1. check if rename is really required
            final Name newName = (Name) AttributeUtil.find(Name.NAME, attributes);

            LOG.debug("Rename required with value {}", newName);

            if (newName != null && newName.equals(beforeObj.getName())
                    && !newName.getNameValue().equals(beforeObj.getUid().getUidValue())) {

                LOG.debug("Remote object name unchanged");
                attributes.remove(newName);
            }

            // 2. check wether anything is actually needing to be propagated, i.e. if there is attribute
            // difference between beforeObj - just read above from the connector - and the values to be propagated
            Map<String, Attribute> originalAttrMap = connObjectUtil.toMap(beforeObj.getAttributes());
            Map<String, Attribute> updateAttrMap = connObjectUtil.toMap(attributes);

            // Only compare attribute from beforeObj that are also being updated
            Set<String> skipAttrNames = originalAttrMap.keySet();
            skipAttrNames.removeAll(updateAttrMap.keySet());
            for (String attrName : new HashSet<String>(skipAttrNames)) {
                originalAttrMap.remove(attrName);
            }

            Set<Attribute> originalAttrs = new HashSet<Attribute>(originalAttrMap.values());

            if (originalAttrs.equals(attributes)) {
                LOG.debug("Don't need to propagate anything: {} is equal to {}", originalAttrs, attributes);
            } else {
                LOG.debug("Attributes that would be updated {}", attributes);

                Set<Attribute> strictlyModified = new HashSet<Attribute>();
                for (Attribute attr : attributes) {
                    if (!originalAttrs.contains(attr)) {
                        strictlyModified.add(attr);
                    }
                }

                // 3. provision entry
                LOG.debug("Update {} on {}", strictlyModified, resource);

                connector.update(propagationMode, beforeObj.getObjectClass(),
                        beforeObj.getUid(), strictlyModified, null, propagationAttempted);
            }
        }
    }

    protected void createOrUpdate(
            final PropagationTask task,
            final ConnectorObject beforeObj,
            final Connector connector,
            final Set<String> propagationAttempted) {

        createOrUpdate(
                new ObjectClass(task.getObjectClassName()),
                task.getAccountId(),
                task.getAttributes(),
                task.getResource().getName(),
                task.getResource().getPropagationMode(),
                beforeObj,
                connector,
                propagationAttempted,
                connObjectUtil);
    }

    protected AbstractSubject getSubject(final PropagationTask task) {
        AbstractSubject subject = null;

        if (task.getSubjectId() != null) {
            switch (task.getSubjectType()) {
                case USER:
                    try {
                        subject = userDataBinder.getUserFromId(task.getSubjectId());
                    } catch (Exception e) {
                        LOG.error("Could not read user {}", task.getSubjectId(), e);
                    }
                    break;

                case ROLE:
                    try {
                        subject = roleDataBinder.getRoleFromId(task.getSubjectId());
                    } catch (Exception e) {
                        LOG.error("Could not read role {}", task.getSubjectId(), e);
                    }
                    break;

                case MEMBERSHIP:
                default:
            }
        }

        return subject;
    }

    protected void delete(final PropagationTask task, final ConnectorObject beforeObj,
            final Connector connector, final Set<String> propagationAttempted) {

        if (beforeObj == null) {
            LOG.debug("{} not found on external resource: ignoring delete", task.getAccountId());
        } else {
            /*
             * We must choose here whether to
             * a. actually delete the provided user / role from the external resource
             * b. just update the provided user / role data onto the external resource
             *
             * (a) happens when either there is no user / role associated with the PropagationTask (this takes place
             * when the task is generated via UserController.delete() / RoleController.delete()) or the provided updated
             * user / role hasn't the current resource assigned (when the task is generated via
             * UserController.update() / RoleController.update()).
             *
             * (b) happens when the provided updated user / role does have the current resource assigned (when the task
             * is generated via UserController.update() / RoleController.updae()): this basically means that before such
             * update, this user / role used to have the current resource assigned by more than one mean (for example,
             * two different memberships with the same resource).
             */
            AbstractSubject subject = getSubject(task);
            if (subject == null || !subject.getResourceNames().contains(task.getResource().getName())) {
                LOG.debug("Delete {} on {}", beforeObj.getUid(), task.getResource().getName());

                connector.delete(
                        task.getPropagationMode(),
                        beforeObj.getObjectClass(),
                        beforeObj.getUid(),
                        null,
                        propagationAttempted);
            } else {
                createOrUpdate(task, beforeObj, connector, propagationAttempted);
            }
        }
    }

    @Override
    public TaskExec execute(final PropagationTask task, final PropagationReporter reporter) {
        final List<PropagationActions> actions = getPropagationActions(task.getResource());

        final Date startDate = new Date();

        final TaskExec execution = new TaskExec();
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());

        String taskExecutionMessage = null;
        String failureReason = null;

        // Flag to state whether any propagation has been attempted
        Set<String> propagationAttempted = new HashSet<String>();

        ConnectorObject beforeObj = null;
        ConnectorObject afterObj = null;

        Connector connector = null;
        Result result;
        try {
            connector = connFactory.getConnector(task.getResource());

            // Try to read remote object (user / group) BEFORE any actual operation
            beforeObj = getRemoteObject(task, connector, false);

            for (PropagationActions action : actions) {
                action.before(task, beforeObj);
            }

            switch (task.getPropagationOperation()) {
                case CREATE:
                case UPDATE:
                    createOrUpdate(task, beforeObj, connector, propagationAttempted);
                    break;

                case DELETE:
                    delete(task, beforeObj, connector, propagationAttempted);
                    break;

                default:
            }

            execution.setStatus(task.getPropagationMode() == PropagationMode.ONE_PHASE
                    ? PropagationTaskExecStatus.SUCCESS.name()
                    : PropagationTaskExecStatus.SUBMITTED.name());

            LOG.debug("Successfully propagated to {}", task.getResource());
            result = Result.SUCCESS;
        } catch (Exception e) {
            result = Result.FAILURE;
            LOG.error("Exception during provision on resource " + task.getResource().getName(), e);

            if (e instanceof ConnectorException && e.getCause() != null) {
                taskExecutionMessage = e.getCause().getMessage();
                if (e.getCause().getMessage() == null) {
                    failureReason = e.getMessage();
                } else {
                    failureReason = e.getMessage() + "\n\n Cause: " + e.getCause().getMessage().split("\n")[0];
                }
            } else {
                taskExecutionMessage = ExceptionUtil.getFullStackTrace(e);
                if (e.getCause() == null) {
                    failureReason = e.getMessage();
                } else {
                    failureReason = e.getMessage() + "\n\n Cause: " + e.getCause().getMessage().split("\n")[0];
                }
            }

            try {
                execution.setStatus(task.getPropagationMode() == PropagationMode.ONE_PHASE
                        ? PropagationTaskExecStatus.FAILURE.name()
                        : PropagationTaskExecStatus.UNSUBMITTED.name());
            } catch (Exception wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted.add(task.getPropagationOperation().name().toLowerCase());
        } finally {
            // Try to read remote object (user / group) AFTER any actual operation
            if (connector != null) {
                try {
                    afterObj = getRemoteObject(task, connector, true);
                } catch (Exception ignore) {
                    // ignore exception
                    LOG.error("Error retrieving after object", ignore);
                }
            }

            LOG.debug("Update execution for {}", task);

            execution.setStartDate(startDate);
            execution.setMessage(taskExecutionMessage);
            execution.setEndDate(new Date());

            if (hasToBeregistered(task, execution)) {
                if (propagationAttempted.isEmpty()) {
                    LOG.debug("No propagation attempted for {}", execution);
                } else {
                    execution.setTask(task);
                    task.addExec(execution);

                    LOG.debug("Execution finished: {}", execution);
                }

                taskDAO.save(task);

                // this flush call is needed to generate a value for the execution id
                taskDAO.flush();
            }

            if (reporter != null) {
                reporter.onSuccessOrSecondaryResourceFailures(
                        task.getResource().getName(),
                        PropagationTaskExecStatus.valueOf(execution.getStatus()),
                        failureReason,
                        beforeObj,
                        afterObj);
            }
        }

        for (PropagationActions action : actions) {
            action.after(task, execution, afterObj);
        }

        notificationManager.createTasks(
                AuditElements.EventCategoryType.PROPAGATION,
                task.getSubjectType().name().toLowerCase(),
                task.getResource().getName(),
                task.getPropagationOperation().name().toLowerCase(),
                result,
                beforeObj, // searching for before object is too much expensive ... 
                new Object[] { execution, afterObj },
                task);

        auditManager.audit(
                AuditElements.EventCategoryType.PROPAGATION,
                task.getSubjectType().name().toLowerCase(),
                task.getResource().getName(),
                task.getPropagationOperation().name().toLowerCase(),
                result,
                beforeObj, // searching for before object is too much expensive ... 
                new Object[] { execution, afterObj },
                task);

        return execution;
    }

    @Override
    public void execute(final Collection<PropagationTask> tasks) {
        execute(tasks, null);
    }

    @Override
    public abstract void execute(Collection<PropagationTask> tasks, final PropagationReporter reporter);

    /**
     * Check whether an execution has to be stored, for a given task.
     *
     * @param task execution's task
     * @param execution to be decide whether to store or not
     * @return true if execution has to be store, false otherwise
     */
    protected boolean hasToBeregistered(final PropagationTask task, final TaskExec execution) {
        boolean result;

        final boolean failed = !PropagationTaskExecStatus.valueOf(execution.getStatus()).isSuccessful();

        switch (task.getPropagationOperation()) {

            case CREATE:
                result = (failed && task.getResource().getCreateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getCreateTraceLevel() == TraceLevel.ALL;
                break;

            case UPDATE:
                result = (failed && task.getResource().getUpdateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getUpdateTraceLevel() == TraceLevel.ALL;
                break;

            case DELETE:
                result = (failed && task.getResource().getDeleteTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getDeleteTraceLevel() == TraceLevel.ALL;
                break;

            default:
                result = false;
        }

        return result;
    }

    /**
     * Get remote object for given task.
     *
     * @param connector connector facade proxy.
     * @param task current propagation task.
     * @param latest 'FALSE' to retrieve object using old accountId if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(final PropagationTask task, final Connector connector,
            final boolean latest) {

        String accountId = latest || task.getOldAccountId() == null
                ? task.getAccountId()
                : task.getOldAccountId();

        ConnectorObject obj = null;
        try {
            obj = connector.getObject(task.getPropagationMode(),
                    task.getPropagationOperation(),
                    new ObjectClass(task.getObjectClassName()),
                    new Uid(accountId),
                    connector.getOperationOptions(AttributableUtil.getInstance(task.getSubjectType()).
                            getMappingItems(task.getResource(), MappingPurpose.PROPAGATION)));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }

        return obj;
    }
}
