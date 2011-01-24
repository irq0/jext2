package jext2;

import java.io.IOException;

/** 
 * Inode allocation and deallocation routines 
 * Adapted from the linux kernel implementation. The long comments on 
 * funcionality are mostly copied from linux or e2fsprogs   
 */
public class InodeAlloc {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	
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
	public static int findGroupDir(Inode parent) {
		int groupsCount = superblock.getGroupsCount();
		long averageFreeInodes = countFreeInodes() / groupsCount;
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
			return -1;
		else
			return bestNr;
	}


	public static int findGroupOther(Inode parent) {
		int groupsCount = superblock.getGroupsCount();
		int group = -1;
		BlockGroupDescriptor desc = null;
		
		// Try to place inode in its parent directory
		group = parent.getBlockGroup();
		desc = blockGroups.getGroupDescriptor(parent.getBlockGroup());
		if (desc != null && desc.getFreeInodesCount() > 0 &&
		    desc.getFreeBlocksCount() > 0) {
			return group;
		}
			
		/*
		 * We're going to place this inode in a different blockgroup from its
		 * parent.	We want to cause files in a common directory to all land in
		 * the same blockgroup.	 But we want files which are in a different
		 * directory which shares a blockgroup with our parent to land in a
		 * different blockgroup.
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

		return -1;
	}	

	/* Orlov's allocator for directories. 
	 * 
	 * We always try to spread first-level directories.
	 *
	 * If there are blockgroups with both free inodes and free blocks counts 
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

	
	/** Register Inode on disk. Find suitable position an reserve this position
	 * for the Inode. Finally set location data in Inode
	 */
	public static void registerInode(Inode dir, Inode inode) throws IOException {
		/* find best suitable block group */
		int group;
		
		if (Mode.isDirectory(inode.getMode())) {
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
			Bitmap bmap = blockGroups.readInodeBitmapOf(bgroup);
			
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
			
			bmap.setBit(ino, true);
			bmap.write();
			break;			
		} 
		
		/* apply changes to meta data */
		superblock.setFreeInodesCount(superblock.getFreeInodesCount() - 1);
		descr.setFreeInodesCount(descr.getFreeInodesCount() - 1);

		if (Mode.isDirectory(inode.getMode())) { 
			superblock.setDirsCount(superblock.getDirsCount() + 1);
			descr.setUsedDirsCount(descr.getUsedDirsCount() + 1);
		}
		
		superblock.write();
		descr.write();
		
		/* set location metadata of inode */
		int offset = (ino * superblock.getInodeSize()) % superblock.getBlocksize();
		long block = descr.getInodeTablePointer() + (ino * superblock.getInodeSize()) / superblock.getBlocksize();
		
		inode.setBlockGroup(group);
		inode.setBlockNr(block);		
		inode.setOffset(offset);
		inode.setIno(globalIno);
	}
	
}

