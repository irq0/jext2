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

public class Calculations {
	private static Superblock superblock = Superblock.getInstance();

	public static int groupOfBlk(long blk) {
		return (int)((blk - superblock.getFirstDataBlock()) /
				superblock.getBlocksPerGroup());
	}

	public static int groupIndexOfBlk(long blk) {
		return (int)((blk - superblock.getFirstDataBlock()) %
				superblock.getBlocksPerGroup());
	}


	public static int groupOfIno(long ino) {
		return (int)((ino - 1) / superblock.getInodesPerGroup());
	}

	public static int localInodeIndex(long ino) {
		return (int)((ino - 1) % superblock.getInodesPerGroup());
	}

	public static int localInodeOffset(long ino) {
		return (int)(((ino - 1) % superblock.getInodesPerGroup()) *
				superblock.getInodeSize());
	}

	public static long blockNrOfLocal(int index, long groupNr) {
		return index + groupNr * superblock.getBlocksPerGroup() +
				superblock.getFirstDataBlock();
	}

}
