/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptModel;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.actions.SelectionConverter;
import org.eclipse.dltk.internal.ui.util.SWTUtil;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.ModelElementSorter;
import org.eclipse.dltk.ui.ScriptElementImageProvider;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.dltk.ui.viewsupport.StyledDecoratingModelLabelProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;

/**
 * The Script working set page allows the user to create and edit a Script
 * working set.
 * <p>
 * Working set elements are presented as a Script element tree.
 * </p>
 */
public class ScriptWorkingSetPage extends WizardPage
		implements IWorkingSetPage {

	final private static String PAGE_TITLE = WorkingSetMessages.ScriptWorkingSetPage_title;
	final private static String PAGE_ID = "ScriptWorkingSetPage"; //$NON-NLS-1$

	private Text fWorkingSetName;
	private CheckboxTreeViewer fTree;
	private ITreeContentProvider fTreeContentProvider;

	private boolean fFirstCheck;
	private IWorkingSet fWorkingSet;

	/**
	 * Default constructor.
	 */
	public ScriptWorkingSetPage() {
		super(PAGE_ID, PAGE_TITLE,
				DLTKPluginImages.DESC_WIZBAN_SCRIPT_WORKINGSET);
		setDescription(
				WorkingSetMessages.ScriptWorkingSetPage_workingSet_description);
		fFirstCheck = true;
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.
	 * widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(WorkingSetMessages.ScriptWorkingSetPage_workingSet_name);
		GridData gd = new GridData(
				GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL
						| GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);

		fWorkingSetName = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(
				GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fWorkingSetName.addModifyListener(e -> validateInput());
		fWorkingSetName.setFocus();

		label = new Label(composite, SWT.WRAP);
		label.setText(
				WorkingSetMessages.ScriptWorkingSetPage_workingSet_content);
		gd = new GridData(
				GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL
						| GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);

		fTree = new CheckboxTreeViewer(composite,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		gd.heightHint = convertHeightInCharsToPixels(15);
		fTree.getControl().setLayoutData(gd);

		fTreeContentProvider = new ScriptWorkingSetPageContentProvider();
		fTree.setContentProvider(fTreeContentProvider);

		AppearanceAwareLabelProvider fScriptElementLabelProvider = new AppearanceAwareLabelProvider(
				AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS
						| ScriptElementLabels.P_COMPRESSED,
				AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
						| ScriptElementImageProvider.SMALL_ICONS,
				DLTKUIPlugin.getDefault().getPreferenceStore());

		fTree.setLabelProvider(new StyledDecoratingModelLabelProvider(
				fScriptElementLabelProvider));
		fTree.setComparator(new ModelElementSorter());
		if (DLTKCore.DEBUG) {
			System.err
					.println("Add empty inner package filter support here..."); //$NON-NLS-1$
		}
		// fTree.addFilter(new EmptyInnerPackageFilter());
		fTree.setUseHashlookup(true);

		fTree.setInput(
				DLTKCore.create(ResourcesPlugin.getWorkspace().getRoot()));

		fTree.addCheckStateListener(event -> handleCheckStateChange(event));

		fTree.addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				final Object element = event.getElement();
				if (fTree.getGrayed(element) == false)
					BusyIndicator.showWhile(getShell().getDisplay(),
							() -> setSubtreeChecked(element,
									fTree.getChecked(element), false));
			}
		});

		// Add select / deselect all buttons for bug 46669
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonComposite.setLayout(layout);
		buttonComposite
				.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText(
				WorkingSetMessages.ScriptWorkingSetPage_selectAll_label);
		selectAllButton.setToolTipText(
				WorkingSetMessages.ScriptWorkingSetPage_selectAll_toolTip);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTree.setCheckedElements(
						fTreeContentProvider.getElements(fTree.getInput()));
				validateInput();
			}
		});
		selectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(selectAllButton);

		Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
		deselectAllButton.setText(
				WorkingSetMessages.ScriptWorkingSetPage_deselectAll_label);
		deselectAllButton.setToolTipText(
				WorkingSetMessages.ScriptWorkingSetPage_deselectAll_toolTip);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTree.setCheckedElements(new Object[0]);
				validateInput();
			}
		});
		deselectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(deselectAllButton);

		if (fWorkingSet != null)
			fWorkingSetName.setText(fWorkingSet.getName());
		initializeCheckedState();
		validateInput();

		Dialog.applyDialogFont(composite);
		// Set help for the page
		if (DLTKCore.DEBUG) {
			System.err.println("Add help support here..."); //$NON-NLS-1$
		}
		// ScriptUIHelp.setHelp(fTree,
		// IScriptHelpContextIds.Script_WORKING_SET_PAGE);
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	@Override
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	@Override
	public void setSelection(IWorkingSet workingSet) {
		Assert.isNotNull(workingSet, "Working set must not be null"); //$NON-NLS-1$
		fWorkingSet = workingSet;
		if (getContainer() != null && getShell() != null
				&& fWorkingSetName != null) {
			fFirstCheck = false;
			fWorkingSetName.setText(fWorkingSet.getName());
			initializeCheckedState();
			validateInput();
		}
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	@Override
	public void finish() {
		String workingSetName = fWorkingSetName.getText();
		ArrayList<IAdaptable> elements = new ArrayList<>(10);
		findCheckedElements(elements, fTree.getInput());
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
					.getWorkingSetManager();
			fWorkingSet = workingSetManager.createWorkingSet(workingSetName,
					elements.toArray(new IAdaptable[elements.size()]));
		} else {
			// Add inaccessible resources
			IAdaptable[] oldItems = fWorkingSet.getElements();
			ArrayList closedWithChildren = new ArrayList(elements.size());
			for (int i = 0; i < oldItems.length; i++) {
				IResource oldResource = null;
				if (oldItems[i] instanceof IResource) {
					oldResource = (IResource) oldItems[i];
				} else {
					oldResource = oldItems[i].getAdapter(IResource.class);
				}
				if (oldResource != null
						&& oldResource.isAccessible() == false) {
					IProject project = oldResource.getProject();
					if (elements.contains(project)
							|| closedWithChildren.contains(project)) {
						elements.add(oldItems[i]);
						elements.remove(project);
						closedWithChildren.add(project);
					}
				}
			}
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements(
					elements.toArray(new IAdaptable[elements.size()]));
		}
	}

	private void validateInput() {
		String errorMessage = null;
		String infoMessage = null;
		String newText = fWorkingSetName.getText();

		if (newText.equals(newText.trim()) == false)
			errorMessage = WorkingSetMessages.ScriptWorkingSetPage_warning_nameWhitespace;
		if (newText.equals("")) { //$NON-NLS-1$
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck = false;
				return;
			}
			errorMessage = WorkingSetMessages.ScriptWorkingSetPage_warning_nameMustNotBeEmpty;
		}

		fFirstCheck = false;

		if (errorMessage == null && (fWorkingSet == null
				|| newText.equals(fWorkingSet.getName()) == false)) {
			IWorkingSet[] workingSets = PlatformUI.getWorkbench()
					.getWorkingSetManager().getWorkingSets();
			for (int i = 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage = WorkingSetMessages.ScriptWorkingSetPage_warning_workingSetExists;
				}
			}
		}

		if (!hasCheckedElement())
			infoMessage = WorkingSetMessages.ScriptWorkingSetPage_warning_resourceMustBeChecked;

		setMessage(infoMessage, INFORMATION);
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

	private boolean hasCheckedElement() {
		TreeItem[] items = fTree.getTree().getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getChecked())
				return true;
		}
		return false;
	}

	private void findCheckedElements(List checkedResources, Object parent) {
		Object[] children = fTreeContentProvider.getChildren(parent);
		for (int i = 0; i < children.length; i++) {
			if (fTree.getGrayed(children[i]))
				findCheckedElements(checkedResources, children[i]);
			else if (fTree.getChecked(children[i]))
				checkedResources.add(children[i]);
		}
	}

	void handleCheckStateChange(final CheckStateChangedEvent event) {
		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
			IAdaptable element = (IAdaptable) event.getElement();
			boolean state = event.getChecked();
			fTree.setGrayed(element, false);
			if (isExpandable(element))
				setSubtreeChecked(element, state, state); // only check subtree
															// if state is set
															// to true

			updateParentState(element, state);
			validateInput();
		});
	}

	private void setSubtreeChecked(Object parent, boolean state,
			boolean checkExpandedState) {
		if (!(parent instanceof IAdaptable))
			return;
		IContainer container = ((IAdaptable) parent)
				.getAdapter(IContainer.class);
		if ((!fTree.getExpandedState(parent) && checkExpandedState)
				|| (container != null && !container.isAccessible()))
			return;

		Object[] children = fTreeContentProvider.getChildren(parent);
		for (int i = children.length - 1; i >= 0; i--) {
			Object element = children[i];
			if (state) {
				fTree.setChecked(element, true);
				fTree.setGrayed(element, false);
			} else
				fTree.setGrayChecked(element, false);
			if (isExpandable(element))
				setSubtreeChecked(element, state, true);
		}
	}

	private void updateParentState(Object child, boolean baseChildState) {
		if (child == null)
			return;
		if (child instanceof IAdaptable) {
			IResource resource = ((IAdaptable) child)
					.getAdapter(IResource.class);
			if (resource != null && !resource.isAccessible())
				return;
		}
		Object parent = fTreeContentProvider.getParent(child);
		if (parent == null)
			return;

		boolean allSameState = true;
		Object[] children = null;
		children = fTreeContentProvider.getChildren(parent);

		for (int i = children.length - 1; i >= 0; i--) {
			if (fTree.getChecked(children[i]) != baseChildState
					|| fTree.getGrayed(children[i])) {
				allSameState = false;
				break;
			}
		}

		fTree.setGrayed(parent, !allSameState);
		fTree.setChecked(parent, !allSameState || baseChildState);

		updateParentState(parent, baseChildState);
	}

	private void initializeCheckedState() {

		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
			Object[] elements;
			if (fWorkingSet == null) {
				// Use current part's selection for initialization
				IWorkbenchPage page = DLTKUIPlugin.getActivePage();
				if (page == null)
					return;

				IWorkbenchPart part = DLTKUIPlugin.getActivePage()
						.getActivePart();
				if (part == null)
					return;

				try {
					elements = SelectionConverter.getStructuredSelection(part)
							.toArray();
					for (int i1 = 0; i1 < elements.length; i1++) {
						if (elements[i1] instanceof IResource) {
							IModelElement je = ((IResource) elements[i1])
									.getAdapter(IModelElement.class);
							if (je != null && je.exists()
									&& je.getScriptProject().isOnBuildpath(
											(IResource) elements[i1]))
								elements[i1] = je;
						}
					}
				} catch (ModelException e) {
					return;
				}
			} else
				elements = fWorkingSet.getElements();

			// Use closed project for elements in closed project
			for (int i2 = 0; i2 < elements.length; i2++) {
				Object element1 = elements[i2];
				if (element1 instanceof IResource) {
					IProject project = ((IResource) element1).getProject();
					if (!project.isAccessible())
						elements[i2] = project;
				}
				if (element1 instanceof IModelElement) {
					IScriptProject jProject = ((IModelElement) element1)
							.getScriptProject();
					if (jProject != null
							&& !jProject.getProject().isAccessible())
						elements[i2] = jProject.getProject();
				}
			}

			fTree.setCheckedElements(elements);
			for (int i3 = 0; i3 < elements.length; i3++) {
				Object element2 = elements[i3];
				if (isExpandable(element2))
					setSubtreeChecked(element2, true, true);

				updateParentState(element2, true);
			}
		});
	}

	private boolean isExpandable(Object element) {
		return (element instanceof IScriptProject
				|| element instanceof IProjectFragment
				|| element instanceof IScriptFolder
				|| element instanceof IScriptModel
				|| element instanceof IContainer);
	}
}
