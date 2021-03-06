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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.core.persistence.validation.entity.NotificationCheck;

@NotificationCheck
@Entity
public class Notification extends AbstractBaseBean {

    private static final long serialVersionUID = 3112582296912757537L;

    @Id
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "events")
    private List<String> events;

    private String userAbout;

    private String roleAbout;

    private String recipients;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "staticRecipients")
    private List<String> staticRecipients;

    @NotNull
    @Enumerated(EnumType.STRING)
    private IntMappingType recipientAttrType;

    @NotNull
    private String recipientAttrName;

    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer selfAsRecipient;

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    private String template;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TraceLevel traceLevel;
    
    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer active;

    public Notification() {
        events = new ArrayList<String>();
        staticRecipients = new ArrayList<String>();
        selfAsRecipient = getBooleanAsInteger(false);
        active = getBooleanAsInteger(true);
        traceLevel = TraceLevel.ALL;
    }

    public Long getId() {
        return id;
    }

    public String getUserAbout() {
        return userAbout;
    }

    public void setUserAbout(final String userAbout) {
        this.userAbout = userAbout;
    }

    public String getRoleAbout() {
        return roleAbout;
    }

    public void setRoleAbout(final String roleAbout) {
        this.roleAbout = roleAbout;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getRecipientAttrName() {
        return recipientAttrName;
    }

    public void setRecipientAttrName(String recipientAttrName) {
        this.recipientAttrName = recipientAttrName;
    }

    public IntMappingType getRecipientAttrType() {
        return recipientAttrType;
    }

    public void setRecipientAttrType(IntMappingType recipientAttrType) {
        this.recipientAttrType = recipientAttrType;
    }

    public List<String> getEvents() {
        return events;
    }

    public boolean addEvent(final String event) {
        return event != null && !events.contains(event) && events.add(event);
    }

    public boolean removeEvent(final String event) {
        return event != null && events.remove(event);
    }

    public void setEvents(List<String> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
    }

    public List<String> getStaticRecipients() {
        return staticRecipients;
    }

    public boolean addStaticRecipient(String staticRecipient) {
        return staticRecipient != null && !staticRecipients.contains(staticRecipient) && staticRecipients.add(staticRecipient);
    }

    public boolean removeStaticRecipient(String staticRecipient) {
        return staticRecipient != null && staticRecipients.remove(staticRecipient);
    }

    public void setStaticRecipients(List<String> staticRecipients) {
        this.staticRecipients.clear();
        if (staticRecipients != null) {
            this.staticRecipients.addAll(staticRecipients);
        }
    }

    public boolean isSelfAsRecipient() {
        return isBooleanAsInteger(selfAsRecipient);
    }

    public void setSelfAsRecipient(final boolean selfAsRecipient) {
        this.selfAsRecipient = getBooleanAsInteger(selfAsRecipient);
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    public boolean isActive() {
        return isBooleanAsInteger(active);
    }

    public void setActive(final boolean active) {
        this.active = getBooleanAsInteger(active);
    }
}
