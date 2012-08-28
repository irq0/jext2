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

package fusejext2;

import java.util.Date;

import jext2.DataInode;
import jext2.Inode;
import jext2.Superblock;
import fuse.EntryParam;
import fuse.Stat;
import fuse.Timespec;

public class Util {
	public static Timespec dateToTimespec(Date date) {
		Timespec tim = new Timespec();
		tim.setSec((int)(date.getTime() / 1000));
		tim.setNsec(0);
		return tim;
	}

	public static Date timespecToDate(Timespec time) {
		return new Date(time.getSec()*1000 + time.getNsec()/1000);
	}

	public static Stat inodeToStat(Superblock superblock, Inode inode) {
		Stat s = new Stat();
		s.setUid(inode.getUidLow());
		s.setGid(inode.getGidLow());
		s.setIno(inode.getIno());
		s.setMode(inode.getMode().numeric());
		s.setBlksize(superblock.getBlocksize());
		s.setNlink(inode.getLinksCount());
		s.setSize(inode.getSize());

		if (inode.hasDataBlocks())
			s.setBlocks(((DataInode)inode).getBlocks());
		else
			s.setBlocks(0);

		s.setAtim(dateToTimespec(inode.getAccessTime()));
		s.setCtim(dateToTimespec(inode.getStatusChangeTime()));
		s.setMtim(dateToTimespec(inode.getModificationTime()));

		return s;
	}

	public static EntryParam inodeToEntryParam(Superblock superblock, Inode inode) {
		EntryParam e = new EntryParam();
		e.setAttr(inodeToStat(superblock, inode));
		e.setGeneration(inode.getGeneration());
		e.setAttr_timeout(0.0);
		e.setEntry_timeout(0.0);
		e.setIno(inode.getIno());
		return e;
	}

}
