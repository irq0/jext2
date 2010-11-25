package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class RegInode extends Inode {
	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();

	public ByteBuffer read(int size, int offset) throws IOException {
		ByteBuffer result = ByteBuffer.allocateDirect(size);

		int startFileBlock = offset / superblock.getBlocksize();
		offset = offset % superblock.getBlocksize();
		
		Iterator<Integer> i = DataBlockAccess.iterateDataBlockNrStartingAt(this, startFileBlock);
		
		while (i.hasNext()) {
			int blockNr = i.next();
			ByteBuffer block = blocks.read(blockNr);

			block.position(offset);

			while (result.hasRemaining() && block.hasRemaining()) {
				result.put(block.get());
				offset++;
			}

			System.out.println( offset + " bytes copied");
			offset = 0;
		}

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
