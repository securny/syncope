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
package org.apache.syncope.ide.eclipse.plugin.editors;

import java.util.ResourceBundle;

import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.AutoIndentAction;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLFileDocumentProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.TextEditorAction;

public class HTMLEditor extends TextEditor {
    
    private static final String RESOURCE_BUNDLE = "/src/main/resources/HTMLEditor";
            
    public HTMLEditor() {
        super();
        setSourceViewerConfiguration(new HTMLSourceConfiguration());
    }

    @Override
    protected final void doSetInput(final IEditorInput input) throws CoreException {
        setDocumentProvider(new HTMLFileDocumentProvider());
        super.doSetInput(input);
    }

    @Override
    protected void editorContextMenuAboutToShow(final IMenuManager menu) {
        super.editorContextMenuAboutToShow(menu);
        addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "AutoIndent");
    }

    @Override
    protected void createActions() {
        super.createActions();
        IAction a = (TextEditorAction) new AutoIndentAction(
                ResourceBundle.getBundle(RESOURCE_BUNDLE), "AutoIndent", null);
        setAction("AutoIndent", a);
    }
}
