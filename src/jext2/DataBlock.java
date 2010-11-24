package jext2;

import java.nio.ByteBuffer;
import java.util.List;
import java.nio.ByteOrder;
import java.io.IOException;
import java.util.Iterator;

public class DataBlock {
	protected static Superblock superblock = Superblock.getInstance();
	protected static BlockAccess blocks = BlockAccess.getInstance();

	private static int getBlockNumberFromDataBlock(int dataBlock, int index) throws IOException{
		ByteBuffer buffer = blocks.getAtOffset(dataBlock);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int nr = buffer.getInt(index*4);
		return nr;
	}
	
	public static int getIndirect(int ind, int nr) throws IOException {
		int block = getBlockNumberFromDataBlock(ind, nr);
		return block;
	}

	public static int getDoubleIndirect(int dint, int nr) throws IOException {
		int addrPerBlock = superblock.getBlocksize()/4;
		int ind = getIndirect(dint, nr / addrPerBlock);
		int block = getIndirect(ind, nr % addrPerBlock);
		return block;
	}

	public static int getTripleIndirect(int tind, int nr) throws IOException{
		int addrPerBlock = superblock.getBlocksize()/4;
		int dind = getDoubleIndirect(tind, nr / addrPerBlock);
		int block = getIndirect(dind, nr % addrPerBlock);
		return block;
	}

	static class DataBlockIterator implements Iterator<Integer> {
		Inode inode;
		int current;

		DataBlockIterator(Inode inode, int offset) {
			this.inode = inode;
			this.current = offset - 1;
		}
			
		DataBlockIterator(Inode inode) {
			this.inode = inode;
			this.current = -1;
		}

		public boolean hasNext() {
			try {
				return (getBlockNumber(this.inode, current + 1) != 0);
			} catch (IOException e) {
				return false;
			}
		}

		public Integer next() {
			current += 1;
			try {
				return getBlockNumber(this.inode, current);
			} catch (IOException e) {
				return null;
			}
		}

		public void remove() {
		}
	}

	public static DataBlockIterator iterateDataBlocks(Inode inode) {
		return new DataBlockIterator(inode);
	}

	public static DataBlockIterator iterateDataBlocks(Inode inode, int offset) {
		return new DataBlockIterator(inode, offset);
	}
		
	
	/** get the logical block number of file block */
	public static int getBlockNumber(Inode inode, int fileBlockNumber) throws IOException{
		int[] directBlocks = inode.getBlock();
		int addrPerBlock = superblock.getBlocksize()/4;
			
		// direct
		if (fileBlockNumber < Constants.EXT2_NDIR_BLOCKS) {
			return directBlocks[fileBlockNumber];
		}

		// indirect
		fileBlockNumber -= Constants.EXT2_NDIR_BLOCKS;
		if (fileBlockNumber < addrPerBlock) {
			return getIndirect(directBlocks[Constants.EXT2_IND_BLOCK],
			                   fileBlockNumber);
		}
		
		// double indirect
		fileBlockNumber -= addrPerBlock;
		if (fileBlockNumber < addrPerBlock*addrPerBlock) {
			return getDoubleIndirect(directBlocks[Constants.EXT2_DIND_BLOCK],
			                         fileBlockNumber);
		}
		
		// triple indirect
		fileBlockNumber -= addrPerBlock*addrPerBlock;
		return getTripleIndirect(directBlocks[Constants.EXT2_TIND_BLOCK],
		                         fileBlockNumber);
	}
}
