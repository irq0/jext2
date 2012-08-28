/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

package jext2;

import jext2.exceptions.JExt2Exception;

public class DirectoryEntryAccess extends DataStructureAccessProvider<String, DirectoryEntry> {
	DirectoryInode inode;


	private DirectoryEntryAccess(DirectoryInode inode) {
		super(100);
		this.inode = inode;
	}

	static DirectoryEntryAccess createForDirectoy(DirectoryInode inode) {
		return new DirectoryEntryAccess(inode);
	}

	public void add(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return;
		add(entry.getName(), entry);
	}

	public void remove(DirectoryEntry entry) {
		assert entry != null;

		remove(entry.getName());
	}

	public void release(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return;
//		assert hasEntry(entry);
		release(entry.getName());
	}

	public DirectoryEntry retain(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return null;
		assert hasEntry(entry);
		return retain(entry.getName());
	}

	public long usageCounter(DirectoryEntry entry) {
		if (entry.isUnused()) return 0;
		return usageCounter(entry.getName());
	}

	public DirectoryEntry retainAdd(DirectoryEntry entry) {
		assert entry != null;
		if (entry.isUnused()) return entry;

		DirectoryEntry result;
		
		Data d = table.get(entry.getName());
		if (d != null) {
			d.lock();
			d.usage += 1;
			result = d.value;
			d.unlock();

		} else {
			d = new Data();
			d.lock();
			d.usage = 1;
			d.value = entry;
			result = entry;

			table.put(entry.getName(), d);
			d.unlock();
		}

		return result;

	}

	public boolean hasEntry(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return true;
		boolean result;

		result = table.containsKey(entry.getName());

		return result;
	}

	@Override
	protected DirectoryEntry createInstance(String key) throws JExt2Exception {
		throw new RuntimeException("Don't use this function!");
	}

}
