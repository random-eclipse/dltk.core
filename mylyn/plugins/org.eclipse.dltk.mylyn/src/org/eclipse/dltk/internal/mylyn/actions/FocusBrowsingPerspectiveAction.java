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

package org.eclipse.dltk.internal.mylyn.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.internal.mylyn.DLTKUiBridgePlugin;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.context.ui.AbstractFocusViewAction;
import org.eclipse.mylyn.context.ui.InterestFilter;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * This class is a bit weird since it doesn't obey the same contract as the other subclasses
 *
 * @author Shawn Minto
 * @author Mik Kersten
 */
public class FocusBrowsingPerspectiveAction extends AbstractFocusViewAction implements IWorkbenchWindowActionDelegate {

	private final String[] viewNames = { "org.eclipse.jdt.ui.MembersView", "org.eclipse.jdt.ui.PackagesView", //$NON-NLS-1$ //$NON-NLS-2$
			"org.eclipse.jdt.ui.TypesView" }; //$NON-NLS-1$

	private IWorkbenchWindow initWindow;

	public FocusBrowsingPerspectiveAction() {
		super(new InterestFilter(), true, true, false);
		globalPrefId = PREF_ID_PREFIX + "javaBrowsing"; //$NON-NLS-1$
	}

	@Override
	public void init(IWorkbenchWindow window) {
		initWindow = window;
		IWorkbenchPage activePage = initWindow.getActivePage();
		super.viewPart = activePage.findView(viewNames[0]);
	}

	@Override
	public List<StructuredViewer> getViewers() {
		List<StructuredViewer> viewers = new ArrayList<>();
		for (String viewName : viewNames) {
			StructuredViewer viewer = getBrowsingViewerFromActivePerspective(viewName);
			if (viewer != null) {
				viewers.add(viewer);
			}
		}
		return viewers;
	}

	private StructuredViewer getBrowsingViewerFromActivePerspective(String id) {
		IWorkbenchPage activePage = initWindow.getActivePage();
		if (activePage == null) {
			return null;
		}
		try {
			IViewPart viewPart = activePage.findView(id);
			if (viewPart == null) {
				// view is not open, just return null
				return null;
			}

			IWorkbenchPartSite site = viewPart.getSite();
			if (site == null) {
				// no site found, view still being initialized, return null
				return null;
			}

			ISelectionProvider provider = site.getSelectionProvider();
			if (!(provider instanceof StructuredViewer)) {
				// provider not a StructuredViewer, return null
				return null;
			}

			return (StructuredViewer) provider;
		} catch (Exception e) {
			StatusHandler.log(new Status(IStatus.ERROR, DLTKUiBridgePlugin.ID_PLUGIN, "Could not get \"" + id //$NON-NLS-1$
					+ "\" view tree viewer", e)); //$NON-NLS-1$
		}
		return null;
	}

}
