/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.dltk.ui.text.IColorManager;
import org.eclipse.dltk.ui.text.IColorManagerExtension;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * DLTK color manager.
 */
public class DLTKColorManager implements IColorManager, IColorManagerExtension {

	protected Map<String, RGB> fKeyTable = new HashMap<>(10);
	protected Map<Display, Map<RGB, Color>> fDisplayTable = new HashMap<>(2);

	/**
	 * Flag which tells if the colors are automatically disposed when the
	 * current display gets disposed.
	 */
	private boolean fAutoDisposeOnDisplayDispose;

	/**
	 * Creates a new DLTK color manager which automatically disposes the
	 * allocated colors when the current display gets disposed.
	 */
	public DLTKColorManager() {
		this(true);
	}

	/**
	 * Creates a new DLTK color manager.
	 *
	 * @param autoDisposeOnDisplayDispose
	 *            if <code>true</code> the color manager automatically disposes
	 *            all managed colors when the current display gets disposed and
	 *            all calls to
	 *            {@link org.eclipse.jface.text.source.ISharedTextColors#dispose()}
	 *            are ignored.
	 *
	 *
	 */
	public DLTKColorManager(boolean autoDisposeOnDisplayDispose) {
		fAutoDisposeOnDisplayDispose = autoDisposeOnDisplayDispose;
	}

	public void dispose(Display display) {
		Map<RGB, Color> colorTable = fDisplayTable.get(display);
		if (colorTable != null) {
			Iterator<Color> e = colorTable.values().iterator();
			while (e.hasNext()) {
				Color color = e.next();
				if (color != null && !color.isDisposed())
					color.dispose();
			}
		}
	}

	@Override
	public Color getColor(RGB rgb) {

		if (rgb == null)
			return null;

		final Display display = Display.getCurrent();
		Map<RGB, Color> colorTable = fDisplayTable.get(display);
		if (colorTable == null) {
			colorTable = new HashMap<>(10);
			fDisplayTable.put(display, colorTable);
			if (fAutoDisposeOnDisplayDispose) {
				display.disposeExec(() -> dispose(display));
			}
		}

		Color color = colorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}

		return color;
	}

	@Override
	public void dispose() {
		if (!fAutoDisposeOnDisplayDispose)
			dispose(Display.getCurrent());
	}

	@Override
	public Color getColor(String key) {

		if (key == null)
			return null;

		RGB rgb = fKeyTable.get(key);
		return getColor(rgb);
	}

	@Override
	public void bindColor(String key, RGB rgb) {
		Object value = fKeyTable.get(key);
		if (value != null)
			throw new UnsupportedOperationException();

		fKeyTable.put(key, rgb);
	}

	@Override
	public void unbindColor(String key) {
		fKeyTable.remove(key);
	}
}
