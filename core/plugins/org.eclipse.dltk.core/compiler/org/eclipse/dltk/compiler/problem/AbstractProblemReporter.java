/*******************************************************************************
 * Copyright (c) 2008, 2016 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.compiler.problem;

import org.eclipse.core.runtime.IAdaptable;

/**
 * The abstract implementation of {@link IProblemReporter} to simplify code in
 * tests, etc.
 */
public abstract class AbstractProblemReporter implements IProblemReporter,
		IAdaptable {

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

}
