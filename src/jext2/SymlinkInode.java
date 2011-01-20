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
	
	protected SymlinkInode(int blockNr, int offset) throws IOException {
		super(blockNr, offset);
	}

	protected void read(ByteBuffer buf) throws IOException {
		super.read(buf);
		int size = getSize();
				
		if (isFastSymlink()) {
			symlink = Ext2fsDataTypes.getString(buf, 40 + offset, size);			
			
		} else { 
			StringBuffer sb = new StringBuffer();
			
			for (long nr : accessData().iterateBlocks()) {
				buf = blocks.read(nr);
				sb.append(Ext2fsDataTypes.getString(buf, 0, size));
				size -= superblock.getBlocksize();
			}
			
			symlink = sb.toString();
		}
		
		
	}
	
	
	public boolean isFastSymlink() {
	    return (getBlocks() == 0);
	}
	
	
	public static SymlinkInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		SymlinkInode inode = new SymlinkInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	
}
