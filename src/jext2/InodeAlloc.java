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

import java.util.logging.Level;
import java.util.logging.Logger;

import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NoSpaceLeftOnDevice;

/**
 * Inode allocation and deallocation routines
 * Adapted from the linux kernel implementation. The long comments on
 * functionality are mostly copied from linux or e2fsprogs
 */
public class InodeAlloc {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	private static BitmapAccess bitmaps = BitmapAccess.getInstance();

	private static Logger logger = Filesystem.getLogger();

	public static long countFreeInodes() {
		long count = 0;
		for (BlockGroupDescriptor group : blockGroups.iterateBlockGroups()) {
			count += group.getFreeInodesCount();
		}
		return count;
	}

	/*
	 * From linux 2.6.36 source:
	 * There are two policies for allocating an inode.	If the new inode is
	 * a directory, then a forward search is made for a block group with both
	 * free space and a low directory-to-inode ratio; if that fails, then of
	 * the groups with above-average free space, that group with the fewest
	 * directories already is chosen.
	 *
	 * For other inodes, search forward from the parent directory\'s block
	 * group to find a free inode.
	 */
	public static int findGroupDir(Inode parent) throws NoSpaceLeftOnDevice {
		int groupsCount = superblock.getGroupsCount();
		long averageFreeInodes = superblock.getFreeInodesCount() / groupsCount;
		BlockGroupDescriptor bestGroup = null;
		int bestNr = -1;

		for (BlockGroupDescriptor group : blockGroups.iterateBlockGroups()) {
			if (group.getFreeInodesCount() < averageFreeInodes)
				continue;
			if (bestGroup == null ||
					group.getFreeBlocksCount() > bestGroup.getFreeBlocksCount()) {
				bestGroup = group;
				bestNr = group.getBlockGroup();
			}
		}

		if (bestGroup == null)
			throw new NoSpaceLeftOnDevice();
		else
			return bestNr;
	}


	public static int findGroupOther(Inode parent) throws NoSpaceLeftOnDevice {
		int groupsCount = superblock.getGroupsCount();

		// Try to place inode in its parent directory
		int group = parent.getBlockGroup();
		BlockGroupDescriptor desc = blockGroups.getGroupDescriptor(parent.getBlockGroup());
		if (desc != null && desc.getFreeInodesCount() > 0 &&
				desc.getFreeBlocksCount() > 0) {
			return group;
		}

		/*
		 * We're going to place this inode in a different block group from its
		 * parent.	We want to cause files in a common directory to all land in
		 * the same block group. But we want files which are in a different
		 * directory which shares a block group with our parent to land in a
		 * different block group.
		 *
		 * So add our directory's i_ino into the starting point for the hash.
		 */
		group = (int)((group + parent.getIno()) % groupsCount);

		/*
		 * Use a quadratic hash to find a group with a free inode and some
		 * free blocks.
		 */
		for (int i=1; i<groupsCount; i = i << 1) {
			group += i;
			if (group >= groupsCount)
				group -= groupsCount;
			desc = blockGroups.getGroupDescriptor(group);
			if (desc != null && desc.getFreeInodesCount() > 0 &&
					desc.getFreeBlocksCount() > 0)
				return group;
		}

		/*
		 * That failed: try linear search for a free inode, even if that group
		 * has no free blocks.
		 */
		group = parent.getBlockGroup();
		for (int i=0; i<groupsCount; i++) {
			group += 1;

			if (group >= groupsCount)
				group = 0;

			desc = blockGroups.getGroupDescriptor(group);
			if (desc.getFreeInodesCount() > 0)
				return group;
		}

		throw new NoSpaceLeftOnDevice();
	}

	/* Orlov's allocator for directories.
	 *
	 * We always try to spread first-level directories.
	 *
	 * If there are block groups with both free inodes and free blocks counts
	 * not worse than average we return one with smallest directory count.
	 * Otherwise we simply return a random group.
	 *
	 * For the rest rules look so:
	 *
	 * It's OK to put directory into a group unless
	 * it has too many directories already (max_dirs) or
	 * it has too few free inodes left (min_inodes) or
	 * it has too few free blocks left (min_blocks) or
	 * it's already running too large debt (max_debt).
	 * Parent's group is preferred, if it doesn't satisfy these
	 * conditions we search cyclically through the rest. If none
	 * of the groups look good we just look for a group with more
	 * free inodes than average (starting at parent's group).
	 *
	 * Debt is incremented each time we allocate a directory and decremented
	 * when we allocate an inode, within 0--255.
	 */

	public static int findGroupOrlov(Inode parent) {
		// TODO implement me
		return -1;
	}

