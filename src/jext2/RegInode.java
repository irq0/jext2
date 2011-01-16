package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

public class RegInode extends Inode {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();

	public ByteBuffer readData(int size, int offset) throws IOException {
		ByteBuffer result = ByteBuffer.allocateDirect(size);

		int start = offset / superblock.getBlocksize();
		int max = size / superblock.getBlocksize() + start;
		offset = offset % superblock.getBlocksize();		
		LinkedList<Long> blockNrs = new LinkedList<Long>();   
		
		while (start < max) { 
			LinkedList<Long> b = DataBlockAccess.getBlocks(this, start, max-start, false);
						
			start += b.size();
			
			blockNrs.addAll(b);
		}
		
		for (long nr : blockNrs) {
			ByteBuffer block = blocks.read((int)nr);
			block.position(offset);

			while (result.hasRemaining() && block.hasRemaining()) {
					result.put(block.get());
			}

			offset = 0;
		}
		
		result.rewind();
		return result;
	}

	protected RegInode(int blockNr, int offset) throws IOException {
		super(blockNr, offset);
	}
	
	public static RegInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		RegInode inode = new RegInode(-1, offset);
		inode.read(buf);
		return inode;
	}
}	
