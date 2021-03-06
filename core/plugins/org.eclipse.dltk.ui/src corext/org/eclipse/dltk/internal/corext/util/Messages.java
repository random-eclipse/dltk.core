/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.corext.util;

import com.ibm.icu.text.MessageFormat;

/**
 * Helper class to format message strings.
 *
 *
 */
public class Messages {

	public static String format(String message, Object... objects) {
		return MessageFormat.format(message, objects);
	}

	private Messages() {
		// Not for instantiation
	}
}
