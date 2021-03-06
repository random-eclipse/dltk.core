/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.debug.core.model;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.debug.core.model.IScriptDebugElement;
import org.eclipse.dltk.debug.core.model.IScriptDebugTarget;

public abstract class ScriptDebugElement extends PlatformObject
		implements IScriptDebugElement {

	@Override
	public IScriptDebugTarget getScriptDebugTarget() {
		return (IScriptDebugTarget) getDebugTarget();
	}

	@Override
	public ILaunch getLaunch() {
		return getDebugTarget().getLaunch();
	}

	@Override
	public String getModelIdentifier() {
		return getDebugTarget().getModelIdentifier();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IDebugElement.class) {
			return (T) this;
		}

		/*
		 * Not implemented currently
		 *
		 * if (adapter == IStepFilters.class) { return getDebugTarget(); }
		 */

		if (adapter == IDebugTarget.class) {
			return (T) getDebugTarget();
		}

		if (adapter == ITerminate.class) {
			return (T) getDebugTarget();
		}

		if (adapter == IScriptDebugTarget.class) {
			return (T) getScriptDebugTarget();
		}

		if (adapter == ILaunch.class) {
			return (T) getLaunch();
		}

		return super.getAdapter(adapter);
	}

	protected void abort(String message, Throwable e) throws DebugException {
		throw new DebugException(
				new Status(IStatus.ERROR, DLTKDebugPlugin.PLUGIN_ID,
						DebugPlugin.INTERNAL_ERROR, message, e));
	}

	protected DebugException makeNotSupported(String message, Throwable e)
			throws DebugException {
		return new DebugException(
				new Status(IStatus.ERROR, DLTKDebugPlugin.PLUGIN_ID,
						DebugException.NOT_SUPPORTED, message, e));
	}

	protected DebugException wrapDbgpException(String message,
			DbgpException e) {
		return new DebugException(
				new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
						DebugException.INTERNAL_ERROR, message, e));
	}

	protected DebugException wrapIOException(String message, IOException e) {
		return new DebugException(
				new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
						DebugException.INTERNAL_ERROR, message, e));
	}
}
