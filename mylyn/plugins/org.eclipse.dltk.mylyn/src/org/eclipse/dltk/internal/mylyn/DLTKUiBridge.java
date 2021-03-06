/*******************************************************************************
 * Copyright (c) 2004, 2017 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Remy Chi Jian Suen - Bug 256071 Reduce/remove reflection usage in Java bridge
 *******************************************************************************/

package org.eclipse.dltk.internal.mylyn;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.actions.SelectionConverter;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.context.ui.AbstractContextUiBridge;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * @author Mik Kersten
 */
public class DLTKUiBridge extends AbstractContextUiBridge {

	@Override
	public void open(IInteractionElement node) {
		IModelElement javaElement = DLTKCore.create(node.getHandleIdentifier());
		if (javaElement == null || !javaElement.exists()) {
			return;
		}
		try {
			IEditorPart part = DLTKUIPlugin.openInEditor(javaElement);
			EditorUtility.revealInEditor(part, javaElement);
		} catch (Throwable t) {
			StatusHandler.log(new Status(IStatus.ERROR, DLTKUiBridgePlugin.ID_PLUGIN, "Could not open editor for: " //$NON-NLS-1$
					+ node, t));
		}
	}

	@Override
	public void close(IInteractionElement node) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page != null) {
				List<IEditorReference> toClose = new ArrayList<>(4);
				for (IEditorReference reference : page.getEditorReferences()) {
					try {
						IModelElement input = reference.getEditorInput().getAdapter(IModelElement.class);
						if (input != null && node.getHandleIdentifier().equals(input.getHandleIdentifier())) {
							toClose.add(reference);
						}
					} catch (PartInitException e) {
						// ignore
					}
				}
				if (toClose.size() > 0) {
					page.closeEditors(toClose.toArray(new IEditorReference[toClose.size()]), true);
				}
			}
		} catch (Throwable t) {
			StatusHandler
					.log(new Status(IStatus.ERROR, DLTKUiBridgePlugin.ID_PLUGIN, "Could not auto close editor", t)); //$NON-NLS-1$
		}
	}

	@Override
	public boolean acceptsEditor(IEditorPart editorPart) {
		return true;
	}

	@Override
	public IInteractionElement getElement(IEditorInput input) {
		Object adapter = input.getAdapter(IModelElement.class);
		if (adapter instanceof IModelElement) {
			IModelElement javaElement = (IModelElement) adapter;
			String handle = ContextCore.getStructureBridge(javaElement).getHandleIdentifier(javaElement);
			return ContextCore.getContextManager().getElement(handle);
		} else {
			return null;
		}
	}

	@Override
	public List<TreeViewer> getContentOutlineViewers(IEditorPart editorPart) {
		if (editorPart == null) {
			return null;
		}
		List<TreeViewer> viewers = new ArrayList<>();
		Object out = editorPart.getAdapter(IContentOutlinePage.class);
		if (out instanceof Page) {
			Page page = (Page) out;
			if (page.getControl() != null) {
				IWorkbenchSite site = page.getSite();
				if (site != null) {
					ISelectionProvider provider = site.getSelectionProvider();
					if (provider instanceof TreeViewer) {
						viewers.add((TreeViewer) provider);
					}
				}
			}
		}
		return viewers;
	}

	@Override
	public Object getObjectForTextSelection(TextSelection selection, IEditorPart editor) {
		TextSelection textSelection = selection;
		try {
			if (selection != null) {
				return SelectionConverter.resolveEnclosingElement(editor, textSelection);
			} else {
				Object element = editor.getEditorInput().getAdapter(IModelElement.class);
				if (element instanceof IModelElement) {
					return element;
				}
			}
		} catch (ModelException e) {
			// ignore
		}
		return null;
	}

	@Override
	public String getContentType() {
		return DLTKStructureBridge.CONTENT_TYPE;
	}

}
