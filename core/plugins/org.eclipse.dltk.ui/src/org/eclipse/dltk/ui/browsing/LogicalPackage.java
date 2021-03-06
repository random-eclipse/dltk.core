/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.ui.browsing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;

/**
 * Contains a list of package fragments with the same name but residing in
 * different source folders of a unique Java project.
 */
public class LogicalPackage extends PlatformObject {

	private Set<IScriptFolder> fPackages;
	private String fName;
	private IScriptProject fScriptProject;

	public LogicalPackage(IScriptFolder fragment) {
		Assert.isNotNull(fragment);
		fPackages = new HashSet<>();
		fScriptProject = fragment.getScriptProject();
		Assert.isNotNull(fScriptProject);
		add(fragment);
		fName = fragment.getElementName();
	}

	public IScriptProject getScriptProject() {
		return fScriptProject;
	}

	public IScriptFolder[] getScriptFolders() {
		return fPackages.toArray(new IScriptFolder[fPackages.size()]);
	}

	public void add(IScriptFolder fragment) {
		Assert.isTrue(fragment != null
				&& fScriptProject.equals(fragment.getScriptProject()));
		fPackages.add(fragment);
	}

	public void remove(IScriptFolder fragment) {
		fPackages.remove(fragment);
	}

	public boolean contains(IScriptFolder fragment) {
		return fPackages.contains(fragment);
	}

	public String getElementName() {
		return fName;
	}

	public int size() {
		return fPackages.size();
	}

	/**
	 * Returns true if the given fragment has the same name and resides inside
	 * the same project as the other fragments in the LogicalPackage.
	 *
	 * @param fragment
	 * @return boolean
	 */
	public boolean belongs(IScriptFolder fragment) {

		if (fragment == null)
			return false;

		if (fScriptProject.equals(fragment.getScriptProject())) {
			return fName.equals(fragment.getElementName());
		}

		return false;
	}

	public boolean hasSubpackages() throws ModelException {
		for (Iterator<IScriptFolder> iter = fPackages.iterator(); iter
				.hasNext();) {
			IScriptFolder pack = iter.next();
			if (pack.hasSubfolders()) {
				return true;
			}
		}
		return false;
	}

	public boolean isDefaultPackage() {
		return fName.length() == 0;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LogicalPackage))
			return false;

		LogicalPackage lp = (LogicalPackage) o;
		if (!fScriptProject.equals(lp.getScriptProject()))
			return false;

		IScriptFolder[] fragments = lp.getScriptFolders();

		if (fragments.length != getScriptFolders().length)
			return false;

		// this works because a LogicalPackage cannot contain the same
		// IPackageFragment twice
		for (int i = 0; i < fragments.length; i++) {
			IScriptFolder fragment = fragments[i];
			if (!fPackages.contains(fragment))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		IScriptFolder[] fragments = getScriptFolders();
		return fScriptProject.hashCode()
				+ getHash(fragments, fragments.length - 1);
	}

	private int getHash(IScriptFolder[] fragments, int index) {
		if (index <= 0) {
			return fragments[0].hashCode() * 17;
		}
		return fragments[index].hashCode() * 17 + getHash(fragments, index - 1);
	}
}
