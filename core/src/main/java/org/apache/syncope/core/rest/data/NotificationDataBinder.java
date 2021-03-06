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

import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.core.persistence.beans.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationDataBinder {

    private static final String[] IGNORE_PROPERTIES = { "id", "about", "recipients" };

    public NotificationTO getNotificationTO(final Notification notification) {
        NotificationTO result = new NotificationTO();

        BeanUtils.copyProperties(notification, result, IGNORE_PROPERTIES);

        result.setId(notification.getId());
        result.setUserAbout(notification.getUserAbout());
        result.setRoleAbout(notification.getRoleAbout());
        result.setRecipients(notification.getRecipients());

        return result;
    }

    public Notification create(final NotificationTO notificationTO) {
        Notification result = new Notification();
        update(result, notificationTO);
        return result;
    }

    public void update(final Notification notification, final NotificationTO notificationTO) {
        BeanUtils.copyProperties(notificationTO, notification, IGNORE_PROPERTIES);

        notification.setUserAbout(notificationTO.getUserAbout());
        notification.setRoleAbout(notificationTO.getRoleAbout());
        notification.setRecipients(notificationTO.getRecipients());
    }
}
