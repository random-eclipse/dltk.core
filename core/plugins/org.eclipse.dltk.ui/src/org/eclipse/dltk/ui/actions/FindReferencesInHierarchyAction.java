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
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.internal.ui.search.DLTKSearchScopeFactory;
import org.eclipse.dltk.internal.ui.search.SearchMessages;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.search.ElementQuerySpecification;
import org.eclipse.dltk.ui.search.QuerySpecification;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;


/**
 * Finds references of the selected element in its hierarchy. The action is
 * applicable to selections representing a Script element.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class FindReferencesInHierarchyAction extends FindReferencesAction {

	/**
	 * Creates a new <code>FindReferencesInHierarchyAction</code>. The action
	 * requires that the selection provided by the site's selection provider is
	 * of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site
	 *            the site providing context information for this action
	 */
	public FindReferencesInHierarchyAction(IDLTKLanguageToolkit toolkit,
			IWorkbenchSite site) {
		super(toolkit, site);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 *
	 * @param editor the Script editor
	 */
	public FindReferencesInHierarchyAction(IDLTKLanguageToolkit toolkit,
			ScriptEditor editor) {
		super(toolkit, editor);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 *
	 * @param editor the Script editor
	 * @since 5.3
	 */
	public FindReferencesInHierarchyAction(IDLTKLanguageToolkit toolkit,
			AbstractDecoratedTextEditor editor) {
		super(toolkit, editor);
	}

	@Override
	Class[] getValidTypes() {
		return new Class[] { ISourceModule.class, IType.class, IMethod.class, IField.class };
	}

	@Override
	void init() {
		setText(SearchMessages.Search_FindHierarchyReferencesAction_label);
		setToolTipText(SearchMessages.Search_FindHierarchyReferencesAction_tooltip);
		setImageDescriptor(DLTKPluginImages.DESC_OBJS_SEARCH_REF);
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_HIERARCHY_ACTION);
		if (DLTKCore.DEBUG) {
			System.out.println("TODO: Add helkp support here..."); //$NON-NLS-1$
		}
	}

	@Override
	QuerySpecification createQuery(IModelElement element) throws ModelException {
		IType type = getType(element);
		if (type == null) {
			return super.createQuery(element);
		}
		DLTKSearchScopeFactory factory = DLTKSearchScopeFactory.getInstance();
		IDLTKSearchScope scope = SearchEngine.createHierarchyScope(type);
		String description = factory.getHierarchyScopeDescription(type);
		return new ElementQuerySpecification(element, getLimitTo(), scope, description);
	}

}
