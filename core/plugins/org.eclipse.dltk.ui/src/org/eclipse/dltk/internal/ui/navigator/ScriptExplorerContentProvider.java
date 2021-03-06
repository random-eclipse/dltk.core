/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.navigator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ElementChangedEvent;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IElementChangedListener;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelElementDelta;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptModel;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.dltk.internal.ui.StandardModelElementContentProvider;
import org.eclipse.dltk.internal.ui.scriptview.BuildPathContainer;
import org.eclipse.dltk.internal.ui.scriptview.LibraryContainer;
import org.eclipse.dltk.internal.ui.workingsets.WorkingSetModel;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.UIJob;

/**
 * Content provider for the PackageExplorer.
 *
 * <p>
 * Since 2.1 this content provider can provide the children for flat or
 * hierarchical layout.
 * </p>
 *
 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider
 */
public class ScriptExplorerContentProvider
		extends StandardModelElementContentProvider implements
		ITreeContentProvider, IElementChangedListener, IPropertyChangeListener {

	protected static final int ORIGINAL = 0;
	protected static final int PARENT = 1 << 0;
	protected static final int GRANT_PARENT = 1 << 1;
	protected static final int PROJECT = 1 << 2;

	private TreeViewer fViewer;
	private Object fInput;
	private boolean fIsFlatLayout;
	private boolean fShowLibrariesNode;
	private boolean fFoldPackages;

	private Collection<Runnable> fPendingUpdates;

	private UIJob fUpdateJob;

	/**
	 * Creates a new content provider for Java elements.
	 *
	 * @param provideMembers
	 *                           if set, members of compilation units and class
	 *                           files are shown
	 */
	public ScriptExplorerContentProvider(final boolean provideMembers) {
		super(provideMembers);
		fShowLibrariesNode = false;
		fIsFlatLayout = false;
		fFoldPackages = arePackagesFoldedInHierarchicalLayout();
		fPendingUpdates = null;
		DLTKUIPlugin.getDefault().getPreferenceStore()
				.addPropertyChangeListener(this);
	}

	private boolean arePackagesFoldedInHierarchicalLayout() {
		return getPreferenceStore().getBoolean(
				PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER);
	}

	protected IPreferenceStore getPreferenceStore() {
		return DLTKUIPlugin.getDefault().getPreferenceStore();
	}

	protected Object getViewerInput() {
		return fInput;
	}

	/*
	 * (non-Javadoc) Method declared on IElementChangedListener.
	 */
	@Override
	public void elementChanged(final ElementChangedEvent event) {
		final ArrayList<Runnable> runnables = new ArrayList<>();
		try {
			// 58952 delete project does not update Package Explorer [package
			// explorer]
			// if the input to the viewer is deleted then refresh to avoid the
			// display of stale elements
			if (inputDeleted(runnables)) {
				return;
			}

			processDelta(event.getDelta(), runnables);
		} catch (ModelException e) {
			DLTKUIPlugin.log(e);
		} finally {
			executeRunnables(runnables);
		}
	}

	protected final void executeRunnables(
			final Collection<Runnable> runnables) {

		// now post all collected runnables
		Control ctrl = fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			final boolean hasPendingUpdates;
			synchronized (this) {
				hasPendingUpdates = fPendingUpdates != null
						&& !fPendingUpdates.isEmpty();
			}
			// Are we in the UIThread? If so spin it until we are done
			if (!hasPendingUpdates
					&& ctrl.getDisplay().getThread() == Thread.currentThread()
					&& !fViewer.isBusy()) {
				runUpdates(runnables);
			} else {
				synchronized (this) {
					if (fPendingUpdates == null) {
						fPendingUpdates = runnables;
					} else {
						fPendingUpdates.addAll(runnables);
					}
				}
				postAsyncUpdate(ctrl.getDisplay());
			}
		}
	}

	private void postAsyncUpdate(final Display display) {
		if (fUpdateJob == null) {
			fUpdateJob = new UIJob(display, "Update Script explorer") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					TreeViewer viewer = fViewer;
					if (viewer != null && viewer.isBusy()) {
						// reschedule when viewer is busy: bug 184991
						schedule(100);
					} else {
						runPendingUpdates();
					}
					return Status.OK_STATUS;
				}
			};
			fUpdateJob.setSystem(true);
		}
		fUpdateJob.schedule();
	}

	/**
	 * Run all of the runnables that are the widget updates. Must be called in
	 * the display thread.
	 */
	public void runPendingUpdates() {
		Collection<Runnable> pendingUpdates;
		synchronized (this) {
			pendingUpdates = fPendingUpdates;
			fPendingUpdates = null;
		}
		if (pendingUpdates != null && fViewer != null) {
			Control control = fViewer.getControl();
			if (control != null && !control.isDisposed()) {
				runUpdates(pendingUpdates);
			}
		}
	}

	private void runUpdates(final Collection<Runnable> runnables) {
		Iterator<Runnable> runnableIterator = runnables.iterator();
		while (runnableIterator.hasNext()) {
			runnableIterator.next().run();
		}
	}

	private boolean inputDeleted(final Collection<Runnable> runnables) {
		if (fInput == null) {
			return false;
		}
		if (fInput instanceof IModelElement
				&& ((IModelElement) fInput).exists()) {
			return false;
		}
		if (fInput instanceof IResource && ((IResource) fInput).exists()) {
			return false;
		}
		if (fInput instanceof WorkingSetModel) {
			return false;
		}
		if (fInput instanceof IWorkingSet) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=156239
			return false;
		}
		postRefresh(fInput, ScriptExplorerContentProvider.ORIGINAL, fInput,
				runnables);
		return true;
	}

	@Override
	public void dispose() {
		super.dispose();
		DLTKCore.removeElementChangedListener(this);
		DLTKUIPlugin.getDefault().getPreferenceStore()
				.removePropertyChangeListener(this);
	}

	@Override
	protected Object[] getProjectFragmentContent(final IProjectFragment root)
			throws ModelException {
		if (fIsFlatLayout) {
			return super.getProjectFragmentContent(root);
		}

		// hierarchical package mode
		ArrayList<Object> result = new ArrayList<>();
		getHierarchicalPackageChildren(root, null, result);
		if (!isProjectProjectFragment(root)) {
			Object[] nonJavaResources = root.getForeignResources();
			for (int i = 0; i < nonJavaResources.length; i++) {
				result.add(nonJavaResources[i]);
			}
		}
		return result.toArray();
	}

	@Override
	protected Object[] getScriptFolderContent(final IScriptFolder fragment)
			throws ModelException {
		if (fIsFlatLayout) {
			return super.getScriptFolderContent(fragment);
		}

		// hierarchical package mode
		ArrayList<Object> result = new ArrayList<>();

		getHierarchicalPackageChildren((IProjectFragment) fragment.getParent(),
				fragment, result);
		Object[] nonPackages = super.getScriptFolderContent(fragment);
		if (result.isEmpty()) {
			return nonPackages;
		}
		for (int i = 0; i < nonPackages.length; i++) {
			result.add(nonPackages[i]);
		}
		return result.toArray();
	}

	@Override
	protected Object[] getFolderContent(final IFolder folder)
			throws CoreException {
		if (fIsFlatLayout) {
			return super.getFolderContent(folder);
		}

		// hierarchical package mode
		ArrayList<Object> result = new ArrayList<>();

		getHierarchicalPackagesInFolder(folder, result);
		Object[] others = super.getFolderContent(folder);
		if (result.isEmpty()) {
			return others;
		}
		for (int i = 0; i < others.length; i++) {
			result.add(others[i]);
		}
		return result.toArray();
	}

	@Override
	public Object[] getChildren(final Object parentElement) {
		try {
			if (parentElement instanceof IScriptModel) {
				return getExtendedChildren(parentElement,
						StandardModelElementContentProvider.concatenate(
								getScriptProjects((IScriptModel) parentElement),
								getNonJavaProjects(
										(IScriptModel) parentElement)));
			}

			if (parentElement instanceof ProjectFragmentContainer) {
				return getExtendedChildren(parentElement,
						getContainerProjectFragments(
								(ProjectFragmentContainer) parentElement));
			}

			if (parentElement instanceof IProject) {
				if (!((IProject) parentElement).isAccessible()) {
					return StandardModelElementContentProvider.NO_CHILDREN;
				}
				return getExtendedChildren(parentElement,
						((IProject) parentElement).members());
			}

			return super.getChildren(parentElement);
		} catch (CoreException e) {
			return getExtendedChildren(parentElement,
					StandardModelElementContentProvider.NO_CHILDREN);
		}
	}

	@Override
	protected Object[] getProjectFragments(final IScriptProject project)
			throws ModelException {
		if (!project.getProject().isOpen()) {
			return StandardModelElementContentProvider.NO_CHILDREN;
		}

		List<Object> result = new ArrayList<>();

		boolean addZIPContainer = false;

		IProjectFragment[] roots = project.getProjectFragments();
		for (int i = 0; i < roots.length; i++) {
			IProjectFragment root = roots[i];
			final IBuildpathEntry classpathEntry;
			try {
				classpathEntry = root.getRawBuildpathEntry();
			} catch (ModelException e) {
				continue;
			}
			if (classpathEntry == null) {
				continue;
			}
			int entryKind = classpathEntry.getEntryKind();
			if (entryKind == IBuildpathEntry.BPE_CONTAINER) {
				// all ClassPathContainers are added later
			} else if (fShowLibrariesNode
					&& (entryKind == IBuildpathEntry.BPE_LIBRARY /*
																	 * ||
																	 * entryKind
																	 * ==
																	 * IBuildpathEntry
																	 * .BPE_VARIABLE
																	 */)) {
				addZIPContainer = true;
			} else {
				if (isProjectProjectFragment(root)) {
					// filter out package fragments that correspond to projects
					// and
					// replace them with the package fragments directly
					Object[] fragments = getProjectFragmentContent(root);
					for (int j = 0; j < fragments.length; j++) {
						result.add(fragments[j]);
					}
				} else {
					result.add(root);
				}
			}
		}

		if (addZIPContainer) {
			result.add(new LibraryContainer(project));
		}

		// separate loop to make sure all containers are on the classpath
		IBuildpathEntry[] rawBuidspath = project.getRawBuildpath();
		for (int i = 0; i < rawBuidspath.length; i++) {
			IBuildpathEntry classpathEntry = rawBuidspath[i];
			if (classpathEntry
					.getEntryKind() == IBuildpathEntry.BPE_CONTAINER) {
				result.add(new BuildPathContainer(project, classpathEntry));
			}
		}
		Object[] resources = project.getForeignResources();
		for (int i = 0; i < resources.length; i++) {
			result.add(resources[i]);
		}
		return result.toArray();
	}

	private Object[] getContainerProjectFragments(
			final ProjectFragmentContainer container) {
		return container.getChildren();
	}

	private Object[] getNonJavaProjects(final IScriptModel model)
			throws ModelException {
		return model.getForeignResources();
	}

	@Override
	protected Object internalGetParent(final Object element) {
		if (!fIsFlatLayout && element instanceof IScriptFolder) {
			return getHierarchicalPackageParent((IScriptFolder) element);
		} else if (element instanceof IProjectFragment) {
			// since we insert logical package containers we have to fix
			// up the parent for package fragment roots so that they refer
			// to the container and containers refer to the project
			IProjectFragment root = (IProjectFragment) element;

			try {
				IBuildpathEntry entry = root.getRawBuildpathEntry();
				if (entry != null) {
					int entryKind = entry.getEntryKind();
					if (entryKind == IBuildpathEntry.BPE_CONTAINER) {
						return new BuildPathContainer(root.getScriptProject(),
								entry);
					} else if (fShowLibrariesNode
							&& (entryKind == IBuildpathEntry.BPE_LIBRARY /*
																			 * ||
																			 * entryKind
																			 * ==
																			 * IBuildpathEntry
																			 * .
																			 * BPE_VARIABLE
																			 */)) {
						return new LibraryContainer(root.getScriptProject());
					}
				}
			} catch (ModelException e) {
				// fall through
			}
		} else if (element instanceof ProjectFragmentContainer) {
			return ((ProjectFragmentContainer) element).getScriptProject();
		}
		return super.internalGetParent(element);
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput,
			final Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		fViewer = (TreeViewer) viewer;
		if (oldInput == null && newInput != null) {
			DLTKCore.addElementChangedListener(this);
		} else if (oldInput != null && newInput == null) {
			DLTKCore.removeElementChangedListener(this);
		}
		fInput = newInput;
	}

	// hierarchical packages
	/**
	 * Returns the hierarchical packages inside a given fragment or root.
	 *
	 * @param parent
	 *                     The parent package fragment root
	 * @param fragment
	 *                     The package to get the children for or 'null' to get
	 *                     the children of the root.
	 * @param result
	 *                     Collection where the resulting elements are added
	 * @throws JavaModelException
	 */
	private void getHierarchicalPackageChildren(final IProjectFragment parent,
			final IScriptFolder fragment, final Collection<Object> result)
			throws ModelException {
		IModelElement[] children = parent.getChildren();

		if (fragment == null || fragment.isRootFolder()) {
			List<IModelElement> newElements = new ArrayList<>();
			for (int i = 0; i < children.length; ++i) {
				if (children[i] instanceof IScriptFolder) {
					IScriptFolder scriptFolder = (IScriptFolder) children[i];
					if (scriptFolder.isRootFolder()) {
						IModelElement[] members = scriptFolder.getChildren();
						for (int j = 0; j < members.length; ++j) {
							newElements.add(members[j]);
						}
						continue;
					}
				}
				newElements.add(children[i]);
			}
			children = newElements
					.toArray(new IModelElement[newElements.size()]);
		}

		String prefix = fragment != null
				? fragment.getElementName()
						+ IScriptFolder.PACKAGE_DELIMETER_STR
				: ""; //$NON-NLS-1$
		int prefixLen = prefix.length();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IScriptFolder) {
				IScriptFolder curr = (IScriptFolder) children[i];
				String name = curr.getElementName();
				if (name.startsWith(prefix) && name.length() > prefixLen
						&& name.indexOf(IScriptFolder.PACKAGE_DELIMITER,
								prefixLen) == -1) {
					if (fFoldPackages) {
						curr = ScriptExplorerContentProvider.getFolded(children,
								curr);
					}
					result.add(curr);
				} /*
					 * else if (fragment == null && curr.isRootFolder()) {
					 * result.add(curr); }
					 */
			} else {
				result.add(children[i]);
			}
		}
	}

	/**
	 * Returns the hierarchical packages inside a given folder.
	 *
	 * @param folder
	 *                   The parent folder
	 * @param result
	 *                   Collection where the resulting elements are added
	 * @throws CoreException
	 *                           thrown when elements could not be accessed
	 */
	private void getHierarchicalPackagesInFolder(final IFolder folder,
			final Collection<Object> result) throws CoreException {
		IResource[] resources = folder.members();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource instanceof IFolder) {
				IFolder curr = (IFolder) resource;
				IModelElement element = DLTKCore.create(curr);
				if (element instanceof IScriptFolder) {
					if (fFoldPackages) {
						IScriptFolder fragment = (IScriptFolder) element;
						IProjectFragment root = (IProjectFragment) fragment
								.getParent();
						element = ScriptExplorerContentProvider
								.getFolded(root.getChildren(), fragment);
					}
					result.add(element);
				}
			}
		}
	}

	public Object getHierarchicalPackageParent(final IScriptFolder child) {
		String name = child.getElementName();
		IProjectFragment parent = (IProjectFragment) child.getParent();
		int index = name.lastIndexOf(IScriptFolder.PACKAGE_DELIMITER);
		if (index != -1) {
			String realParentName = name.substring(0, index);
			IScriptFolder element = parent.getScriptFolder(realParentName);
			if (element != null && element.exists()) {
				try {
					if (fFoldPackages
							&& ScriptExplorerContentProvider.isEmpty(element)
							&& ScriptExplorerContentProvider
									.findSinglePackageChild(element,
											parent.getChildren()) != null) {
						return getHierarchicalPackageParent(element);
					}
				} catch (ModelException e) {
					// ignore
				}
				return element;
			} else if (element != null) { // bug 65240
				IResource resource = element.getResource();
				if (resource != null) {
					return resource;
				}
			}
		}
		if (parent.getResource() instanceof IProject) {
			return parent.getScriptProject();
		}
		return parent;
	}

	private static IScriptFolder getFolded(final IModelElement[] children,
			IScriptFolder pack) throws ModelException {
		while (ScriptExplorerContentProvider.isEmpty(pack)) {
			IScriptFolder collapsed = ScriptExplorerContentProvider
					.findSinglePackageChild(pack, children);
			if (collapsed == null) {
				return pack;
			}
			pack = collapsed;
		}
		return pack;
	}

	private static boolean isEmpty(final IScriptFolder fragment)
			throws ModelException {
		return !fragment.containsScriptResources()
				&& fragment.getForeignResources().length == 0;
	}

	private static IScriptFolder findSinglePackageChild(
			final IScriptFolder fragment, final IModelElement[] children) {
		String prefix = fragment.getElementName()
				+ IScriptFolder.PACKAGE_DELIMITER;
		int prefixLen = prefix.length();
		IScriptFolder found = null;
		for (int i = 0; i < children.length; i++) {
			IModelElement element = children[i];
			String name = element.getElementName();
			if (name.startsWith(prefix) && name.length() > prefixLen
					&& name.indexOf(IScriptFolder.PACKAGE_DELIMITER,
							prefixLen) == -1) {
				if (found == null) {
					found = (IScriptFolder) element;
				} else {
					return null;
				}
			}
		}
		return found;
	}

	// ------ delta processing ------

	/**
	 * Processes a delta recursively. When more than two children are affected
	 * the tree is fully refreshed starting at this node.
	 *
	 * @param delta
	 *                      the delta to process
	 * @param runnables
	 *                      the resulting view changes as runnables (type
	 *                      {@link Runnable} )
	 * @return true is returned if the conclusion is to refresh a parent of an
	 *         element. In that case no siblings need to be processed
	 * @throws JavaModelException
	 *                                thrown when the access to an element
	 *                                failed
	 */
	private boolean processDelta(final IModelElementDelta delta,
			final Collection<Runnable> runnables) throws ModelException {

		int kind = delta.getKind();
		int flags = delta.getFlags();
		IModelElement element = delta.getElement();
		int elementType = element.getElementType();

		if (elementType != IModelElement.SCRIPT_MODEL
				&& elementType != IModelElement.SCRIPT_PROJECT) {
			IScriptProject proj = element.getScriptProject();
			if (proj == null || !proj.getProject().isOpen()) {
				// TODO: Not needed if parent already did the 'open' check!
				return false;
			}
		}

		if (elementType == IModelElement.SCRIPT_FOLDER) {
			if ((flags & (IModelElementDelta.F_CONTENT
					| IModelElementDelta.F_CHILDREN)) == IModelElementDelta.F_CONTENT) {
				if (!fIsFlatLayout) {
					Object parent = getHierarchicalPackageParent(
							(IScriptFolder) element);
					if (!(parent instanceof IProjectFragment)) {
						postRefresh(internalGetParent(parent), GRANT_PARENT,
								element, runnables);
						return true;
					}
				}
				// content change, without children info (for example resource
				// added/removed to class folder package)
				postRefresh(internalGetParent(element), PARENT, element,
						runnables);
				return true;
			}

			if (!fIsFlatLayout) {
				if (kind == IModelElementDelta.REMOVED) {
					final Object parent = getHierarchicalPackageParent(
							(IScriptFolder) element);
					if (parent instanceof IProjectFragment) {
						postRemove(element, runnables);
						return false;
					}
					postRefresh(internalGetParent(parent), GRANT_PARENT,
							element, runnables);
					return true;
				} else if (kind == IModelElementDelta.ADDED) {
					final Object parent = getHierarchicalPackageParent(
							(IScriptFolder) element);
					if (parent instanceof IProjectFragment) {
						if (fFoldPackages) {
							postRefresh(parent, PARENT, element, runnables);
							return true;
						}
						postAdd(parent, element, runnables);
						return false;
					}
					postRefresh(internalGetParent(parent), GRANT_PARENT,
							element, runnables);
					return true;
				}
				handleAffectedChildren(delta, element, runnables);
				return false;
			}
		}

		if (elementType == IModelElement.SOURCE_MODULE) {
			ISourceModule cu = (ISourceModule) element;
			if (!ScriptModelUtil.isPrimary(cu)) {
				return false;
			}

			if (!getProvideMembers() && cu.isWorkingCopy()
					&& kind == IModelElementDelta.CHANGED) {
				return false;
			}

			if (kind == IModelElementDelta.CHANGED
					&& !isStructuralCUChange(flags)) {
				return false; // test moved ahead
			}

			if (!isOnClassPath(cu)) { // TODO: isOnClassPath expensive! Should
				// be put after all cheap tests
				return false;
			}

		}

		if (elementType == IModelElement.SCRIPT_PROJECT) {
			// handle open and closing of a project
			if ((flags & (IModelElementDelta.F_CLOSED
					| IModelElementDelta.F_OPENED)) != 0) {
				postRefresh(element, ORIGINAL, element, runnables);
				return false;
			}
			// if the class path has changed we refresh the entire project
			if ((flags & IModelElementDelta.F_BUILDPATH_CHANGED) != 0) {
				postRefresh(element, ORIGINAL, element, runnables);
				return false;
			}
			// if added it could be that the corresponding IProject is already
			// shown. Remove it first.
			// bug 184296
			if (kind == IModelElementDelta.ADDED) {
				postRemove(element.getResource(), runnables);
				postAdd(element.getParent(), element, runnables);
				return false;
			}
		}

		if (kind == IModelElementDelta.REMOVED) {
			Object parent = internalGetParent(element);
			if (element instanceof IScriptFolder) {
				// refresh package fragment root to allow filtering empty
				// (parent) packages: bug 72923
				if (fViewer.testFindItem(parent) != null) {
					postRefresh(parent, PARENT, element, runnables);
				}
				return true;

			} else if (element instanceof IProjectFragment
					&& ((IProjectFragment) element)
							.getKind() != IProjectFragment.K_SOURCE) {
				// libs and class folders can show up twice (in library
				// container and as resource at original location)
				IResource resource = element.getResource();
				if (resource != null)
					postRemove(resource, runnables);
			}

			postRemove(element, runnables);
			if (parent instanceof IScriptFolder) {
				postUpdateIcon((IScriptFolder) parent, runnables);
			}
			// we are filtering out empty subpackages, so we
			// a package becomes empty we remove it from the viewer.
			if (isScriptFolderEmpty(element.getParent())) {
				if (fViewer.testFindItem(parent) != null) {
					postRefresh(internalGetParent(parent), GRANT_PARENT,
							element, runnables);
				}
				return true;
			}
			return false;
		}

		if (kind == IModelElementDelta.ADDED) {
			Object parent = internalGetParent(element);
			// we are filtering out empty subpackages, so we
			// have to handle additions to them specially.
			if (parent instanceof IScriptFolder) {
				Object grandparent = internalGetParent(parent);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an invisible parent
				if (parent.equals(fInput)) {
					postRefresh(parent, PARENT, element, runnables);
				} else {
					// refresh from grandparent if parent isn't visible yet
					if (fViewer.testFindItem(parent) == null) {
						postRefresh(grandparent, GRANT_PARENT, element,
								runnables);
					} else {
						postRefresh(parent, PARENT, element, runnables);
					}
				}
				return true;
			}
			postAdd(parent, element, runnables);
		}

		if (elementType == IModelElement.SOURCE_MODULE
				|| elementType == IModelElement.BINARY_MODULE) {
			if (kind == IModelElementDelta.CHANGED) {
				// isStructuralCUChange already performed above
				postRefresh(element, ORIGINAL, element, runnables);
			}
			return false;
		}

		if (elementType == IModelElement.PROJECT_FRAGMENT) {
			// the contents of an external JAR or class folder has changed
			if ((flags & IModelElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0) {
				postRefresh(element, ORIGINAL, element, runnables);
				return false;
			}
			if ((flags & (IModelElementDelta.F_CONTENT
					| IModelElementDelta.F_CHILDREN)) == IModelElementDelta.F_CONTENT) {
				// content change, without children info (for example resource
				// added/removed to class folder package)
				postRefresh(internalGetParent(element), PARENT, element,
						runnables);
				return true;
			}
			// the source attachment of a JAR has changed
			// if ((flags & ( | IModelElementDelta.F_SOURCEDETACHED)) != 0) {
			// postUpdateIcon(element, runnables);
			// }

			if (isBuildPathChange(delta)) {
				// throw the towel and do a full refresh of the affected java
				// project.
				postRefresh(element.getScriptProject(), PROJECT, element,
						runnables);
				return true;
			}
		}

		handleAffectedChildren(delta, element, runnables);
		return false;
	}

	private static boolean isStructuralCUChange(final int flags) {
		// No refresh on working copy creation (F_PRIMARY_WORKING_COPY)
		return (flags & IModelElementDelta.F_CHILDREN) != 0
				|| (flags & (IModelElementDelta.F_CONTENT
						| IModelElementDelta.F_FINE_GRAINED)) == IModelElementDelta.F_CONTENT;
	}

	/* package */void handleAffectedChildren(final IModelElementDelta delta,
			final IModelElement element, final Collection<Runnable> runnables)
			throws ModelException {
		int count = 0;

		IResourceDelta[] resourceDeltas = delta.getResourceDeltas();
		if (resourceDeltas != null) {
			for (int i = 0; i < resourceDeltas.length; i++) {
				int kind = resourceDeltas[i].getKind();
				if (kind == IResourceDelta.ADDED
						|| kind == IResourceDelta.REMOVED) {
					count++;
				}
			}
		}
		IModelElementDelta[] affectedChildren = delta.getAffectedChildren();
		for (int i = 0; i < affectedChildren.length; i++) {
			int kind = affectedChildren[i].getKind();
			if (kind == IModelElementDelta.ADDED
					|| kind == IModelElementDelta.REMOVED) {
				count++;
			}
		}

		if (count > 1) {
			// more than one child changed, refresh from here downwards
			if (element instanceof IScriptFolder) {
				// a package fragment might become non empty refresh from the
				// parent
				IModelElement parent = (IModelElement) internalGetParent(
						element);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an invisible parent
				if (element.equals(fInput)) {
					postRefresh(element, ScriptExplorerContentProvider.ORIGINAL,
							element, runnables);
				} else {
					postRefresh(parent, ScriptExplorerContentProvider.PARENT,
							element, runnables);
				}
			} else if (element instanceof IProjectFragment) {
				Object toRefresh = internalGetParent(element);
				postRefresh(toRefresh, ScriptExplorerContentProvider.ORIGINAL,
						toRefresh, runnables);
			} else {
				postRefresh(element, ScriptExplorerContentProvider.ORIGINAL,
						element, runnables);
			}
			return;
		}
		if (resourceDeltas != null) {
			for (int i = 0; i < resourceDeltas.length; i++) {
				if (processResourceDelta(resourceDeltas[i], element,
						runnables)) {
					return; // early return, element got refreshed
				}
			}
		}
		for (int i = 0; i < affectedChildren.length; i++) {
			if (processDelta(affectedChildren[i], runnables)) {
				return; // early return, element got refreshed
			}
		}
	}

	protected void processAffectedChildren(
			final IModelElementDelta[] affectedChildren,
			final Collection<Runnable> runnables) throws ModelException {
		for (int i = 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i], runnables);
		}
	}

	private boolean isOnClassPath(final ISourceModule element) {
		IScriptProject project = element.getScriptProject();
		if (project == null || !project.exists()) {
			return false;
		}
		return project.isOnBuildpath(element);
	}

	/**
	 * Updates the package icon
	 *
	 * @param element
	 *                      the element to update
	 * @param runnables
	 *                      the resulting view changes as runnables (type
	 *                      {@link Runnable} )
	 */
	private void postUpdateIcon(final IModelElement element,
			final Collection<Runnable> runnables) {
		runnables.add(() -> fViewer.update(element,
				new String[] { IBasicPropertyConstants.P_IMAGE }));
	}

	/**
	 * Process a resource delta.
	 *
	 * @param delta
	 *                      the delta to process
	 * @param parent
	 *                      the parent
	 * @param runnables
	 *                      the resulting view changes as runnables (type
	 *                      {@link Runnable} )
	 * @return true if the parent got refreshed
	 */
	private boolean processResourceDelta(final IResourceDelta delta,
			final Object parent, final Collection<Runnable> runnables) {
		int status = delta.getKind();
		int flags = delta.getFlags();

		IResource resource = delta.getResource();
		// filter out changes affecting the output folder
		if (resource == null) {
			return false;
		}

		// this could be optimized by handling all the added children in the
		// parent
		if ((status & IResourceDelta.REMOVED) != 0) {
			if (parent instanceof IScriptFolder) {
				// refresh one level above to deal with empty package filtering
				// properly
				postRefresh(internalGetParent(parent),
						ScriptExplorerContentProvider.PARENT, parent,
						runnables);
				return true;
			}
			postRemove(resource, runnables);
			return false;
		}
		if ((status & IResourceDelta.ADDED) != 0) {
			if (parent instanceof IScriptFolder) {
				// refresh one level above to deal with empty package filtering
				// properly
				postRefresh(internalGetParent(parent),
						ScriptExplorerContentProvider.PARENT, parent,
						runnables);
				return true;
			}
			postAdd(parent, resource, runnables);
			return false;
		}
		if ((status & IResourceDelta.CHANGED) != 0) {
			if ((flags & IResourceDelta.TYPE) != 0) {
				postRefresh(parent, ScriptExplorerContentProvider.PARENT,
						resource, runnables);
				return true;
			}
		}
		// open/close state change of a project
		if ((flags & IResourceDelta.OPEN) != 0) {
			postProjectStateChanged(internalGetParent(parent), runnables);
			return true;
		}
		IResourceDelta[] resourceDeltas = delta.getAffectedChildren();

		int count = 0;
		for (int i = 0; i < resourceDeltas.length; i++) {
			int kind = resourceDeltas[i].getKind();
			if (kind == IResourceDelta.ADDED
					|| kind == IResourceDelta.REMOVED) {
				count++;
				if (count > 1) {
					postRefresh(parent, PARENT, resource, runnables);
					return true;
				}
			}
		}

		for (int i = 0; i < resourceDeltas.length; i++) {
			if (processResourceDelta(resourceDeltas[i], resource, runnables)) {
				return false; // early return, element got refreshed
			}
		}
		return false;
	}

	public void setIsFlatLayout(final boolean state) {
		fIsFlatLayout = state;
	}

	public void setShowLibrariesNode(final boolean state) {
		fShowLibrariesNode = state;
	}

	protected void postRefresh(Object root, final int relation,
			final Object affectedElement,
			final Collection<Runnable> runnables) {
		// JFace doesn't refresh when object isn't part of the viewer
		// Therefore move the refresh start down to the viewer's input
		if (isParent(root, fInput) || root instanceof IScriptModel) {
			root = fInput;
		}
		List<Object> toRefresh = new ArrayList<>(1);
		toRefresh.add(root);
		augmentElementToRefresh(toRefresh, relation, affectedElement);
		postRefresh(toRefresh, true, runnables);
	}

	/**
	 * Can be implemented by subclasses to add additional elements to refresh
	 *
	 * @param toRefresh
	 *                            the elements to refresh
	 * @param relation
	 *                            the relation to the affected element
	 *                            ({@link #GRANT_PARENT}, {@link #PARENT},
	 *                            {@link #ORIGINAL}, {@link #PROJECT})
	 * @param affectedElement
	 *                            the affected element
	 */
	protected void augmentElementToRefresh(final List<Object> toRefresh,
			final int relation, final Object affectedElement) {
	}

	private boolean isParent(final Object root, final Object child) {
		Object parent = getParent(child);
		if (parent == null) {
			return false;
		}
		if (parent.equals(root)) {
			return true;
		}
		return isParent(root, parent);
	}

	protected void postRefresh(final List<?> toRefresh,
			final boolean updateLabels, final Collection<Runnable> runnables) {
		runnables.add(() -> {
			for (Object item : toRefresh) {
				fViewer.refresh(item, updateLabels);
			}
		});
	}

	protected void postAdd(final Object parent, final Object element,
			final Collection<Runnable> runnables) {
		runnables.add(() -> {
			Widget[] items = fViewer.testFindItems(element);
			for (int i = 0; i < items.length; i++) {
				Widget item = items[i];
				if (item instanceof TreeItem && !item.isDisposed()) {
					TreeItem parentItem = ((TreeItem) item).getParentItem();
					if (parentItem != null && !parentItem.isDisposed()
							&& parent.equals(parentItem.getData())) {
						return; // no add, element already added (most
						// likely by a refresh)
					}
				}
			}
			fViewer.add(parent, element);
		});
	}

	protected void postRemove(final Object element,
			final Collection<Runnable> runnables) {
		runnables.add(() -> fViewer.remove(element));
	}

	protected void postProjectStateChanged(final Object root,
			final Collection<Runnable> runnables) {
		runnables.add(() -> {
			fViewer.refresh(root, true);
			// trigger a synthetic selection change so that action refresh
			// their
			// enable state.
			fViewer.setSelection(fViewer.getSelection());
		});
	}

	/*
	 * @see
	 * org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse
	 * .jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (arePackagesFoldedInHierarchicalLayout() != fFoldPackages) {
			fFoldPackages = arePackagesFoldedInHierarchicalLayout();
			if (fViewer != null && !fViewer.getControl().isDisposed()) {
				fViewer.getControl().setRedraw(false);
				Object[] expandedObjects = fViewer.getExpandedElements();
				fViewer.refresh();
				fViewer.setExpandedElements(expandedObjects);
				fViewer.getControl().setRedraw(true);
			}
		}
	}
}
