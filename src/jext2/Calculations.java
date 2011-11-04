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
