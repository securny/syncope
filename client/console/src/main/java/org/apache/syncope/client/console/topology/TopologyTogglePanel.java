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
package org.apache.syncope.client.console.topology;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.ConnObjects;
import org.apache.syncope.client.console.wizards.resources.ConnectorWizardBuilder;
import org.apache.syncope.client.console.wizards.resources.ResourceWizardBuilder;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.status.ResourceStatusModal;
import org.apache.syncope.client.console.tasks.PropagationTasks;
import org.apache.syncope.client.console.tasks.PushTasks;
import org.apache.syncope.client.console.tasks.SchedTasks;
import org.apache.syncope.client.console.tasks.PullTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.resources.ResourceProvisionPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class TopologyTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -2025535531121434056L;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final WebMarkupContainer container;

    protected final BaseModal<Serializable> propTaskModal;

    protected final BaseModal<Serializable> schedTaskModal;

    protected final BaseModal<Serializable> provisionModal;

    public TopologyTogglePanel(final String id, final PageReference pageRef) {
        super(id, pageRef);

        modal.size(Modal.Size.Large);
        setFooterVisibility(false);
        setWindowClosedReloadCallback(modal);

        propTaskModal = new BaseModal<>("outer");
        propTaskModal.size(Modal.Size.Large);
        addOuterObject(propTaskModal);

        schedTaskModal = new BaseModal<Serializable>("outer") {

            private static final long serialVersionUID = 389935548143327858L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        schedTaskModal.size(Modal.Size.Large);
        addOuterObject(schedTaskModal);

        provisionModal = new BaseModal<>("outer");
        provisionModal.size(Modal.Size.Large);
        provisionModal.addSubmitButton();
        addOuterObject(provisionModal);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        container.add(getEmptyFragment());
    }

    public void toggleWithContent(final AjaxRequestTarget target, final TopologyNode node) {
        setHeader(target, node.getDisplayName());

        switch (node.getKind()) {
            case SYNCOPE:
                container.addOrReplace(getSyncopeFragment(pageRef));
                break;
            case CONNECTOR_SERVER:
                container.addOrReplace(getLocationFragment(node, pageRef));
                break;
            case FS_PATH:
                container.addOrReplace(getLocationFragment(node, pageRef));
                break;
            case CONNECTOR:
                container.addOrReplace(getConnectorFragment(node, pageRef));
                break;
            case RESOURCE:
                container.addOrReplace(getResurceFragment(node, pageRef));
                break;
            default:
                container.addOrReplace(getEmptyFragment());
        }

        target.add(container);

        this.toggle(target, true);
    }

    private Fragment getEmptyFragment() {
        return new Fragment("actions", "emptyFragment", this);
    }

    private Fragment getSyncopeFragment(final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "syncopeActions", this);

        AjaxLink<String> reload = new IndicatingOnConfirmAjaxLink<String>("reload", "connectors.confirm.reload", true) {

            private static final long serialVersionUID = -2075933173666007020L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.reload();
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While reloading all connectors", e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        fragment.add(reload);
        MetaDataRoleAuthorizationStrategy.authorize(reload, RENDER, StandardEntitlement.CONNECTOR_RELOAD);

        AjaxLink<String> tasks = new IndicatingAjaxLink<String>("tasks") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(propTaskModal.setContent(new SchedTasks(propTaskModal, pageRef)));
                propTaskModal.header(new ResourceModel("task.custom.list"));
                propTaskModal.show(true);
            }
        };
        fragment.add(tasks);
        MetaDataRoleAuthorizationStrategy.authorize(tasks, RENDER, StandardEntitlement.TASK_LIST);

        return fragment;
    }

    private Fragment getLocationFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "locationActions", this);

        AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = new ConnInstanceTO();
                modelObject.setLocation(node.getKey().toString());

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(modelObject, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.getForm(), RENDER, StandardEntitlement.CONNECTOR_CREATE);

                modal.show(true);
            }
        };
        fragment.add(create);
        MetaDataRoleAuthorizationStrategy.authorize(create, RENDER, StandardEntitlement.CONNECTOR_CREATE);

        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "connectorActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<String>("delete", true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.delete(String.class.cast(node.getKey()));
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                            getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, RENDER, StandardEntitlement.CONNECTOR_DELETE);
        fragment.add(delete);

        AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(String.class.cast(node.getKey()));
                modelObject.setConnectorDisplayName(node.getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(modelObject, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.getForm(), RENDER, StandardEntitlement.RESOURCE_CREATE);

                modal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(create, RENDER, StandardEntitlement.RESOURCE_CREATE);
        fragment.add(create);

        AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = connectorRestClient.read(String.class.cast(node.getKey()));

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(modelObject, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.edit"), node.getDisplayName())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.getForm(), RENDER, StandardEntitlement.CONNECTOR_UPDATE);

                modal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, StandardEntitlement.CONNECTOR_UPDATE);
        fragment.add(edit);

        return fragment;
    }

    private Fragment getResurceFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "resourceActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<String>("delete", true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    resourceRestClient.delete(node.getKey().toString());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                            getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, RENDER, StandardEntitlement.RESOURCE_DELETE);
        fragment.add(delete);

        AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO modelObject = resourceRestClient.read(node.getKey().toString());

                IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(modelObject, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.getForm(), RENDER, StandardEntitlement.RESOURCE_UPDATE);

                modal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, StandardEntitlement.RESOURCE_UPDATE);
        fragment.add(edit);

        AjaxLink<String> status = new IndicatingAjaxLink<String>("status") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO modelObject = resourceRestClient.read(node.getKey().toString());
                target.add(propTaskModal.setContent(
                        new ResourceStatusModal(propTaskModal, pageRef, modelObject)));
                propTaskModal.header(new ResourceModel("resource.provisioning.status", "Provisioning Status"));
                propTaskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(status, RENDER, StandardEntitlement.USER_UPDATE);
        fragment.add(status);

        AjaxLink<String> provision = new IndicatingAjaxLink<String>("provision") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO modelObject = resourceRestClient.read(node.getKey().toString());

                IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                provisionModal.setFormModel(model);

                target.add(provisionModal.setContent(new ResourceProvisionPanel(provisionModal, modelObject, pageRef)));

                provisionModal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(provisionModal.getForm(), RENDER, StandardEntitlement.RESOURCE_UPDATE);

                provisionModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, StandardEntitlement.RESOURCE_UPDATE);
        fragment.add(provision);

        AjaxLink<String> explore = new IndicatingAjaxLink<String>("explore") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(propTaskModal.setContent(new ConnObjects(propTaskModal, node.getKey().toString(), pageRef)));
                propTaskModal.header(new StringResourceModel("resource.explore.list", Model.of(node)));
                propTaskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(explore, RENDER, StandardEntitlement.RESOURCE_LIST_CONNOBJECT);
        fragment.add(explore);

        AjaxLink<String> propagation = new IndicatingAjaxLink<String>("propagation") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            @SuppressWarnings("unchecked")
            public void onClick(final AjaxRequestTarget target) {
                target.add(propTaskModal.setContent(
                        new PropagationTasks(propTaskModal, node.getKey().toString(), pageRef)));
                propTaskModal.header(new ResourceModel("task.propagation.list"));
                propTaskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(propagation, RENDER, StandardEntitlement.TASK_LIST);
        fragment.add(propagation);

        AjaxLink<String> pull = new IndicatingAjaxLink<String>("pull") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new PullTasks(schedTaskModal, pageRef, node.getKey().toString())));
                schedTaskModal.header(new ResourceModel("task.pull.list"));
                schedTaskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(pull, RENDER, StandardEntitlement.TASK_LIST);
        fragment.add(pull);

        AjaxLink<String> push = new IndicatingAjaxLink<String>("push") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new PushTasks(schedTaskModal, pageRef, node.getKey().toString())));
                schedTaskModal.header(new ResourceModel("task.push.list"));
                schedTaskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(push, RENDER, StandardEntitlement.TASK_LIST);
        fragment.add(push);

        return fragment;
    }
}
