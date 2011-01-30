package jext2;
import java.nio.ByteBuffer;
import java.io.IOException;

import jext2.exceptions.InvalidArgument;
public class InodeAccess {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	
	public static Inode readFromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		int mode = Ext2fsDataTypes.getLE16(buf, offset);

		if (Mode.isDirectory(mode)) {
			DirectoryInode inode = DirectoryInode.fromByteBuffer(buf, offset);
			return inode;
		} else if (Mode.isRegular(mode)) {
			RegularInode inode = RegularInode.fromByteBuffer(buf, offset);
			return inode;
		} else if (Mode.isSymlink(mode)) {
			SymlinkInode inode = SymlinkInode.fromByteBuffer(buf, offset);
			return inode;
		} else {
			Inode inode = Inode.fromByteBuffer(buf, offset);
			return inode;
		}
	}

	public static Inode readByIno(long ino) throws IOException, InvalidArgument {
		if (ino == 0 || ino > superblock.getInodesCount()) {
			throw new InvalidArgument();
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

		// TODO check for NOENT exception
		// TODO add blockNr and offset to readFrom call
		
		inode.setBlockGroup(group);
		inode.setIno(ino);
		inode.setBlockNr(absBlock);
		inode.setOffset(relOffset);
		
		return inode;
		
	}
	
	public static Inode readRootInode() throws IOException {
	    try {
	        return readByIno(Constants.EXT2_ROOT_INO);
	    } catch (InvalidArgument e) {
	        throw new RuntimeException("should not happen");
	    }
	}
}