/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.ui.actions;

import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IField;
import org.eclipse.dltk.core.ILocalVariable;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IPackageDeclaration;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.internal.ui.search.DLTKSearchScopeFactory;
import org.eclipse.dltk.internal.ui.search.SearchMessages;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.search.ElementQuerySpecification;
import org.eclipse.dltk.ui.search.QuerySpecification;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Finds references to the selected element in the enclosing project of the
 * selected element. The action is applicable to selections representing a
 * Script element.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class FindReferencesInProjectAction extends FindReferencesAction {

	/**
	 * Creates a new <code>FindReferencesInProjectAction</code>. The action
	 * requires that the selection provided by the site's selection provider is of type
	 * <code>IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public FindReferencesInProjectAction(IDLTKLanguageToolkit toolkit,
			IWorkbenchSite site) {
		super(toolkit, site);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Script editor
	 */
	public FindReferencesInProjectAction(IDLTKLanguageToolkit toolkit,
			ScriptEditor editor) {
		super(toolkit, editor);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Script editor
	 * @since 5.3
	 */
	public FindReferencesInProjectAction(IDLTKLanguageToolkit toolkit,
			AbstractDecoratedTextEditor editor) {
		super(toolkit, editor);
	}

	@Override
	@SuppressWarnings("rawtypes")
	Class[] getValidTypes() {
		return new Class[] { IField.class, IMethod.class, IType.class,
				ISourceModule.class, IPackageDeclaration.class,
				IScriptFolder.class, ILocalVariable.class };
	}

	@Override
	void init() {
		setText(SearchMessages.Search_FindReferencesInProjectAction_label);
		setToolTipText(SearchMessages.Search_FindReferencesInProjectAction_tooltip);
		setImageDescriptor(DLTKPluginImages.DESC_OBJS_SEARCH_REF);
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_PROJECT_ACTION);
		if (DLTKCore.DEBUG) {
			System.out.println("TODO: Add help support here..."); //$NON-NLS-1$
		}
	}

	@Override
	QuerySpecification createQuery(IModelElement element) throws ModelException {
		DLTKSearchScopeFactory factory= DLTKSearchScopeFactory.getInstance();
		AbstractTextEditor editor = getTextEditor();

		IDLTKSearchScope scope;
		String description;
		boolean isInsideInterpreterEnvironment= factory.isInsideInterpreter(element);
		if (editor != null) {
			scope= factory.createProjectSearchScope(editor.getEditorInput(), isInsideInterpreterEnvironment);
			description= factory.getProjectScopeDescription(editor.getEditorInput(), isInsideInterpreterEnvironment);
		} else {
			scope= factory.createProjectSearchScope(element.getScriptProject(), isInsideInterpreterEnvironment);
			description=  factory.getProjectScopeDescription(element.getScriptProject(), isInsideInterpreterEnvironment);
		}
		return new ElementQuerySpecification(element, getLimitTo(), scope, description);
	}

}
