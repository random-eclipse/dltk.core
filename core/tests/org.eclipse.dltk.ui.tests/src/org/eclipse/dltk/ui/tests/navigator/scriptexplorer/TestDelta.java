/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.ui.tests.navigator.scriptexplorer;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelElementDelta;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptModel;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;


/**
 * @author Jen's account
 *
 */
public class TestDelta implements IModelElementDelta {

	private int fKind;
	private IModelElement fElement;

	private IModelElementDelta[] fAffectedChildren;

	public TestDelta(int kind, IModelElement element) {
		fKind= kind;
		fElement= element;
	}
	
	@Override
	public IModelElementDelta[] getAddedChildren() {
		return null;
	}
	
	@Override
	public IModelElementDelta[] getAffectedChildren() {
		if (fAffectedChildren == null)
			return new IModelElementDelta[0];
		else
			return fAffectedChildren;
	}
	
	public IModelElementDelta[] getChangedChildren() {
		return null;
	}
	
	@Override
	public IModelElement getElement() {
		return fElement;
	}
	
	@Override
	public int getFlags() {
		return 0;
	}
	
	@Override
	public int getKind() {
		return fKind;
	}
	
	@Override
	public IModelElement getMovedFromElement() {
		return null;
	}
	
	@Override
	public IModelElement getMovedToElement() {
		return null;
	}
	
	public IModelElementDelta[] getRemovedChildren() {
		return null;
	}
	
	@Override
	public IResourceDelta[] getResourceDeltas() {
		return null;
	}

	public void setAffectedChildren(IModelElementDelta[] children) {
		fAffectedChildren= children;
	}
	
	public static TestDelta createParentDeltas(IScriptFolder frag, TestDelta delta) {
		IModelElement root= frag.getParent();
		TestDelta rootDelta= new TestDelta(IModelElementDelta.CHANGED, root);

		IScriptProject proj= root.getScriptProject();
		TestDelta projectDelta= new TestDelta(IModelElementDelta.CHANGED, proj);

		IScriptModel model= proj.getModel();
		TestDelta modelDelta= new TestDelta(IModelElementDelta.CHANGED, model);

		//set affected children
		modelDelta.setAffectedChildren(new IModelElementDelta[] { projectDelta });
		projectDelta.setAffectedChildren(new IModelElementDelta[] { rootDelta });
		rootDelta.setAffectedChildren(new IModelElementDelta[] { delta });
		return modelDelta;
	}

	public static IModelElementDelta createCUDelta(ISourceModule[] cu, IScriptFolder parent, int action) {
		TestDelta fragmentDelta= new TestDelta(IModelElementDelta.CHANGED, parent);

		TestDelta[] deltas= new TestDelta[cu.length];
		for (int i= 0; i < cu.length; i++) {
			deltas[i]= new TestDelta(action, cu[i]);
		}

		fragmentDelta.setAffectedChildren(deltas);
		return createParentDeltas(parent, fragmentDelta);
	}

	public static IModelElementDelta createDelta(IScriptFolder frag, int action) {
		TestDelta delta= new TestDelta(action, frag);
		return createParentDeltas(frag, delta);
	}

}
