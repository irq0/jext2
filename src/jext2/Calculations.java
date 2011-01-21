package jext2;

public class Calculations {
	private static Superblock superblock = Superblock.getInstance();
	
	public static long groupOfBlk(long blk) {
		return (blk - superblock.getFirstDataBlock()) /
			superblock.getBlocksPerGroup();
	}

	public static long groupOfIno(long ino) {
		return (ino - 1) / superblock.getInodesPerGroup();
	}

	public static long localInodeIndex(long ino) {
		return (ino - 1) % superblock.getInodesPerGroup();
	}

	public static long localInodeOffset(long ino) {
		return ((ino - 1) % superblock.getInodesPerGroup()) *
			superblock.getInodeSize();
	}

	public static int blockPerInodeTable() {
		return (int)(superblock.getInodesPerGroup() / superblock.getInodeSize());
	}
	
	public static long blockNrOfLocal(int index, long groupNr) {
	    return index + groupNr * superblock.getBlocksPerGroup() + 
	        superblock.getFirstDataBlock();
	}

}
