package jext2;

public class Calculations {
	private static Superblock superblock = Superblock.getInstance();
	
	public static int groupOfBlk(int blk) {
		return (blk - superblock.getFirstDataBlock()) /
			superblock.getBlocksPerGroup();
	}

	public static int groupOfIno(int ino) {
		return (ino - 1) / superblock.getInodesPerGroup();
	}

	public static int localInodeIndex(int ino) {
		return (ino - 1) % superblock.getInodesPerGroup();
	}

	public static int localInodeOffset(int ino) {
		return ((ino - 1) % superblock.getInodesPerGroup()) *
			superblock.getInodeSize();
	}


	public static int blockPerInodeTable() {
		return superblock.getInodesPerGroup() / superblock.getInodeSize();
	}

}
