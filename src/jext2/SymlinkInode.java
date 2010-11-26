package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SymlinkInode extends Inode {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();
	private String symlink;
	
	protected SymlinkInode(int blockNr, int offset) {
		super(blockNr, offset);
	}

	protected void read(ByteBuffer buf) throws IOException {
		super.read(buf);
		int size = getSize();
				
		if (getBlocks() == 0) { // fast symlink
			symlink = Ext2fsDataTypes.getString(buf, 40 + offset, size);			
			
		} else { // slow symlink
			StringBuffer sb = new StringBuffer();
			
			for (int nr : DataBlockAccess.iterateDataBlockNr(this)) {
				buf = blocks.read(nr);
				sb.append(Ext2fsDataTypes.getString(buf, 0, size));
				size -= superblock.getBlocksize();
			}
			
			symlink = sb.toString();
		}
		
		
	}
	
	
	public static SymlinkInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		SymlinkInode inode = new SymlinkInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	
}
