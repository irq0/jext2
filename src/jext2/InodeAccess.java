package jext2;
import java.nio.ByteBuffer;
import java.io.IOException;
public class InodeAccess {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	
	public static boolean mask(int mode, int mask) {
		return (mode & Constants.LINUX_S_IFMT) == mask;
	}	
	
	public static Inode readFromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		int mode = Ext2fsDataTypes.getLE16(buf, offset);

		if (mask(mode, Constants.LINUX_S_IFDIR)) {
			DirectoryInode inode = DirectoryInode.fromByteBuffer(buf, offset);
			return inode;
		} else if (mask(mode, Constants.LINUX_S_IFREG)) {
			RegularInode inode = RegularInode.fromByteBuffer(buf, offset);
			return inode;
		} else if (mask(mode, Constants.LINUX_S_IFLNK)) {
			SymlinkInode inode = SymlinkInode.fromByteBuffer(buf, offset);
			return inode;
		} else {
			Inode inode = Inode.fromByteBuffer(buf, offset);
			return inode;
		}
	}

	public static Inode readByIno(long ino) throws IOException {
		if (ino == 0 || ino > superblock.getInodesCount()) {
			return null;
		}
		
		int group = Calculations.groupOfIno(ino);
		int offset = Calculations.localInodeOffset(ino);
		int tblBlock = offset / superblock.getBlocksize();
	
		BlockGroupDescriptor descr = blockGroups.getGroupDescriptor(group);
	
		long absBlock = descr.getInodeTablePointer() + tblBlock;
		int relOffset = offset - (tblBlock * superblock.getBlocksize());
		
		if (absBlock < 0 || relOffset < 0) 
		    throw new IOException(); 
		
		ByteBuffer table = blocks.read(absBlock);
		Inode inode = InodeAccess.readFromByteBuffer(table, relOffset);

		inode.setBlockGroup(group);
		inode.setIno(ino);
		inode.setBlockNr(absBlock);
		inode.setOffset(relOffset);
		
		return inode;
		
	}
	
	public static Inode readRootInode() throws IOException {
		return readByIno(Constants.EXT2_ROOT_INO);
	}
}