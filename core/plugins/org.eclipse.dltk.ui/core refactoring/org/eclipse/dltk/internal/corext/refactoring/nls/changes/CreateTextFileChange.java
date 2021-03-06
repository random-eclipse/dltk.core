/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.corext.refactoring.nls.changes;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dltk.core.IModelStatusConstants;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.corext.refactoring.nls.NLSUtil;

public class CreateTextFileChange extends CreateFileChange {

	private final String fTextType;

	public CreateTextFileChange(IPath path, String source, String encoding,
			String textType) {
		super(path, source, encoding);
		fTextType = textType;
	}

	public String getTextType() {
		return fTextType;
	}

	public String getCurrentContent() throws ModelException {
		IFile file = getOldFile(new NullProgressMonitor());
		if (!file.exists())
			return ""; //$NON-NLS-1$
		InputStream stream = null;
		try {
			stream = file.getContents();
			String c = NLSUtil.readString(stream);
			return (c == null) ? "" : c; //$NON-NLS-1$
		} catch (CoreException e) {
			throw new ModelException(e, IModelStatusConstants.CORE_EXCEPTION);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException x) {
			}
		}
	}

	public String getPreview() {
		return getSource();
	}
}
