/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.editor.selectionaction;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.texteditor.IUpdate;


public class StructureSelectHistoryAction extends Action implements IUpdate {
	private ScriptEditor fEditor;
	private SelectionHistory fHistory;

	public StructureSelectHistoryAction(ScriptEditor editor, SelectionHistory history) {
		super(SelectionActionMessages.StructureSelectHistory_label);
		setToolTipText(SelectionActionMessages.StructureSelectHistory_tooltip);
		setDescription(SelectionActionMessages.StructureSelectHistory_description);
		Assert.isNotNull(history);
		Assert.isNotNull(editor);
		fHistory= history;
		fEditor= editor;
		update();
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.STRUCTURED_SELECTION_HISTORY_ACTION);
	}

	@Override
	public void update() {
		setEnabled(!fHistory.isEmpty());
	}

	@Override
	public void run() {
		ISourceRange old= fHistory.getLast();
		if (old != null) {
			try {
				fHistory.ignoreSelectionChanges();
				fEditor.selectAndReveal(old.getOffset(), old.getLength());
			} finally {
				fHistory.listenToSelectionChanges();
			}
		}
	}
}
