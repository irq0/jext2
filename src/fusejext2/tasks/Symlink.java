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
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.InodeAlloc;
import jext2.SymlinkInode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FuseContext;
import fusejext2.Jext2Context;
import fusejext2.Util;

public class Symlink extends jlowfuse.async.tasks.Symlink<Jext2Context> {

	public Symlink(FuseReq arg0, String arg1, long arg2, String arg3) {
		super(arg0, arg1, arg2, arg3);
	}


	@Override
	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;
		try {
			DirectoryInode parentInode = (DirectoryInode)context.inodes.getOpened(parent);

			FuseContext fuseContext = req.getContext();
			SymlinkInode inode = SymlinkInode.createEmpty();
			inode.setUid(fuseContext.getUid());
			inode.setGid(fuseContext.getGid());
			InodeAlloc.registerInode(parentInode, inode);
			inode.write();

			parentInode.addLink(inode, name);
			inode.setSymlink(link);

			context.inodes.retainInode(inode.getIno());

			Reply.entry(req, Util.inodeToEntryParam(context.superblock, inode));
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
