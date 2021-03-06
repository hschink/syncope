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
package org.apache.syncope.console.pages;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.console.commons.AttrLayoutType;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.HttpResourceStream;
import org.apache.syncope.console.commons.Mode;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.pages.panels.AttributesPanel;
import org.apache.syncope.console.pages.panels.LayoutsPanel;
import org.apache.syncope.console.pages.panels.PoliciesPanel;
import org.apache.syncope.console.rest.ConfigurationRestClient;
import org.apache.syncope.console.rest.LoggerRestClient;
import org.apache.syncope.console.rest.NotificationRestClient;
import org.apache.syncope.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.console.rest.WorkflowRestClient;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.console.wicket.markup.html.link.VeilPopupSettings;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Configurations WebPage.
 */
public class Configuration extends BasePage {

    private static final long serialVersionUID = -2838270869037702214L;

    private static final int NOTIFICATION_WIN_HEIGHT = 500;

    private static final int NOTIFICATION_WIN_WIDTH = 1100;

    private static final int SECURITY_QUESTION_WIN_HEIGHT = 300;

    private static final int SECURITY_QUESTION_WIN_WIDTH = 900;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SpringBean
    private LoggerRestClient loggerRestClient;

    @SpringBean
    private NotificationRestClient notificationRestClient;

    @SpringBean
    private SecurityQuestionRestClient securityQuestionRestClient;

    @SpringBean
    private WorkflowRestClient wfRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createNotificationWin;

    private final ModalWindow editNotificationWin;

    private final ModalWindow createSecurityQuestionWin;

    private final ModalWindow editSecurityQuestionWin;

    private WebMarkupContainer notificationContainer;

    private WebMarkupContainer securityQuestionContainer;

    private int notificationPaginatorRows;

    public Configuration() {
        super();

        setupSyncopeConf();

        add(new PoliciesPanel("passwordPoliciesPanel", getPageReference(), PolicyType.PASSWORD));
        add(new PoliciesPanel("accountPoliciesPanel", getPageReference(), PolicyType.ACCOUNT));
        add(new PoliciesPanel("syncPoliciesPanel", getPageReference(), PolicyType.SYNC));

        add(createNotificationWin = new ModalWindow("createNotificationWin"));
        add(editNotificationWin = new ModalWindow("editNotificationWin"));
        setupNotification();

        add(createSecurityQuestionWin = new ModalWindow("createSecurityQuestionWin"));
        add(editSecurityQuestionWin = new ModalWindow("editSecurityQuestionWin"));
        setupSecurityQuestion();

        // Workflow definition stuff
        WebMarkupContainer noActivitiEnabledForUsers = new WebMarkupContainer("noActivitiEnabledForUsers");
        noActivitiEnabledForUsers.setOutputMarkupPlaceholderTag(true);
        add(noActivitiEnabledForUsers);

        WebMarkupContainer workflowDefContainer = new WebMarkupContainer("workflowDefContainer");
        workflowDefContainer.setOutputMarkupPlaceholderTag(true);

        if (wfRestClient.isActivitiEnabledForUsers()) {
            noActivitiEnabledForUsers.setVisible(false);
        } else {
            workflowDefContainer.setVisible(false);
        }

        BookmarkablePageLink<Void> activitiModeler =
                new BookmarkablePageLink<Void>("activitiModeler", ActivitiModelerPopupPage.class);
        activitiModeler.setPopupSettings(new VeilPopupSettings().setHeight(600).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(activitiModeler, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "workflowDefRead"));
        workflowDefContainer.add(activitiModeler);
        // Check if Activiti Modeler directory is found
        boolean activitiModelerEnabled = false;
        try {
            String activitiModelerDirectory = WebApplicationContextUtils.getWebApplicationContext(
                    WebApplication.get().getServletContext()).getBean("activitiModelerDirectory", String.class);
            File baseDir = new File(activitiModelerDirectory);
            activitiModelerEnabled = baseDir.exists() && baseDir.canRead() && baseDir.isDirectory();
        } catch (Exception e) {
            LOG.error("Could not check for Activiti Modeler directory", e);
        }
        activitiModeler.setEnabled(activitiModelerEnabled);

        BookmarkablePageLink<Void> xmlEditor =
                new BookmarkablePageLink<Void>("xmlEditor", XMLEditorPopupPage.class);
        xmlEditor.setPopupSettings(new VeilPopupSettings().setHeight(480).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditor, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "workflowDefRead"));
        workflowDefContainer.add(xmlEditor);

        Image workflowDefDiagram = new Image("workflowDefDiagram", new Model()) {

            private static final long serialVersionUID = -8457850449086490660L;

            @Override
            protected IResource getImageResource() {
                return new DynamicImageResource() {

                    private static final long serialVersionUID = 923201517955737928L;

                    @Override
                    protected byte[] getImageData(final IResource.Attributes attributes) {
                        return wfRestClient.isActivitiEnabledForUsers()
                                ? wfRestClient.getDiagram()
                                : new byte[0];
                    }
                };
            }

        };
        workflowDefContainer.add(workflowDefDiagram);

