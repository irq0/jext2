package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * access to filesystem blocks - smallest access unit is a block which depends on 
 * filesystem.
 */
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
		
		blockdev.position(((long)(nr & 0xffffffff)) * blocksize);
		blockdev.read(buf);

		return buf; 
	}	

	/** Write a block to the logical address nr on disk */
	public void write(int nr, ByteBuffer buf) throws IOException {
		buf.rewind();
		blockdev.position(((long)(nr & 0xffffffff)) * blocksize);
		blockdev.write(buf);
	}	
	
	/** Write only part of a block */
	public void write(int nr, ByteBuffer buf, int offset) throws IOException {
		buf.rewind();
		if (offset + buf.capacity() > blocksize)
			throw new IllegalArgumentException("attempt to write over block boundries" + buf + ", " + offset);
		System.out.println(offset);
		blockdev.position((((long)(nr & 0xffffffff)) * blocksize) + offset);
		blockdev.write(buf);
	}
		
	
	public void setBlocksize(int blocksize) {
		this.blocksize = blocksize;
	}

	public static BlockAccess getInstance() {
		return BlockAccess.instance;
	}
	
}
