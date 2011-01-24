package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SymlinkInode extends DataInode {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();
	private String symlink;
	
	public final String getSymlink() {
		return this.symlink;
	}
	
	protected SymlinkInode(long blockNr, int offset) throws IOException {
		super(blockNr, offset);
	}

	protected void read(ByteBuffer buf) throws IOException {
		super.read(buf);
		long size = getSize();
				
		if (isFastSymlink()) {
			symlink = Ext2fsDataTypes.getString(buf, 40 + offset, (int)size);			
			
		} else { 
			StringBuffer sb = new StringBuffer();
			
			for (long nr : accessData().iterateBlocks()) {
				buf = blocks.read(nr);
				sb.append(Ext2fsDataTypes.getString(buf, 0, 
				        (int)(size % superblock.getBlocksize())));
				size -= superblock.getBlocksize();
			}
			
			symlink = sb.toString();
		}
	}
	
	/**
	 * Check if inode is a fast symlink. A fast symlink stores the link in the
	 * data block pointers instead of data blocks itself
	 * 
	 * @return true if Inode is fast symlink; false otherwise.
	 */
	public boolean isFastSymlink() {
	    return (getBlocks() == 0);
	}
		
	public static SymlinkInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		SymlinkInode inode = new SymlinkInode(-1, offset);
		inode.read(buf);
		return inode;
	}	
	
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_SYMLINK;
	}
}