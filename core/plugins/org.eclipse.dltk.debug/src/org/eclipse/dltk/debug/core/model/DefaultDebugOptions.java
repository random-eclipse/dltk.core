/*******************************************************************************
 * Copyright (c) 2008, 2017 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.debug.core.model;

import org.eclipse.dltk.debug.core.IDebugOptions;

public class DefaultDebugOptions implements IDebugOptions {

	private static IDebugOptions defaultInstance = null;

	public static IDebugOptions getDefaultInstance() {
		if (defaultInstance == null) {
			defaultInstance = new DefaultDebugOptions();
		}
		return defaultInstance;
	}

	protected DefaultDebugOptions() {
		// empty
	}

	@Override
	public boolean get(BooleanOption option) {
		return option.getDefaultValue();
	}

	@Override
	public int get(IntegerOption option) {
		return option.getDefaultValue();
	}

	@Override
	public String get(StringOption option) {
		return option.getDefaultValue();
	}

	@Override
	public IScriptStackFrame[] filterStackLevels(IScriptStackFrame[] frames) {
		return frames.clone();
	}

	@Override
	public boolean isValidStack(IScriptStackFrame[] frames) {
		return true;
	}

}
