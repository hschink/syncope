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
package org.apache.syncope.core.sync.impl;

import org.apache.syncope.core.sync.SyncProfile;
import org.apache.syncope.core.sync.SyncUtilities;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.AbstractSyncTask;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.sync.DefaultSyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple action for synchronizing LDAP groups memberships to Syncope role memberships, when the same resource is
 * configured for both users and roles.
 *
 * @see org.apache.syncope.core.propagation.impl.LDAPMembershipPropagationActions
 */
public class LDAPMembershipSyncActions extends DefaultSyncActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipSyncActions.class);

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private SyncUtilities syncUtilities;

    protected Map<Long, Long> membersBeforeRoleUpdate = Collections.<Long, Long>emptyMap();

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @param connector A Connector instance to query for the groupMemberAttribute property name
     * @return the name of the attribute used to keep track of group memberships
     */
    protected String getGroupMembershipAttrName(final Connector connector) {
        ConnInstance connInstance = connector.getActiveConnInstance();
        Iterator<ConnConfProperty> propertyIterator = connInstance.getConfiguration().iterator();
        String groupMembershipName = "uniquemember";
        while (propertyIterator.hasNext()) {
            ConnConfProperty property = propertyIterator.next();
            if ("groupMemberAttribute".equals(property.getSchema().getName())
                    && property.getValues() != null && !property.getValues().isEmpty()) {
                groupMembershipName = (String) property.getValues().get(0);
                break;
            }
        }

        return groupMembershipName;
    }

    /**
     * Keep track of members of the role being updated <b>before</b> actual update takes place. This is not needed on
     * <ul> <li>beforeProvision() - because the synchronizing role does not exist yet on Syncope</li> <li>beforeDelete()
     * -
     * because role delete cascades as membership removal for all users involved</li> </ul>
     *
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO, K extends AbstractAttributableMod> SyncDelta beforeUpdate(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final K subjectMod) throws JobExecutionException {

        if (subject instanceof RoleTO) {
            // search for all users assigned to given role
            SyncopeRole role = roleDAO.find(subject.getId());
            if (role != null) {
                List<Membership> membs = roleDAO.findMemberships(role);
                // save memberships before role update takes place
                membersBeforeRoleUpdate = new HashMap<Long, Long>(membs.size());
                for (Membership memb : membs) {
                    membersBeforeRoleUpdate.put(memb.getSyncopeUser().getId(), memb.getId());
                }
            }
        }

        return super.beforeUpdate(profile, delta, subject, subjectMod);
    }

    /**
     * Build UserMod for adding membership to given user, for given role.
     *
     * @param userId user to be assigned membership to given role
     * @param roleTO role for adding membership
     * @return UserMod for user update
     */
    protected UserMod getUserMod(final Long userId, final RoleTO roleTO) {
        UserMod userMod = new UserMod();
        // no actual modification takes place when user has already the role assigned
        if (membersBeforeRoleUpdate.containsKey(userId)) {
            membersBeforeRoleUpdate.remove(userId);
        } else {
            userMod.setId(userId);

            MembershipMod membershipMod = new MembershipMod();
            membershipMod.setRole(roleTO.getId());
            userMod.getMembershipsToAdd().add(membershipMod);
        }

        return userMod;
    }

    /**
     * Read values of attribute returned by getGroupMembershipAttrName(); if not present in the given delta, perform an
     * additional read on the underlying connector.
     *
     * @param delta representing the synchronizing role
     * @param connector associated to the current resource
     * @return value of attribute returned by
     * {@link #getGroupMembershipAttrName(org.apache.syncope.core.propagation.Connector) }
     */
    protected List<Object> getMembAttrValues(final SyncDelta delta, final Connector connector) {
        List<Object> result = Collections.<Object>emptyList();
        String groupMemberName = getGroupMembershipAttrName(connector);

        // first, try to read the configured attribute from delta, returned by the ongoing synchronization
        Attribute membAttr = delta.getObject().getAttributeByName(groupMemberName);
        // if not found, perform an additional read on the underlying connector for the same connector object
        if (membAttr == null) {
            final OperationOptionsBuilder oob = new OperationOptionsBuilder();
            oob.setAttributesToGet(groupMemberName);
            membAttr = connector.getObjectAttribute(ObjectClass.GROUP, delta.getUid(), oob.build(), groupMemberName);
        }
        if (membAttr != null && membAttr.getValue() != null) {
            result = membAttr.getValue();
        }

        return result;
    }

    /**
     * Perform actual modifications (i.e. membership add / remove) for the given role on the given resource.
     *
     * @param userMod modifications to perform on the user
     * @param resourceName resource to be propagated for changes
     */
    protected void userUpdate(final UserMod userMod, final String resourceName) {
        if (userMod.getId() == 0) {
            return;
        }

        Result result;

        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = null;

        try {
            updated = uwfAdapter.update(userMod);

            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    updated, false, Collections.singleton(resourceName));

            taskExecutor.execute(tasks);
            result = Result.SUCCESS;
        } catch (PropagationException e) {
            result = Result.FAILURE;
            LOG.error("Could not propagate {}", userMod, e);
        } catch (Exception e) {
            result = Result.FAILURE;
            LOG.error("Could not perform update {}", userMod, e);
        }

        notificationManager.createTasks(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                this.getClass().getSimpleName(),
                null,
                "update",
                result,
                null, // searching for before object is too much expensive ... 
                updated == null ? null : updated.getResult().getKey(),
                userMod,
                resourceName);

        auditManager.audit(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                this.getClass().getSimpleName(),
                null,
                "update",
                result,
                null, // searching for before object is too much expensive ... 
                updated == null ? null : updated.getResult().getKey(),
                userMod,
                resourceName);
    }

    /**
     * Synchronize Syncope memberships with the situation read on the external resource's group.
     *
     * @param profile sync profile
     * @param delta representing the synchronizing role
     * @param roleTO role after modification performed by the handler
     * @throws JobExecutionException if anything goes wrong
     */
    protected void synchronizeMemberships(
            final SyncProfile<?, ?> profile, final SyncDelta delta, final RoleTO roleTO) throws JobExecutionException {

        final AbstractSyncTask task = profile.getSyncTask();
        final ExternalResource resource = task.getResource();
        final Connector connector = profile.getConnector();

        for (Object membValue : getMembAttrValues(delta, connector)) {
            Long userId = syncUtilities.findMatchingAttributableId(
                    ObjectClass.ACCOUNT,
                    membValue.toString(),
                    profile.getSyncTask().getResource(),
                    profile.getConnector());
            if (userId != null) {
                UserMod userMod = getUserMod(userId, roleTO);
                userUpdate(userMod, resource.getName());
            }
        }

        // finally remove any residual membership that was present before role update but not any more
        for (Map.Entry<Long, Long> member : membersBeforeRoleUpdate.entrySet()) {
            UserMod userMod = new UserMod();
            userMod.setId(member.getKey());
            userMod.getMembershipsToRemove().add(member.getValue());
            userUpdate(userMod, resource.getName());
        }
    }

    /**
     * Synchronize membership at role synchronization time (because SyncJob first synchronize users then roles).
     * {@inheritDoc}
     */
    @Override
    public <T extends AbstractAttributableTO> void after(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final SyncResult result) throws JobExecutionException {
        if (!(profile.getSyncTask() instanceof SyncTask)) {
            return;
        }

        if (!(subject instanceof RoleTO) || profile.getSyncTask().getResource().getUmapping() == null) {
            super.after(profile, delta, subject, result);
        } else {
            synchronizeMemberships(profile, delta, (RoleTO) subject);
        }
    }
}
