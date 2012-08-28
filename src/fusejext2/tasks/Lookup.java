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

package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryEntry;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NotADirectory;
import fusejext2.Jext2Context;
import fusejext2.Util;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Lookup extends jlowfuse.async.tasks.Lookup<Jext2Context> {

	public Lookup(FuseReq req, long parent, String name) {
		super(req, parent, name);
	}

	@Override
	public void run() {
		long parent = this.parent;

		if (parent == 1) parent = Constants.EXT2_ROOT_INO;

		Inode parentInode = null;
		try {
			parentInode = context.inodes.openInode(parent);

			if (!parentInode.isDirectory())
				throw new NotADirectory();

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
			return;
		}

		DirectoryEntry entry = null;
		try {
			entry = ((DirectoryInode)parentInode).lookup(name);
			((DirectoryInode)parentInode).directoryEntries.release(entry);

			Inode child = context.inodes.openInode(entry.getIno());
			Reply.entry(req, Util.inodeToEntryParam(context.superblock, child));

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