        MetaDataRoleAuthorizationStrategy.authorize(workflowDefContainer, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "workflowDefRead"));
        add(workflowDefContainer);

        // Logger stuff
        PropertyListView<LoggerTO> coreLoggerList =
                new LoggerPropertyList(null, "corelogger", loggerRestClient.listLogs());
        WebMarkupContainer coreLoggerContainer = new WebMarkupContainer("coreLoggerContainer");
        coreLoggerContainer.add(coreLoggerList);
        coreLoggerContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(coreLoggerContainer, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "logList"));
        add(coreLoggerContainer);

        ConsoleLoggerController consoleLoggerController = new ConsoleLoggerController();
        PropertyListView<LoggerTO> consoleLoggerList =
                new LoggerPropertyList(consoleLoggerController, "consolelogger", consoleLoggerController.getLoggers());
        WebMarkupContainer consoleLoggerContainer = new WebMarkupContainer("consoleLoggerContainer");
        consoleLoggerContainer.add(consoleLoggerList);
        consoleLoggerContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(consoleLoggerContainer, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "logList"));
        add(consoleLoggerContainer);

        add(new LayoutsPanel("adminUserLayoutPanel", AttrLayoutType.ADMIN_USER, feedbackPanel));
        add(new LayoutsPanel("selfUserLayoutPanel", AttrLayoutType.SELF_USER, feedbackPanel));
        add(new LayoutsPanel("adminRoleLayoutPanel", AttrLayoutType.ADMIN_ROLE, feedbackPanel));
        add(new LayoutsPanel("selfRoleLayoutPanel", AttrLayoutType.SELF_ROLE, feedbackPanel));
        add(new LayoutsPanel("adminMembershipLayoutPanel", AttrLayoutType.ADMIN_MEMBERSHIP, feedbackPanel));
        add(new LayoutsPanel("selfMembershipLayoutPanel", AttrLayoutType.SELF_MEMBERSHIP, feedbackPanel));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setupSyncopeConf() {
        WebMarkupContainer parameters = new WebMarkupContainer("parameters");
        add(parameters);
        MetaDataRoleAuthorizationStrategy.authorize(parameters, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "list"));

        final ConfTO conf = confRestClient.list();

        for (Iterator<AttributeTO> it = conf.getAttrs().iterator(); it.hasNext();) {
            AttributeTO attr = it.next();
            for (AttrLayoutType type : AttrLayoutType.values()) {
                if (type.getConfKey().equals(attr.getSchema())) {
                    it.remove();
                }
            }
        }

        final Form<?> form = new Form<Void>("confForm");
        form.setModel(new CompoundPropertyModel(conf));
        parameters.add(form);

        form.add(new AttributesPanel("parameters", conf, form, Mode.ADMIN));

        IndicatingAjaxLink<Void> save = new IndicatingAjaxLink<Void>("saveParameters") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConfTO updatedConf = (ConfTO) form.getModelObject();

                try {
                    for (AttributeTO attr : updatedConf.getAttrs()) {
                        if (attr.getValues().isEmpty()
                                || attr.getValues().equals(Collections.singletonList(StringUtils.EMPTY))) {

                            confRestClient.delete(attr.getSchema());
                        } else {
                            confRestClient.set(attr);
                        }
                    }

                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                } catch (Exception e) {
                    LOG.error("While updating configuration parameters", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(save, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "set"));
        form.add(save);

        Link<Void> dbExportLink = new Link<Void>("dbExportLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    HttpResourceStream stream = new HttpResourceStream(confRestClient.dbExport());

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(stream.getFilename() == null ? "content.xml" : stream.getFilename());
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(dbExportLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "export"));
        add(dbExportLink);
    }

    private void setupNotification() {
        notificationPaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_NOTIFICATION_PAGINATOR_ROWS);

        final List<IColumn<NotificationTO, String>> notificationCols = new ArrayList<IColumn<NotificationTO, String>>();
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("id"), "id", "id"));
        notificationCols.add(new CollectionPropertyColumn<NotificationTO>(
                new ResourceModel("events"), "events", "events"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("subject"), "subject", "subject"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("template"), "template", "template"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("traceLevel"), "traceLevel", "traceLevel"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("active"), "active", "active"));

        notificationCols.add(new AbstractColumn<NotificationTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<NotificationTO>> cellItem, final String componentId,
                    final IModel<NotificationTO> model) {

                final NotificationTO notificationTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editNotificationWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new NotificationModalPage(Configuration.this.getPageReference(),
                                        editNotificationWin, notificationTO, false);
                            }
                        });

                        editNotificationWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Notification");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            notificationRestClient.delete(notificationTO.getId());
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting a notification", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        feedbackPanel.refresh(target);
                        target.add(notificationContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Notification");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<NotificationTO, String> notificationTable =
                new AjaxFallbackDefaultDataTable<NotificationTO, String>(
                        "notificationTable", notificationCols, new NotificationProvider(), notificationPaginatorRows);

        notificationContainer = new WebMarkupContainer("notificationContainer");
        notificationContainer.add(notificationTable);
        notificationContainer.setOutputMarkupId(true);

        add(notificationContainer);

        createNotificationWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createNotificationWin.setInitialHeight(NOTIFICATION_WIN_HEIGHT);
        createNotificationWin.setInitialWidth(NOTIFICATION_WIN_WIDTH);
        createNotificationWin.setCookieName("create-notification-modal");

        editNotificationWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editNotificationWin.setInitialHeight(NOTIFICATION_WIN_HEIGHT);
        editNotificationWin.setInitialWidth(NOTIFICATION_WIN_WIDTH);
        editNotificationWin.setCookieName("edit-notification-modal");

        setWindowClosedCallback(createNotificationWin, notificationContainer);
        setWindowClosedCallback(editNotificationWin, notificationContainer);

        AjaxLink<Void> createNotificationLink = new AjaxLink<Void>("createNotificationLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                createNotificationWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new NotificationModalPage(Configuration.this.getPageReference(), createNotificationWin,
                                new NotificationTO(), true);
                    }
                });

                createNotificationWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createNotificationLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Notification", "create"));
        add(createNotificationLink);

        @SuppressWarnings("rawtypes")
        Form notificationPaginatorForm = new Form("notificationPaginatorForm");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this,
                "notificationPaginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_NOTIFICATION_PAGINATOR_ROWS, String.valueOf(
                        notificationPaginatorRows));
                notificationTable.setItemsPerPage(notificationPaginatorRows);

                target.add(notificationContainer);
            }
        });

        notificationPaginatorForm.add(rowsChooser);
        add(notificationPaginatorForm);
    }

    private void setupSecurityQuestion() {
        final List<IColumn<SecurityQuestionTO, String>> securityQuestionCols =
                new ArrayList<IColumn<SecurityQuestionTO, String>>();
        securityQuestionCols.add(new PropertyColumn<SecurityQuestionTO, String>(
                new ResourceModel("id"), "id", "id"));
        securityQuestionCols.add(new PropertyColumn<SecurityQuestionTO, String>(
                new ResourceModel("content"), "content", "content"));

        securityQuestionCols.add(new AbstractColumn<SecurityQuestionTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<SecurityQuestionTO>> cellItem, final String componentId,
                    final IModel<SecurityQuestionTO> model) {

                final SecurityQuestionTO securityQuestionTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editSecurityQuestionWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new SecurityQuestionModalPage(Configuration.this.getPageReference(),
                                        editSecurityQuestionWin, securityQuestionTO, false);
                            }
                        });

                        editSecurityQuestionWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "SecurityQuestion");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            securityQuestionRestClient.delete(securityQuestionTO.getId());
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting a security question", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        feedbackPanel.refresh(target);
                        target.add(securityQuestionContainer);
                    }
                }, ActionLink.ActionType.DELETE, "SecurityQuestion");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<SecurityQuestionTO, String> securityQuestionTable =
                new AjaxFallbackDefaultDataTable<SecurityQuestionTO, String>("securityQuestionTable",
                        securityQuestionCols, new SecurityQuestionProvider(), 50);

        securityQuestionContainer = new WebMarkupContainer("securityQuestionContainer");
        securityQuestionContainer.add(securityQuestionTable);
        securityQuestionContainer.setOutputMarkupId(true);

        add(securityQuestionContainer);

        createSecurityQuestionWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createSecurityQuestionWin.setInitialHeight(SECURITY_QUESTION_WIN_HEIGHT);
        createSecurityQuestionWin.setInitialWidth(SECURITY_QUESTION_WIN_WIDTH);
        createSecurityQuestionWin.setCookieName("create-security-question-modal");

        editSecurityQuestionWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editSecurityQuestionWin.setInitialHeight(SECURITY_QUESTION_WIN_HEIGHT);
        editSecurityQuestionWin.setInitialWidth(SECURITY_QUESTION_WIN_WIDTH);
        editSecurityQuestionWin.setCookieName("edit-security-question-modal");

        setWindowClosedCallback(createSecurityQuestionWin, securityQuestionContainer);
        setWindowClosedCallback(editSecurityQuestionWin, securityQuestionContainer);

        AjaxLink<Void> createSecurityQuestionLink = new AjaxLink<Void>("createSecurityQuestionLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                createSecurityQuestionWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new SecurityQuestionModalPage(Configuration.this.getPageReference(),
                                createSecurityQuestionWin, new SecurityQuestionTO(), true);
                    }
                });

                createSecurityQuestionWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createSecurityQuestionLink, ENABLE, xmlRolesReader.
                getAllAllowedRoles("SecurityQuestion", "create"));
        add(createSecurityQuestionLink);
    }

    private class NotificationProvider extends SortableDataProvider<NotificationTO, String> {

        private static final long serialVersionUID = -276043813563988590L;

        private final SortableDataProviderComparator<NotificationTO> comparator;

        public NotificationProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<NotificationTO>(this);
        }

        @Override
        public Iterator<NotificationTO> iterator(final long first, final long count) {
            List<NotificationTO> list = notificationRestClient.getAllNotifications();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return notificationRestClient.getAllNotifications().size();
        }

        @Override
        public IModel<NotificationTO> model(final NotificationTO notification) {
            return new AbstractReadOnlyModel<NotificationTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public NotificationTO getObject() {
                    return notification;
                }
            };
        }
    }

    private class SecurityQuestionProvider extends SortableDataProvider<SecurityQuestionTO, String> {

        private static final long serialVersionUID = -1458398823626281188L;

        private final SortableDataProviderComparator<SecurityQuestionTO> comparator;

        public SecurityQuestionProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<SecurityQuestionTO>(this);
        }

        @Override
        public Iterator<SecurityQuestionTO> iterator(final long first, final long count) {
            List<SecurityQuestionTO> list = securityQuestionRestClient.list();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return securityQuestionRestClient.list().size();
        }

        @Override
        public IModel<SecurityQuestionTO> model(final SecurityQuestionTO securityQuestionTO) {
            return new AbstractReadOnlyModel<SecurityQuestionTO>() {

                private static final long serialVersionUID = 5079291243768775704L;

                @Override
                public SecurityQuestionTO getObject() {
                    return securityQuestionTO;
                }
            };
        }
    }

    private class LoggerPropertyList extends PropertyListView<LoggerTO> {

        private static final long serialVersionUID = 5911412425994616111L;

        private final ConsoleLoggerController consoleLoggerController;

        public LoggerPropertyList(final ConsoleLoggerController consoleLoggerController, final String id,
                final List<? extends LoggerTO> list) {

            super(id, list);
            this.consoleLoggerController = consoleLoggerController;
        }

        @Override
        protected void populateItem(final ListItem<LoggerTO> item) {
            item.add(new Label("name"));

            DropDownChoice<LoggerLevel> level = new DropDownChoice<LoggerLevel>("level");
            level.setModel(new IModel<LoggerLevel>() {

                private static final long serialVersionUID = -2350428186089596562L;

                @Override
                public LoggerLevel getObject() {
                    return item.getModelObject().getLevel();
                }

                @Override
                public void setObject(final LoggerLevel object) {
                    item.getModelObject().setLevel(object);
                }

                @Override
                public void detach() {
                }
            });
            level.setChoices(Arrays.asList(LoggerLevel.values()));
            level.setOutputMarkupId(true);
            level.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    try {
                        if (getId().equals("corelogger")) {
                            loggerRestClient.setLogLevel(item.getModelObject().getName(),
                                    item.getModelObject().getLevel());
                        } else {
                            consoleLoggerController.setLogLevel(item.getModelObject().getName(),
                                    item.getModelObject().getLevel());
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                    } catch (SyncopeClientException e) {
                        info(getString(Constants.OPERATION_ERROR));
                    }

                    feedbackPanel.refresh(target);
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(level, ENABLE, xmlRolesReader.getAllAllowedRoles(
                    "Configuration", "logSetLevel"));

            item.add(level);
        }
    }

    private static class ConsoleLoggerController implements Serializable {

        private static final long serialVersionUID = -1550459341476431714L;

        public List<LoggerTO> getLoggers() {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            List<LoggerTO> result = new ArrayList<LoggerTO>();
            for (LoggerConfig logger : ctx.getConfiguration().getLoggers().values()) {
                final String loggerName = LogManager.ROOT_LOGGER_NAME.equals(logger.getName())
                        ? SyncopeConstants.ROOT_LOGGER : logger.getName();
                if (logger.getLevel() != null) {
                    LoggerTO loggerTO = new LoggerTO();
                    loggerTO.setName(loggerName);
                    loggerTO.setLevel(LoggerLevel.fromLevel(logger.getLevel()));
                    result.add(loggerTO);
                }
            }

            return result;
        }

        public void setLogLevel(final String name, final LoggerLevel level) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            LoggerConfig logConf = SyncopeConstants.ROOT_LOGGER.equals(name)
                    ? ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
                    : ctx.getConfiguration().getLoggerConfig(name);
            logConf.setLevel(level.getLevel());
            ctx.updateLoggers();
        }
    }
}
