/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.scriptview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.BuildpathContainerInitializer;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathContainer;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.corext.util.Messages;
import org.eclipse.dltk.internal.ui.navigator.ProjectFragmentContainer;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Representation of class path containers in Java UI.
 */
public class BuildPathContainer extends ProjectFragmentContainer {

	private IBuildpathEntry fClassPathEntry;
	private IBuildpathContainer fContainer;

	public static class RequiredProjectWrapper
			implements IAdaptable, IWorkbenchAdapter {

		private final BuildPathContainer fParent;
		private final IScriptProject fProject;

		public RequiredProjectWrapper(BuildPathContainer parent,
				IScriptProject project) {
			fParent = parent;
			fProject = project;
		}

		public IScriptProject getProject() {
			return fProject;
		}

		public BuildPathContainer getParentClassPathContainer() {
			return fParent;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return (T) this;
			return null;
		}

		@Override
		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return PlatformUI.getWorkbench().getSharedImages()
					.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		@Override
		public String getLabel(Object o) {
			return fProject.getElementName();
		}

		@Override
		public Object getParent(Object o) {
			return fParent;
		}
	}

	public BuildPathContainer(IScriptProject parent, IBuildpathEntry entry) {
		super(parent);
		fClassPathEntry = entry;
		try {
			fContainer = DLTKCore.getBuildpathContainer(entry.getPath(),
					parent);
		} catch (ModelException e) {
			fContainer = null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BuildPathContainer) {
			BuildPathContainer other = (BuildPathContainer) obj;
			if (getScriptProject().equals(other.getScriptProject())
					&& fClassPathEntry.equals(other.fClassPathEntry)) {
				return true;
			}

		}
		return false;
	}

	@Override
	public int hashCode() {
		return getScriptProject().hashCode() * 17 + fClassPathEntry.hashCode();
	}

	@Override
	public IProjectFragment[] getProjectFragments() {
		return getScriptProject().findProjectFragments(fClassPathEntry);
	}

	@Override
	public IAdaptable[] getChildren() {
		List<IAdaptable> list = new ArrayList<>();
		IProjectFragment[] roots = getProjectFragments();
		for (int i = 0; i < roots.length; i++) {
			list.add(roots[i]);
		}
		if (fContainer != null) {
			IBuildpathEntry[] classpathEntries = fContainer
					.getBuildpathEntries();
			if (classpathEntries == null) {
				// invalid implementation of a classpath container
				DLTKUIPlugin.log(new IllegalArgumentException(
						"Invalid classpath container implementation: getClasspathEntries() returns null. " //$NON-NLS-1$
								+ fContainer.getPath()));
			} else {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				for (int i = 0; i < classpathEntries.length; i++) {
					IBuildpathEntry entry = classpathEntries[i];
					if (entry.getEntryKind() == IBuildpathEntry.BPE_PROJECT) {
						IResource resource = root.findMember(entry.getPath());
						if (resource instanceof IProject)
							list.add(new RequiredProjectWrapper(this,
									DLTKCore.create((IProject) resource)));
					}
				}
			}
		}
		return list.toArray(new IAdaptable[list.size()]);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO (alex) check status and decorate with error
		return DLTKPluginImages.DESC_OBJS_LIBRARY;
	}

	@Override
	public String getLabel() {
		if (fContainer != null)
			return fContainer.getDescription();

		IPath path = fClassPathEntry.getPath();
		String containerId = path.segment(0);
		BuildpathContainerInitializer initializer = DLTKCore
				.getBuildpathContainerInitializer(containerId);
		if (initializer != null) {
			String description = initializer.getDescription(path,
					getScriptProject());
			return Messages.format(
					ScriptMessages.BuildPathContainer_unbound_label,
					description);
		}
		return Messages.format(ScriptMessages.BuildPathContainer_unknown_label,
				path.toString());
	}

	public IBuildpathEntry getBuildpathEntry() {
		return fClassPathEntry;
	}

	static boolean contains(IScriptProject project, IBuildpathEntry entry,
			IProjectFragment root) {
		IProjectFragment[] roots = project.findProjectFragments(entry);
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].equals(root))
				return true;
		}
		return false;
	}

}
