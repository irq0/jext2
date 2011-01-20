package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

/**
 * Access to the filesystem blocks  
 * */
public class BlockAccess {
	private int blocksize = Constants.EXT2_MIN_BLOCK_SIZE;
	private FileChannel blockdev;
	private static BlockAccess instance;
	
	
	public BlockAccess(FileChannel blockdev) {
		if (BlockAccess.instance != null) {
			throw new RuntimeException("BlockAccess is singleton!");
		} 
		this.blockdev = blockdev;
		BlockAccess.instance = this;
	}

	/** Read a block off size specified by setBlocksize() at logical address nr */
	public ByteBuffer read(long nr) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(blocksize);		
		buf.order(ByteOrder.BIG_ENDIAN);
		blockdev.read(buf,(((long)(nr & 0xffffffff)) * blocksize));

		return buf; 
	}	
	
	/**
	 * Read data from device to buffer starting at postion. To use this method set the
	 * limit and position of the buffer to your needs and note that position is
	 * not the block number. This method is indened for bulk data retrieval such as
	 * the inode.read()  
	 */
	public void readToBuffer(long position, ByteBuffer buf) throws IOException {
	   buf.order(ByteOrder.BIG_ENDIAN);
	   blockdev.read(buf, position);
	}

	/** Write a whole block to the logical address nr on disk */
	public void write(int nr, ByteBuffer buf) throws IOException {
		buf.rewind();
		blockdev.position(((long)(nr & 0xffffffff)) * blocksize);
		blockdev.write(buf);
        blockdev.force(true);
	}	
	
	
	@SuppressWarnings("unused")
    private void dumpByteBuffer(ByteBuffer buf) {
	    try {
	    while (buf.hasRemaining()) {
	        for (int i=0; i<8; i++) {
	            System.out.print(buf.get());
	            System.out.print("\t\t");
	        }
	        System.out.println();
	    }
	    } catch (Exception e) {
	        return;
	    }
	}   
	
	
	/** 
	 * Write partial buffer to a disk block. It is not possible to write over 
	 * the blocksize boundry. 
	 * @throws IOException 
	 */
	public void writePartial(int nr, int offset, ByteBuffer buf) throws IOException {
        if (offset + buf.limit() > blocksize)
            throw new IllegalArgumentException("attempt to write over block boundries" + buf + ", " + offset);

	    buf.rewind();
	    blockdev.write(buf, ((((long)(nr & 0xffffffff)) * blocksize) + offset));
	    blockdev.force(true);
	}
	
	public void writePartial(int nr, int offset, ByteBuffer buf, int bufOffset, int bufLength) throws IOException {
		if (offset + bufLength > blocksize)
			throw new IllegalArgumentException("attempt to write over block boundries" + buf + ", " + offset);

		ByteBuffer partial = ByteBuffer.allocate(bufLength);
		buf.position(bufOffset);
		for (int i=0; i<bufLength; i++) 
		    partial.put(buf.get());
		blockdev.write(partial, (((long)(nr & 0xffffffff)) * blocksize) + offset);
		blockdev.force(true);
	}
		
	public void setBlocksize(int blocksize) {
		this.blocksize = blocksize;
	}

	public static BlockAccess getInstance() {
		return BlockAccess.instance;
	}
	

	
	
	
}
