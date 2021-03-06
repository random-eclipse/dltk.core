/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.core.IBuffer;
import org.eclipse.dltk.internal.core.util.LRUCache;

/**
 * An LRU cache of <code>IBuffers</code>.
 */
public class BufferCache extends OverflowingLRUCache {

	private ThreadLocal<List<IBuffer>> buffersToClose = new ThreadLocal<>();

	/**
	 * Constructs a new buffer cache of the given size.
	 */
	public BufferCache(int size) {
		super(size);
	}

	/**
	 * Constructs a new buffer cache of the given size.
	 */
	public BufferCache(int size, int overflow) {
		super(size, overflow);
	}

	/**
	 * Returns true if the buffer is successfully closed and removed from the
	 * cache, otherwise false.
	 *
	 * <p>
	 * NOTE: this triggers an external removal of this buffer by closing the
	 * buffer.
	 */
	@Override
	protected boolean close(LRUCacheEntry entry) {
		IBuffer buffer = (IBuffer) entry._fValue;

		// prevent buffer that have unsaved changes or working copy buffer to be
		// removed see https://bugs.eclipse.org/bugs/show_bug.cgi?id=39311
		if (!((Openable) buffer.getOwner())
				.canBufferBeRemovedFromCache(buffer)) {
			return false;
		} else {
			List<IBuffer> buffers = this.buffersToClose.get();
			if (buffers == null) {
				buffers = new ArrayList<>();
				this.buffersToClose.set(buffers);
			}
			buffers.add(buffer);
			return true;
		}
	}

	void closeBuffers() {
		List<IBuffer> buffers = this.buffersToClose.get();
		if (buffers == null)
			return;
		this.buffersToClose.set(null);
		for (int i = 0, length = buffers.size(); i < length; i++) {
			buffers.get(i).close();
		}
	}

	/**
	 * Returns a new instance of the receiver.
	 */
	@Override
	protected LRUCache newInstance(int size, int newOverflow) {
		return new BufferCache(size, newOverflow);
	}
}
