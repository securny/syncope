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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.UserDataProvider;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.BasePage;
import org.apache.syncope.console.pages.DisplayAttributesModalPage;
import org.apache.syncope.console.pages.EditUserModalPage;
import org.apache.syncope.console.pages.StatusModalPage;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.TokenColumn;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.UserAttrColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

public class ResultSetPanel extends Panel implements IEventSource {

    private static final long serialVersionUID = -9170191461250434024L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResultSetPanel.class);

    /**
     * Edit modal window height.
     */
    private final static int EDIT_MODAL_WIN_HEIGHT = 550;

    /**
     * Edit modal window width.
     */
    private final static int EDIT_MODAL_WIN_WIDTH = 800;

    /**
     * Schemas to be shown modal window height.
     */
    private final static int DISPLAYATTRS_MODAL_WIN_HEIGHT = 550;

    /**
     * Schemas to be shown modal window width.
     */
    private final static int DISPLAYATTRS_MODAL_WIN_WIDTH = 550;

    /**
     * Schemas to be shown modal window height.
     */
    private final static int STATUS_MODAL_WIN_HEIGHT = 500;

    /**
     * Schemas to be shown modal window width.
     */
    private final static int STATUS_MODAL_WIN_WIDTH = 500;

    /**
     * User rest client.
     */
    @SpringBean
    private UserRestClient restClient;

    /**
     * Application preferences.
     */
    @SpringBean
    private PreferenceManager prefMan;

    /**
     * Role reader for authorizations management.
     */
    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    /**
     * Number of rows per page.
     */
    private int rows;

    /**
     * Container used to refresh table.
     */
    final protected WebMarkupContainer container;

    /**
     * Feedback panel specified by the caller.
     */
    final private FeedbackPanel feedbackPanel;

    /**
     * Specify if results are about a filtered search or not. Using this attribute it is possible to use this panel to
     * show results about user list and user search.
     */
    private boolean filtered;

    /**
     * Filter used in case of filtered search.
     */
    private NodeCond filter;

    /**
     * Result table.
     */
    private AjaxFallbackDefaultDataTable<UserTO> resultTable;

    /**
     * Data provider used to search for users.
     */
    private UserDataProvider dataProvider;

    /**
     * Modal window to be used for user profile editing. Global visibility is required ...
     */
    private final ModalWindow editmodal = new ModalWindow("editModal");

    /**
     * Modal window to be used for attributes choosing to display in tables.
     */
    final ModalWindow displaymodal = new ModalWindow("displayModal");

    /**
     * Modal window to be used for user status management.
     */
    final ModalWindow statusmodal = new ModalWindow("statusModal");

    /**
     * Owner page.
     */
    private final BasePage page;

    public <T extends AbstractAttributableTO> ResultSetPanel(final String id, final boolean filtered,
            final NodeCond searchCond, final PageReference pageRef) {
        super(id);

        setOutputMarkupId(true);

        page = (BasePage) pageRef.getPage();

        this.filtered = filtered;
        this.filter = searchCond;
        this.feedbackPanel = page.getFeedbackPanel();

        editmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editmodal.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        editmodal.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        editmodal.setCookieName("edit-modal");
        add(editmodal);

        displaymodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        displaymodal.setInitialHeight(DISPLAYATTRS_MODAL_WIN_HEIGHT);
        displaymodal.setInitialWidth(DISPLAYATTRS_MODAL_WIN_WIDTH);
        displaymodal.setCookieName("display-modal");
        add(displaymodal);

        statusmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        statusmodal.setInitialHeight(STATUS_MODAL_WIN_HEIGHT);
        statusmodal.setInitialWidth(STATUS_MODAL_WIN_WIDTH);
        statusmodal.setCookieName("status-modal");
        add(statusmodal);

        // Container for user search result
        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // ---------------------------
        // Result table initialization
        // ---------------------------
        // preferences and container must be not null to use it ...
        rows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_USERS_PAGINATOR_ROWS);
        updateResultTable(false);
        // ---------------------------

        // ---------------------------
        // Link to select schemas/columns to be shown
        // ---------------------------
        final AjaxLink displayAttrs = new ClearIndicatingAjaxLink("displayAttrsLink", page.getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                displaymodal.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new DisplayAttributesModalPage(page.getPageReference(), displaymodal);
                    }
                });

                displaymodal.show(target);
            }
        };

        // Add class to specify relative position of the link.
        // Position depends on result pages number.
        displayAttrs.add(new Behavior() {

            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {

                if (resultTable.getRowCount() > rows) {
                    tag.remove("class");
                    tag.put("class", "settingsPosMultiPage");
                } else {
                    tag.remove("class");
                    tag.put("class", "settingsPos");
                }
            }
        });

        MetaDataRoleAuthorizationStrategy.authorize(
                displayAttrs, ENABLE, xmlRolesReader.getAllAllowedRoles("Users", "changeView"));

        container.add(displayAttrs);
        // ---------------------------

        // ---------------------------
        // Rows-per-page selector
        // ---------------------------
        final Form paginatorForm = new Form("paginator");
        container.add(paginatorForm);

        final DropDownChoice<Integer> rowsChooser = new DropDownChoice<Integer>(
                "rowsChooser", new PropertyModel(this, "rows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_USERS_PAGINATOR_ROWS, String.valueOf(rows));

                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(getParent(), Broadcast.BREADTH, data);
            }
        });
        paginatorForm.add(rowsChooser);
        // ---------------------------

        setWindowClosedReloadCallback(statusmodal);
        setWindowClosedReloadCallback(editmodal);
        setWindowClosedReloadCallback(displaymodal);
    }

    public void search(final NodeCond searchCond, final AjaxRequestTarget target) {

        this.filter = searchCond;
        dataProvider.setSearchCond(filter);
        target.add(container);
    }

    private void updateResultTable(final boolean create) {
        updateResultTable(create, rows);
    }

    private void updateResultTable(final boolean create, final int rows) {
        dataProvider = new UserDataProvider(restClient, rows, filtered);
        dataProvider.setSearchCond(filter);

        final int currentPage = resultTable != null
                ? (create
                ? resultTable.getPageCount() - 1
                : resultTable.getCurrentPage())
                : 0;

        resultTable = new AjaxFallbackDefaultDataTable<UserTO>("resultTable", getColumns(), dataProvider, rows);

        resultTable.setCurrentPage(currentPage);

        resultTable.setOutputMarkupId(true);

        container.addOrReplace(resultTable);
    }

    private List<IColumn<UserTO>> getColumns() {
        final List<IColumn<UserTO>> columns = new ArrayList<IColumn<UserTO>>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_DETAILS_VIEW)) {
            final Field field = ReflectionUtils.findField(UserTO.class, name);

            if ("token".equalsIgnoreCase(name)) {
                columns.add(new TokenColumn("token"));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new DatePropertyColumn<UserTO>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(new PropertyColumn(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW)) {
            columns.add(new UserAttrColumn(name, UserAttrColumn.SchemaType.schema));
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW)) {
            columns.add(new UserAttrColumn(name, UserAttrColumn.SchemaType.derivedSchema));
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_VIRTUAL_ATTRIBUTES_VIEW)) {
            columns.add(new UserAttrColumn(name, UserAttrColumn.SchemaType.virtualSchema));
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : DisplayAttributesModalPage.DEFAULT_SELECTION) {
                columns.add(new PropertyColumn(new ResourceModel(name, name), name, name));
            }

            prefMan.setList(getRequest(), getResponse(), Constants.PREF_USERS_DETAILS_VIEW,
                    DisplayAttributesModalPage.DEFAULT_SELECTION);
        }

        columns.add(new AbstractColumn<UserTO>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<UserTO>> cellItem, final String componentId,
                    final IModel<UserTO> model) {

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, page.getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        statusmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new StatusModalPage(page.getPageReference(), statusmodal, model.getObject());
                            }
                        });

                        statusmodal.show(target);
                    }
                }, ActionLink.ActionType.SEARCH, "Users", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                // SYNCOPE-294: re-read userTO before edit
                                UserTO userTO = restClient.read(model.getObject().getId());
                                return new EditUserModalPage(page.getPageReference(), editmodal, userTO);
                            }
                        });

                        editmodal.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Users", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            final UserTO userTO = restClient.delete(model.getObject().getId());

                            page.setModalResult(true);

                            editmodal.setPageCreator(new ModalWindow.PageCreator() {

                                private static final long serialVersionUID = -7834632442532690940L;

                                @Override
                                public Page createPage() {
                                    return new EditUserModalPage(editmodal, userTO);
                                }
                            });

                            editmodal.show(target);

                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(getString("operation_error") + ": " + scce.getMessage());
                            target.add(feedbackPanel);
                        }
                    }
                }, ActionLink.ActionType.DELETE, "Users", "delete");

                cellItem.add(panel);
            }
        });

        return columns;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof EventDataWrapper) {

            final EventDataWrapper data = (EventDataWrapper) event.getPayload();

            if (data.getRows() < 1) {
                updateResultTable(data.isCreate());
            } else {
                updateResultTable(data.isCreate(), data.getRows());
            }

            data.getTarget().add(container);
        }
    }

    private void setWindowClosedReloadCallback(final ModalWindow window) {
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(getParent(), Broadcast.BREADTH, data);

                if (page.isModalResult()) {
                    // reset modal result
                    page.setModalResult(false);
                    // set operation succeded
                    getSession().info(getString("operation_succeded"));
                    // refresh feedback panel
                    target.add(feedbackPanel);
                }
            }
        });
    }

    public static class EventDataWrapper {

        private AjaxRequestTarget target;

        private boolean create;

        private int rows;

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public void setTarget(final AjaxRequestTarget target) {
            this.target = target;
        }

        public boolean isCreate() {
            return create;
        }

        public void setCreate(boolean create) {
            this.create = create;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }
    }
}