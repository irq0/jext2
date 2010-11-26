package jext2;
import java.nio.ByteBuffer;
import java.io.IOException;
class InodeAccess {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	
	private static boolean mask(int mode, int mask) {
		return (mode & Constants.LINUX_S_IFMT) == mask;
	}	
	
	public static Inode readFromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		Inode inode = Inode.fromByteBuffer(buf, offset);
		int mode = inode.getMode();

		if (mask(mode, Constants.LINUX_S_IFDIR)) {
			DirectoryInode newInode = DirectoryInode.fromByteBuffer(buf, offset);
			return newInode;
		} else if (mask(mode, Constants.LINUX_S_IFREG)) {
			RegInode newInode = RegInode.fromByteBuffer(buf, offset);
			return newInode;
		} else if (mask(mode, Constants.LINUX_S_IFLNK)) {
			SymlinkInode newInode = SymlinkInode.fromByteBuffer(buf, offset);
			return newInode;
		} else {
			return inode;
		}
	}

	public static Inode readByIno(int ino) throws IOException {
		if (ino == 0 || ino > superblock.getInodesCount()) {
			return null;
		}
		
		int group = Calculations.groupOfIno(ino);
		int offset = Calculations.localInodeOffset(ino);
		int tblBlock = offset / superblock.getBlocksize();
	
		BlockGroup descr = blockGroups.getGroupDescriptor(group);
	
		int absBlock = descr.getInodeTable() + tblBlock;
		int relOffset = offset - (tblBlock * superblock.getBlocksize());
		
		ByteBuffer table = blocks.read(absBlock);
		Inode inode = InodeAccess.readFromByteBuffer(table, relOffset);
		
		return inode;
		
	}
	
	
	
}

