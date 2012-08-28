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
import jext2.InodeAccess;
import jext2.exceptions.InvalidArgument;
import jext2.exceptions.JExt2Exception;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Unlink extends jlowfuse.async.tasks.Unlink<Jext2Context> {

	public Unlink(FuseReq req, long parent, String name) {
		super(req, parent, name);
	}

	@Override
	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;

		try {
			if (name.equals(".") || name.equals(".."))
				throw new InvalidArgument();

			DirectoryInode parentInode = (DirectoryInode)(context.inodes.getOpened(parent));
			Inode child = context.inodes.openInode(parentInode.lookup(name).getIno());

			parentInode.unLinkOther(child, name);

			context.inodes.forgetInode(child.getIno(), 1);

			Reply.err(req, 0);
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}

}