	/**
	 * Free Inode: Remove data blocks and set bit to 0
	 * @throws JExt2Exception
	 */
	static void freeInode(Inode inode) throws JExt2Exception {
		long ino = inode.getIno();

		if (ino < superblock.getFirstIno() ||
				ino > superblock.getInodesCount()) {
			throw new RuntimeException("reserved or nonexistent inode " + ino);
		}

		BlockGroupDescriptor groupDescr =
				blockGroups.getGroupDescriptor(Calculations.groupOfIno(ino));
		Bitmap bitmap = bitmaps.openInodeBitmap(groupDescr);
		int bit = Calculations.localInodeIndex(ino);

		if (!bitmap.isSet(bit)) {
			throw new RuntimeException("Bit allready cleared for inode " + ino);
		} else {
			bitmap.setBit(bit, false);
			bitmap.write();
		}

		bitmaps.closeBitmap(bitmap);

		groupDescr.setFreeBlocksCount(groupDescr.getFreeInodesCount() + 1);

		if (inode.isDirectory()) {
			groupDescr.setUsedDirsCount(groupDescr.getUsedDirsCount() - 1);
			superblock.setDirsCount(superblock.getDirsCount() - 1);
		}

		if (logger.isLoggable(Level.FINE)) {
			String s = new StringBuilder()
				.append("Freed Inode #")
				.append(ino)
				.append(" in block group ")
				.append(groupDescr.getBlockGroup())
				.append(" stored in block ")
				.append(inode.getBlockNr())
				.append("-")
				.append(inode.getOffset())
				.toString();
			logger.fine(s);
		}

		InodeAccess.getInstance().removeInode(ino);
	}

	/** Register Inode on disk. Find suitable position an reserve this position
	 * for the Inode. Finally set location data in Inode
	 * @throws JExt2Exception
	 */
	public static synchronized void registerInode(Inode dir, Inode inode) throws JExt2Exception {
		assert inode.getIno() == -1 : "It's my job to set the ino!";

		/* find best suitable block group */
		int group;

		if (inode.isDirectory()) {
			group = InodeAlloc.findGroupDir(dir);
		} else {
			group = InodeAlloc.findGroupOther(dir);
		}

		if (group == -1)
			throw new RuntimeException("No group found");

		BlockGroupDescriptor descr = blockGroups.getGroupDescriptor(group);

		/* find free inode slot in block groups starting at $group */
		int ino;
		long globalIno;
		if (group == 0)
			ino = (int)(superblock.getFirstIno());
		else
			ino = 0;

		while (true) {
			BlockGroupDescriptor bgroup = blockGroups.getGroupDescriptor(group);
			Bitmap bmap = bitmaps.openInodeBitmap(bgroup);

			ino = bmap.getNextZeroBitPos(ino);
			if (ino > superblock.getInodesPerGroup()) {
				group++;
				continue;
			}

			globalIno = ino + (group * superblock.getInodesPerGroup() + 1);
			if (globalIno < superblock.getFirstIno() ||
					globalIno > superblock.getInodesCount()) {
				continue;
			}

			InodeAccess inodes = InodeAccess.getInstance();
			if (inodes.getOpened(globalIno) != null) {
				logger.warning("Found inode in cache with same number - inode caching is broken :(");
				inodes.remove(globalIno);
			}

			bmap.setBit(ino, true);
			bmap.write();
			bitmaps.closeBitmap(bmap);
			break;
		}

		/* apply changes to meta data */
		superblock.setFreeInodesCount(superblock.getFreeInodesCount() - 1);
		descr.setFreeInodesCount(descr.getFreeInodesCount() - 1);

		if (inode.getMode().isDirectory()) {
			superblock.setDirsCount(superblock.getDirsCount() + 1);
			descr.setUsedDirsCount(descr.getUsedDirsCount() + 1);
		}

		/* set location metadata of inode */
		int offset = (ino * superblock.getInodeSize()) % superblock.getBlocksize();
		long block = descr.getInodeTablePointer() +
				(ino * superblock.getInodeSize()) / superblock.getBlocksize();

		inode.setBlockGroup(group);
		inode.setBlockNr(block);
		inode.setOffset(offset);
		inode.setIno(globalIno);

		if (logger.isLoggable(Level.FINE)) {
			String s = new StringBuilder()
				.append("Registered Inode #")
				.append(globalIno)
				.append(" in block group ")
				.append(group)
				.append(" stored in block ")
				.append(block)
				.append("-")
				.append(offset)
				.toString();
			logger.fine(s);
		}
	}
}

