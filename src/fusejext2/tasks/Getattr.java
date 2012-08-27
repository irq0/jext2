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
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fuse.Stat;
import fusejext2.Jext2Context;
import fusejext2.Util;


public class Getattr extends jlowfuse.async.tasks.Getattr<Jext2Context> {

	public Getattr(FuseReq req, long ino, FileInfo fi) {
		super(req, ino, fi);
	}


	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		Inode inode = context.inodes.getOpened(ino);
		Stat stat = Util.inodeToStat(context.superblock, inode);
		Reply.attr(req, stat, 0.0);
	}
}
