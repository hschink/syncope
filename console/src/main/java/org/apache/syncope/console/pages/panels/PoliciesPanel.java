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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.BasePage;
import org.apache.syncope.console.pages.PolicyModalPage;
import org.apache.syncope.console.rest.PolicyRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoliciesPanel extends Panel {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PoliciesPanel.class);

    private static final int MODAL_WIN_HEIGHT = 400;

    private static final int MODAL_WIN_WIDTH = 1000;

    private static final long serialVersionUID = -6804066913177804275L;

    @SpringBean
    private PolicyRestClient policyRestClient;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    @SpringBean
    private PreferenceManager prefMan;

    private final PageReference pageRef;

    private final int paginatorRows = prefMan.getPaginatorRows(getWebRequest(), Constants.PREF_POLICY_PAGINATOR_ROWS);

    protected boolean modalResult = false;

    private final PolicyType policyType;

    public PoliciesPanel(final String id, final PageReference pageRef, final PolicyType policyType) {
        super(id);
        this.pageRef = pageRef;
        this.policyType = policyType;

        // Modal window for editing user attributes
        final ModalWindow mwindow = new ModalWindow("editModalWin");
        mwindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        mwindow.setInitialHeight(MODAL_WIN_HEIGHT);
        mwindow.setInitialWidth(MODAL_WIN_WIDTH);
        mwindow.setCookieName("policy-modal");
        add(mwindow);

        // Container for user list
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        setWindowClosedCallback(mwindow, container);

        final List<IColumn<AbstractPolicyTO, String>> columns = new ArrayList<IColumn<AbstractPolicyTO, String>>();

        columns.add(new PropertyColumn<AbstractPolicyTO, String>(new ResourceModel("id"), "id", "id"));

        columns.add(new PropertyColumn<AbstractPolicyTO, String>(
                new ResourceModel("description"), "description", "description"));

        columns.add(new AbstractColumn<AbstractPolicyTO, String>(new ResourceModel("type")) {

            private static final long serialVersionUID = 8263694778917279290L;

            @Override
            public void populateItem(final Item<ICellPopulator<AbstractPolicyTO>> cellItem, final String componentId,
                    final IModel<AbstractPolicyTO> model) {

                cellItem.add(new Label(componentId, getString(model.getObject().getType().name())));
            }
        });

        columns.add(new AbstractColumn<AbstractPolicyTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<AbstractPolicyTO>> cellItem, final String componentId,
                    final IModel<AbstractPolicyTO> model) {

                final AbstractPolicyTO policyTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, pageRef);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        mwindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            @Override
                            public Page createPage() {
                                return new PolicyModalPage(pageRef, mwindow, policyTO);
                            }
                        });

                        mwindow.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Policies");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            policyRestClient.delete(policyTO.getId(), policyTO.getClass());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.OPERATION_ERROR));

                            LOG.error("While deleting policy {}({})",
                                    policyTO.getId(), policyTO.getDescription(), e);
                        }

                        target.add(container);
                        ((NotificationPanel) getPage().get(Constants.FEEDBACK)).refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, "Policies");

                cellItem.add(panel);
            }
        });

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("datatable", columns,
                new PolicyDataProvider(), paginatorRows);

        container.add(table);

        final AjaxLink<Void> createButton = new ClearIndicatingAjaxLink<Void>("createLink", pageRef) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                mwindow.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    @Override
                    public Page createPage() {
                        return new PolicyModalPage(pageRef, mwindow, getPolicyTOInstance(policyType));
                    }
                });

                mwindow.show(target);
            }
        };

        add(createButton);

        MetaDataRoleAuthorizationStrategy.authorize(createButton, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Policies", "create"));

        @SuppressWarnings("rawtypes")
        final Form paginatorForm = new Form("PaginatorForm");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(), Constants.PREF_POLICY_PAGINATOR_ROWS, String
                        .valueOf(paginatorRows));
                table.setItemsPerPage(paginatorRows);

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    private void setWindowClosedCallback(final ModalWindow window, final WebMarkupContainer container) {
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(container);
                BasePage configuration = ((BasePage) pageRef.getPage());
                if (configuration.isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    configuration.getFeedbackPanel().refresh(target);
                    configuration.setModalResult(false);
                }
            }
        });
    }

    private class PolicyDataProvider extends SortableDataProvider<AbstractPolicyTO, String> {

        private static final long serialVersionUID = -6976327453925166730L;

        private final SortableDataProviderComparator<AbstractPolicyTO> comparator;

        public PolicyDataProvider() {
            super();

            //Default sorting
            setSort("description", SortOrder.ASCENDING);

            comparator = new SortableDataProviderComparator<AbstractPolicyTO>(this);
        }

        @Override
        public long size() {
            return policyRestClient.getPolicies(policyType, true).size();
        }

        @Override
        public Iterator<AbstractPolicyTO> iterator(final long first, final long count) {
            final List<AbstractPolicyTO> policies = policyRestClient.getPolicies(policyType, true);

            Collections.sort(policies, comparator);

            return policies.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public IModel<AbstractPolicyTO> model(final AbstractPolicyTO object) {
            return new CompoundPropertyModel<AbstractPolicyTO>(object);
        }
    }

    private AbstractPolicyTO getPolicyTOInstance(final PolicyType policyType) {
        AbstractPolicyTO policyTO;
        switch (policyType) {
            case GLOBAL_ACCOUNT:
                policyTO = new AccountPolicyTO(true);
                break;

            case ACCOUNT:
                policyTO = new AccountPolicyTO();
                break;

            case GLOBAL_PASSWORD:
                policyTO = new PasswordPolicyTO(true);
                break;

            case PASSWORD:
                policyTO = new PasswordPolicyTO();
                break;

            case GLOBAL_SYNC:
                policyTO = new SyncPolicyTO(true);
                break;

            case SYNC:
            default:
                policyTO = new SyncPolicyTO();
        }

        return policyTO;
    }
}
