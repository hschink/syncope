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

import org.apache.syncope.console.pages.panels.RoleSummaryPanel;
import org.apache.syncope.console.wicket.markup.html.tree.TreeRolePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Roles WebPage.
 */
public class Roles extends BasePage {

    private static final long serialVersionUID = -2147758241610831969L;

    private ModalWindow createRoleWin = null;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 750;

    private final WebMarkupContainer container;

    public Roles(final PageParameters parameters) {
        super(parameters);

        createRoleWin = new ModalWindow("createRoleWin");
        createRoleWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleWin.setInitialHeight(WIN_HEIGHT);
        createRoleWin.setInitialWidth(WIN_WIDTH);
        createRoleWin.setCookieName("create-role-modal");
        add(createRoleWin);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final TreeRolePanel treePanel = new TreeRolePanel("treePanel");
        treePanel.setOutputMarkupId(true);
        container.add(treePanel);

        final RoleSummaryPanel nodePanel = new RoleSummaryPanel("summaryPanel", createRoleWin,
                Roles.this.getPageReference());
        
        nodePanel.setOutputMarkupId(true);
        container.add(nodePanel);

        createRoleWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {

                final TreeNodeClickUpdate data = new TreeNodeClickUpdate(target, nodePanel.getSelectedNode() == null
                        ? 0
                        : nodePanel.getSelectedNode().getId());

                send(getPage(), Broadcast.BREADTH, data);
                target.add(container);
                if (modalResult) {
                    getSession().info(getString("operation_succeded"));
                    target.add(feedbackPanel);
                    modalResult = false;
                }
            }
        });

        container.add(createRoleWin);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeClickUpdate) {

            final TreeNodeClickUpdate update = (TreeNodeClickUpdate) event.getPayload();

            final RoleSummaryPanel nodePanel = new RoleSummaryPanel("summaryPanel", createRoleWin,
                    Roles.this.getPageReference(), update.getSelectedNodeId());
            
            container.addOrReplace(nodePanel);
            update.getTarget().add(this);           
        }
    }

    public static class TreeNodeClickUpdate {

        private AjaxRequestTarget target;

        private Long selectedNodeId;

        public TreeNodeClickUpdate(final AjaxRequestTarget target, final Long selectedNodeId) {

            this.target = target;
            this.selectedNodeId = selectedNodeId;
        }

        /**
         * @return ajax request target
         */
        public AjaxRequestTarget getTarget() {
            return target;
        }

        public Long getSelectedNodeId() {
            return selectedNodeId;
        }

        public void setSelectedNodeId(final Long selectedNodeId) {
            this.selectedNodeId = selectedNodeId;
        }
    }
}
