/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.search;

import org.eclipse.dltk.internal.corext.util.Messages;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

public abstract class TextSearchLabelProvider extends LabelProvider {

	private AbstractTextSearchViewPage fPage;
	private String fMatchCountFormat;

	public TextSearchLabelProvider(AbstractTextSearchViewPage page) {
		fPage = page;
		fMatchCountFormat = SearchMessages.TextSearchLabelProvider_matchCountFormat;
	}

	@Override
	public final String getText(Object element) {
		int matchCount = fPage.getInput().getMatchCount(element);
		String text = doGetText(element);
		if (matchCount < 2) {
			return text;
		}
		return Messages.format(fMatchCountFormat,
				new Object[] { text, Integer.valueOf(matchCount) });
	}

	protected abstract String doGetText(Object element);
}
