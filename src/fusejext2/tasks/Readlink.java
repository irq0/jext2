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
import jext2.Inode;
import jext2.SymlinkInode;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.OperationNotPermitted;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Readlink extends jlowfuse.async.tasks.Readlink<Jext2Context> {

	public Readlink(FuseReq arg0, long arg1) {
		super(arg0, arg1);
	}

	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
			Inode inode = context.inodes.getOpened(ino);
			if (!inode.isSymlink())
				throw new OperationNotPermitted();

			Reply.readlink(req, ((SymlinkInode)inode).getSymlink());

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}

	}
}
