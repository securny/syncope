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
package org.apache.syncope.fit.console;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class NotificationsITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
        TESTER.assertRenderedPage(Notifications.class);
    }

    private void createNotification(final String sender, final String subject) {
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");

        formTester.setValue("content:form:view:recipientAttrName:textField", "email");
        formTester.select("content:form:view:template:dropDownChoiceField", 2);
        formTester.select("content:form:view:traceLevel:dropDownChoiceField", 0);
        formTester.setValue("content:form:view:sender:textField", sender);
        formTester.setValue("content:form:view:subject:textField", subject);

        TESTER.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        TESTER.assertNoErrorMessage();

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");

        // -------------------------------
        // generate event to populate eventsPanel
        // -------------------------------
        formTester.setValue("content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField", "0");
        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:"
                + "content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField",
                Constants.ON_CHANGE);
        // -------------------------------

        // -------------------------------
        // select event template
        // -------------------------------
        formTester.setValue("content:form:view:eventSelection:eventsContainer:eventsPanel:successGroup", "check0");
        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:"
                + "form:view:eventSelection:eventsContainer:eventsPanel:successGroup",
                Constants.ON_CLICK);
        // -------------------------------

        formTester.setValue("content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField", "0");
        formTester.setValue("content:form:view:eventSelection:eventsContainer:eventsPanel:successGroup", "check0");

        TESTER.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        TESTER.assertNoErrorMessage();

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        TESTER.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:form:"
                + "view:staticRecipients:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);
        formTester.setValue("content:form:view:staticRecipients:multiValueContainer:innerForm:content:view:0:panel:"
                + "textField", "recipient@syncope.org");
        formTester.setValue("content:form:view:selfAsRecipient:checkboxField", true);

        TESTER.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:finish");
        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.cleanupFeedbackMessages();
        TESTER.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
    }

    @Test
    public void read() {
        assertNull(findComponentByProp(KEY, "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", 1));
    }

    @Test
    public void create() {
        createNotification("create@syncope.org", "create");
    }

    @Test
    public void update() {
        createNotification("update@syncope.org", "createToUpdate");
        Component result = findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToUpdate");

        TESTER.clickLink(result.getPageRelativePath() + ":cells:7:cell:panelEdit:editLink");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void execute() {
        TESTER.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");

        Component result = findComponentByProp("subject",
                "body:content:tabbedPanel:panel:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "Password Reset request");

        TESTER.clickLink(
                result.getPageRelativePath() + ":cells:7:cell:panelNotificationTasks:notificationTasksLink");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", WebMarkupContainer.class);

        result = findComponentByProp("subject", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "Notification for SYNCOPE-81");

        TESTER.clickLink(result.getPageRelativePath() + ":cells:8:cell:panelExecute:executeLink");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");

        result = findComponentByProp("subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "Password Reset request");

        TESTER.clickLink(
                result.getPageRelativePath() + ":cells:7:cell:panelNotificationTasks:notificationTasksLink");

        result = findComponentByProp("subject", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "Notification for SYNCOPE-81");

        TESTER.clickLink(result.getPageRelativePath() + ":cells:8:cell:panelView:viewLink");

        TESTER.assertLabel("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:tasks:"
                + "secondLevelContainer:title", "Notifications with subject &#039;Notification for SYNCOPE-81&#039;");

        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:tasks:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("subject", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "Notification for SYNCOPE-81"));
    }

    @Test
    public void delete() {
        createNotification("delete@syncope.org", "createToDelete");
        Component result = findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToDelete");

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToDelete"));
    }
}
