/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.dbgp.internal.commands;

import java.net.URI;

import org.eclipse.dltk.dbgp.DbgpBaseCommands;
import org.eclipse.dltk.dbgp.DbgpRequest;
import org.eclipse.dltk.dbgp.IDbgpCommunicator;
import org.eclipse.dltk.dbgp.commands.IDbgpSourceCommands;
import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.dbgp.internal.utils.DbgpXmlParser;
import org.w3c.dom.Element;

public class DbgpSourceCommands extends DbgpBaseCommands
		implements IDbgpSourceCommands {

	private static final String SOURCE_COMMAND = "source"; //$NON-NLS-1$

	public DbgpSourceCommands(IDbgpCommunicator communicator) {
		super(communicator);
	}

	protected String parseResponseXml(Element response) throws DbgpException {
		boolean success = DbgpXmlParser.parseSuccess(response);

		if (success) {
			return DbgpXmlParser.parseBase64Content(response);
		}

		return null;
	}

	protected String getSource(URI uri, Integer beginLine, Integer endLine)
			throws DbgpException {
		DbgpRequest request = createRequest(SOURCE_COMMAND);

		if (beginLine != null) {
			request.addOption("-b", beginLine); //$NON-NLS-1$
		}
		if (endLine != null) {
			request.addOption("-e", endLine); //$NON-NLS-1$
		}

		request.addOption("-f", uri.toString()); //$NON-NLS-1$

		return parseResponseXml(communicate(request));
	}

	@Override
	public String getSource(URI uri) throws DbgpException {
		return getSource(uri, null, null);
	}

	@Override
	public String getSource(URI uri, int beginLine) throws DbgpException {
		return getSource(uri, Integer.valueOf(beginLine), null);
	}

	@Override
	public String getSource(URI uri, int beginLine, int endLine)
			throws DbgpException {
		return getSource(uri, Integer.valueOf(beginLine),
				Integer.valueOf(endLine));
	}
}
