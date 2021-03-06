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
package org.eclipse.dltk.ui.templates;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.internal.ui.text.hover.SourceViewerInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

final public class TemplateInformationControlCreator implements
		IInformationControlCreator, IInformationControlCreatorExtension {

	private SourceViewerInformationControl fControl;

	/**
	 * The orientation to be used by this hover. Allowed values are:
	 * SWT#RIGHT_TO_LEFT or SWT#LEFT_TO_RIGHT
	 *
	 * @since 3.2
	 */
	private int fOrientation;

	private IDLTKLanguageToolkit fToolkit;

	/**
	 * @param orientation
	 *            the orientation, allowed values are: SWT#RIGHT_TO_LEFT or
	 *            SWT#LEFT_TO_RIGHT
	 */
	public TemplateInformationControlCreator(int orientation,
			IDLTKLanguageToolkit toolkit) {
		Assert.isLegal(orientation == SWT.RIGHT_TO_LEFT
				|| orientation == SWT.LEFT_TO_RIGHT);
		fOrientation = orientation;
		fToolkit = toolkit;
	}

	@Override
	public IInformationControl createInformationControl(Shell parent) {
		fControl = new SourceViewerInformationControl(parent, SWT.TOOL
				| fOrientation, SWT.NONE, fToolkit);
		fControl.addDisposeListener(e -> fControl = null);
		return fControl;
	}

	@Override
	public boolean canReuse(IInformationControl control) {
		return fControl == control && fControl != null;
	}

	@Override
	public boolean canReplace(IInformationControlCreator creator) {
		return (creator != null && getClass() == creator.getClass());
	}
}
