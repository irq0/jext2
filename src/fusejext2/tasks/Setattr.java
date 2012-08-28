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

import java.util.Date;

import jext2.Constants;
import jext2.Inode;
import jext2.Mode;
import jext2.RegularInode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fuse.FuseConstants;
import fuse.Stat;
import fusejext2.Jext2Context;
import fusejext2.Util;

public class Setattr extends jlowfuse.async.tasks.Setattr<Jext2Context> {

	public Setattr(FuseReq req, long ino, Stat attr, int to_set, FileInfo fi) {
		super(req, ino, attr, to_set, fi);
	}

	private boolean checkToSet(int to_set, int attr) {
		return ((to_set & attr) != 0);
	}

	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;

		try {
			Inode inode = context.inodes.getOpened(ino);

			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_ATIME)) {
				inode.setAccessTime(Util.timespecToDate(attr.getAtim()));
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_ATIME_NOW)) {
				inode.setAccessTime(new Date());
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_GID)) {
				inode.setGid(attr.getGid());
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_MODE)) {
				inode.setMode(Mode.createWithNumericValue(attr.getMode()));
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_MTIME)) {
				inode.setModificationTime(Util.timespecToDate(attr.getMtim()));
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_MTIME_NOW)) {
				inode.setModificationTime(new Date());
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_SIZE)) {
				if (inode.isRegularFile()) {
					((RegularInode) inode).setSizeAndTruncate(attr.getSize());
				}
			}
			if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_UID)) {
				inode.setUid(attr.getUid());
			}

			inode.write();

			Stat s = Util.inodeToStat(context.superblock, inode);
			Reply.attr(req, s, 0.0);

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}

}
