/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.dltk.ui.viewsupport;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.dltk.internal.corext.util.Messages;
import org.eclipse.dltk.internal.ui.DLTKUIMessages;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.dltk.ui.dialogs.StatusInfo;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/*package*/class HistoryListAction<E> extends Action {

	private class HistoryListDialog extends StatusDialog {
		private static final int MAX_MAX_ENTRIES = 100;
		private ListDialogField fHistoryList;
		private StringDialogField fMaxEntriesField;
		private int fMaxEntries;

		private E fResult;

		private HistoryListDialog() {
			super(fHistory.getShell());
			setTitle(fHistory.getHistoryListDialogTitle());

			createHistoryList();
			createMaxEntriesField();
			setHelpAvailable(false);
		}

		private void createHistoryList() {
			IListAdapter adapter = new IListAdapter() {
				@Override
				public void customButtonPressed(ListDialogField field,
						int index) {
					doCustomButtonPressed(index);
				}

				@Override
				public void selectionChanged(ListDialogField field) {
					doSelectionChanged();
				}

				@Override
				public void doubleClicked(ListDialogField field) {
					doDoubleClicked();
				}
			};
			String[] buttonLabels = new String[] {
					DLTKUIMessages.HistoryListAction_remove,
					DLTKUIMessages.HistoryListAction_remove_all };
			LabelProvider labelProvider = new TestRunLabelProvider();
			fHistoryList = new ListDialogField(adapter, buttonLabels,
					labelProvider);
			fHistoryList.setLabelText(fHistory.getHistoryListDialogMessage());

			List<E> historyEntries = fHistory.getHistoryEntries();
			fHistoryList.setElements(historyEntries);

			E currentEntry = fHistory.getCurrentEntry();
			ISelection sel;
			if (currentEntry != null) {
				sel = new StructuredSelection(currentEntry);
			} else {
				sel = new StructuredSelection();
			}
			fHistoryList.selectElements(sel);
		}

		private void createMaxEntriesField() {
			fMaxEntriesField = new StringDialogField();
			fMaxEntriesField.setLabelText(fHistory.getMaxEntriesMessage());
			fMaxEntriesField.setDialogFieldListener(field -> {
				String maxString = fMaxEntriesField.getText();
				boolean valid;
				try {
					fMaxEntries = Integer.parseInt(maxString);
					valid = fMaxEntries > 0 && fMaxEntries < MAX_MAX_ENTRIES;
				} catch (NumberFormatException e) {
					valid = false;
				}
				if (valid)
					updateStatus(StatusInfo.OK_STATUS);
				else
					updateStatus(new StatusInfo(IStatus.ERROR, Messages.format(
							DLTKUIMessages.HistoryListAction_max_entries_constraint,
							Integer.toString(MAX_MAX_ENTRIES))));
			});
			fMaxEntriesField
					.setText(Integer.toString(fHistory.getMaxEntries()));
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			initializeDialogUnits(parent);

			Composite composite = (Composite) super.createDialogArea(parent);

			Composite inner = new Composite(composite, SWT.NONE);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			inner.setFont(composite.getFont());

			LayoutUtil.doDefaultLayout(inner,
					new DialogField[] { fHistoryList, new Separator() }, true);
			LayoutUtil.setHeightHint(fHistoryList.getListControl(null),
					convertHeightInCharsToPixels(12));
			LayoutUtil.setHorizontalGrabbing(fHistoryList.getListControl(null));

			Composite additionalControls = new Composite(inner, SWT.NONE);
			additionalControls.setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, false));
			LayoutUtil.doDefaultLayout(additionalControls,
					new DialogField[] { fMaxEntriesField }, false);
			LayoutUtil.setHorizontalGrabbing(
					fMaxEntriesField.getTextControl(null));

			applyDialogFont(composite);
			return composite;
		}

		private void doCustomButtonPressed(int index) {
			switch (index) {
			case 0: // remove
				fHistoryList.removeElements(fHistoryList.getSelectedElements());
				fHistoryList.selectFirstElement();
				break;

			case 1: // remove all
				fHistoryList.removeAllElements();

			default:
				break;
			}
		}

		private void doDoubleClicked() {
			okPressed();
		}

		private void doSelectionChanged() {
			List<E> selected = fHistoryList.getSelectedElements();
			if (selected.size() >= 1) {
				fResult = selected.get(0);
			} else {
				fResult = null;
			}
			fHistoryList.enableButton(0, selected.size() != 0);
		}

		public E getResult() {
			return fResult;
		}

		public List<E> getRemaining() {
			return fHistoryList.getElements();
		}

		public int getMaxEntries() {
			return fMaxEntries;
		}

		@Override
		public void create() {
			setShellStyle(getShellStyle() | SWT.RESIZE);
			super.create();
		}

	}

	private final class TestRunLabelProvider extends LabelProvider {
		private final HashMap<ImageDescriptor, Image> fImages = new HashMap<>();

		@Override
		public String getText(Object element) {
			return fHistory.getText((E) element);
		}

		@Override
		public Image getImage(Object element) {
			ImageDescriptor imageDescriptor = fHistory
					.getImageDescriptor((E) element);
			return getCachedImage(imageDescriptor);
		}

		private Image getCachedImage(ImageDescriptor imageDescriptor) {
			Image cached = fImages.get(imageDescriptor);
			if (cached != null)
				return cached;
			Image image = imageDescriptor
					.createImage(fHistory.getShell().getDisplay());
			fImages.put(imageDescriptor, image);
			return image;
		}

		@Override
		public void dispose() {
			for (Image image : fImages.values()) {
				image.dispose();
			}
			fImages.clear();
		}
	}

	private ViewHistory<E> fHistory;

	public HistoryListAction(ViewHistory history) {
		super(null, IAction.AS_RADIO_BUTTON);
		fHistory = history;
		fHistory.configureHistoryListAction(this);
	}

	@Override
	public void run() {
		HistoryListDialog dialog = new HistoryListDialog();
		if (dialog.open() == Window.OK) {
			fHistory.setHistoryEntries(dialog.getRemaining(),
					dialog.getResult());
			fHistory.setMaxEntries(dialog.getMaxEntries());
		}
	}

}
