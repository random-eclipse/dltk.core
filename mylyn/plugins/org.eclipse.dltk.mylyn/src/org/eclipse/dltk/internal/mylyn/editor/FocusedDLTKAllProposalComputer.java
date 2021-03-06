/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tasktop Technologies - changes for bug 219692
 *******************************************************************************/

package org.eclipse.dltk.internal.mylyn.editor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.CompletionProposal;
import org.eclipse.dltk.internal.mylyn.DLTKUiUtil;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.text.completion.ContentAssistInvocationContext;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalCollector;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalComputer;
import org.eclipse.dltk.ui.text.completion.ScriptContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;

/**
 * Based on org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer.
 *
 * @author Mik Kersten
 * @author Steffen Pingel
 */
// TODO e3.5 extend org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer
public class FocusedDLTKAllProposalComputer extends ScriptCompletionProposalComputer {

	/**
	 * @see CompletionProposal#METHOD_REF_WITH_CASTED_RECEIVER
	 */
	// TODO e3.4 replace by CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER
	public static final int METHOD_REF_WITH_CASTED_RECEIVER = 24;

	/**
	 * @see CompletionProposal#METHOD_REF_WITH_CASTED_RECEIVER
	 */
	// TODO e3.4 replace by CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER
	public static final int FIELD_REF_WITH_CASTED_RECEIVER = 25;

	/**
	 * @see CompletionProposal#CONSTRUCTOR_INVOCATION
	 */
	// TODO e3.5 replace by CompletionProposal.CONSTRUCTOR_INVOCATION
	public static final int CONSTRUCTOR_INVOCATION = 26;

	/**
	 * @see CompletionProposal#ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION
	 */
	// TODO e3.5 replace by CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION
	public static final int ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION = 27;

	public FocusedDLTKAllProposalComputer() {
		FocusedDLTKProposalProcessor.getDefault().addMonitoredComputer(this);
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		if (shouldReturnResults()) {
			List<ICompletionProposal> proposals = super.computeCompletionProposals(context, monitor);
			return FocusedDLTKProposalProcessor.getDefault().projectInterestModel(this, proposals);
		} else {
			return Collections.emptyList();
		}
	}

	private boolean shouldReturnResults() {
		if (DLTKUiUtil.isDefaultAssistActive(DLTKUiUtil.ASSIST_DLTK_ALL)) {
			// do not return duplicates if the default JDT processor is already enabled on Eclipse 3.5
			return false;
		}
		Set<String> disabledIds = DLTKUiUtil.getDisabledIds(DLTKUIPlugin.getDefault().getPreferenceStore());
		if (!disabledIds.contains(DLTKUiUtil.ASSIST_DLTK_NOTYPE)
				&& !disabledIds.contains(DLTKUiUtil.ASSIST_DLTK_TYPE)) {
			// do not return duplicates if the default JDT processors are already enabled on on Eclipse 3.3 and 3.4
			return false;
		}
		return true;
	}

	@Override
	protected ScriptCompletionProposalCollector createCollector(ScriptContentAssistInvocationContext context) {
		return null;
	}

	@Override
	protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
		int invocationOffset = context.getInvocationOffset();
		int typeContext = super.guessContextInformationPosition(context);
		int methodContext = guessMethodContextInformationPosition2(context);
		if (typeContext != invocationOffset && typeContext > methodContext) {
			return typeContext;
		} else if (methodContext != invocationOffset) {
			return methodContext;
		} else {
			return invocationOffset;
		}
	}

	// renamed, since guessMethodContextInformationPosition(ContentAssistInvocationContext) is final
	protected final int guessMethodContextInformationPosition2(ContentAssistInvocationContext context) {
		return context.getInvocationOffset();
	}

	@Override
	protected TemplateCompletionProcessor createTemplateProposalComputer(ScriptContentAssistInvocationContext context) {
		return null;
	}
}
