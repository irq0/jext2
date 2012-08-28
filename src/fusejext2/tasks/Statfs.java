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

import jext2.DirectoryEntry;
import jext2.Superblock;
import fuse.StatVFS;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Statfs extends jlowfuse.async.tasks.Statfs<Jext2Context> {

	public Statfs(FuseReq req, long ino) {
		super(req, ino);
	}

	@Override
	public void run() {
		StatVFS s = new StatVFS();

		Superblock superblock = context.superblock;

		s.setBsize(superblock.getBlocksize());
		s.setFrsize(superblock.getBlocksize());
		s.setBlocks(superblock.getBlocksCount() - superblock.getOverhead());
		s.setBfree(superblock.getFreeBlocksCount());

		if (s.getBfree() >= superblock.getReservedBlocksCount())
			s.setBavail(superblock.getFreeBlocksCount() - superblock.getReservedBlocksCount());
		else
			s.setBavail(0);

		s.setFiles(superblock.getInodesCount());
		s.setFfree(superblock.getFreeInodesCount());
		s.setFavail(superblock.getFreeInodesCount());

		s.setFsid(0);
		s.setFlag(0);
		s.setNamemax(DirectoryEntry.MAX_NAME_LEN);

		Reply.statfs(req, s);
	}

}
