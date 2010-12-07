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

	/** Read a block of size specified by setBlocksize() at logical address nr */
	public ByteBuffer read(int nr) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(blocksize);		
		buf.order(ByteOrder.BIG_ENDIAN);
		
		blockdev.position(((long)(nr & 0xffffffff)) * blocksize);
		blockdev.read(buf);

		return buf; 
	}	

	public void setBlocksize(int blocksize) {
		this.blocksize = blocksize;
	}

	public static BlockAccess getInstance() {
		return BlockAccess.instance;
	}
	
}
