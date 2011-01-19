package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class RegInode extends Inode {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();

	
	public int writeData(ByteBuffer buf, int offset) throws IOException {
	    System.out.println("WRITE DATA: " + buf + " AT: " + offset);
	    
	    int start = offset / superblock.getBlocksize();
	    int max = buf.capacity() / superblock.getBlocksize() + start + 1;
	    buf.rewind();

	    System.out.println("START: " + start + " MAX: " + max);
	    
	    LinkedList<Long> blockNrs = new LinkedList<Long>();
	    
	    /* get all the blocks needed to hold buf */
	    while (start < max) {
	        LinkedList<Long> b = DataBlockAccess.getBlocksAllocate(this, start, max-start);
	        
	        if (b == null) 
	            break;
	        
	        start += b.size();
	        blockNrs.addAll(b);
	    }

	    /* iterate blocks and write buf */
	    int blocksize = superblock.getBlocksize();
	    int blockOffset = offset % blocksize;
	    int bufOffset = 0;
	    int remaining = buf.capacity();
	    for (long nr : blockNrs) {
	        int bytesToWrite = remaining;
	        if (bytesToWrite > blocksize)
	            bytesToWrite = blocksize - blockOffset;
	        
	        blocks.writePartial((int) nr, blockOffset, buf, bufOffset, bytesToWrite); 
	        
	        remaining -= bytesToWrite;
	        bufOffset += bytesToWrite;
	        blockOffset = 0;
	    }
	    return bufOffset;
	}
	
	
	public ByteBuffer readData(int size, int offset) throws IOException {
		ByteBuffer result = ByteBuffer.allocateDirect(size);

		int start = offset / superblock.getBlocksize();
		int max = size / superblock.getBlocksize() + start;
		offset = offset % superblock.getBlocksize();		
		LinkedList<Long> blockNrs = new LinkedList<Long>();   
		
		while (start < max) { 
			LinkedList<Long> b = DataBlockAccess.getBlocks(this, start, max-start);
			
			// getBlocks returns null in case create=false and the block does not exist. FUSE can
			// and will request not existing blocks. 
			if (b == null) {
                break;
			}
						
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
