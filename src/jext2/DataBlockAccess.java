package jext2;

import java.nio.ByteBuffer;
import java.util.List;
import java.nio.ByteOrder;
import java.io.IOException;
import java.util.Iterator;

public class DataBlockAccess {
	protected static Superblock superblock = Superblock.getInstance();
	protected static BlockAccess blocks = BlockAccess.getInstance();

	private static int readBlockNumberFromBlock(int dataBlock, int index) throws IOException{
		ByteBuffer buffer = blocks.read(dataBlock);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int nr = buffer.getInt(index*4);
		return nr;
	}
	
	private static int readIndirect(int ind, int nr) throws IOException {
		int block = readBlockNumberFromBlock(ind, nr);
		return block;
	}

	private static int readDoubleIndirect(int dint, int nr) throws IOException {
		int addrPerBlock = superblock.getBlocksize()/4;
		int ind = readIndirect(dint, nr / addrPerBlock);
		int block = readIndirect(ind, nr % addrPerBlock);
		return block;
	}

	private static int readTripleIndirect(int tind, int nr) throws IOException{
		int addrPerBlock = superblock.getBlocksize()/4;
		int dind = readDoubleIndirect(tind, nr / addrPerBlock);
		int block = readIndirect(dind, nr % addrPerBlock);
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
				return (getDataBlockNr(this.inode, current + 1) != 0);
			} catch (IOException e) {
				return false;
			}
		}

		public Integer next() {
			current += 1;
			try {
				return getDataBlockNr(this.inode, current);
			} catch (IOException e) {
				return null;
			}
		}

		public void remove() {
		}
	}

	public static DataBlockIterator iterateDataBlockNr(Inode inode) {
		return new DataBlockIterator(inode);
	}

	public static DataBlockIterator iterateDataBlockNrStartingAt(Inode inode, int offset) {
		return new DataBlockIterator(inode, offset);
	}
	
	/** get the logical block number of file block */
	public static int getDataBlockNr(Inode inode, int fileBlockNumber) throws IOException {
		int[] directBlocks = inode.getBlock();
		int addrPerBlock = superblock.getBlocksize()/4;
			
		// direct
		if (fileBlockNumber < Constants.EXT2_NDIR_BLOCKS) {
			return directBlocks[fileBlockNumber];
		}

		// indirect
		fileBlockNumber -= Constants.EXT2_NDIR_BLOCKS;
		if (fileBlockNumber < addrPerBlock) {
			return readIndirect(directBlocks[Constants.EXT2_IND_BLOCK],
			                   fileBlockNumber);
		}
		
		// double indirect
		fileBlockNumber -= addrPerBlock;
		if (fileBlockNumber < addrPerBlock*addrPerBlock) {
			return readDoubleIndirect(directBlocks[Constants.EXT2_DIND_BLOCK],
			                         fileBlockNumber);
		}
		
		// triple indirect
		fileBlockNumber -= addrPerBlock*addrPerBlock;
		return readTripleIndirect(directBlocks[Constants.EXT2_TIND_BLOCK],
		                         fileBlockNumber);
	}
	
	public static ByteBuffer readDataBlock(Inode inode, int fileBlockNumber) throws IOException {
		return blocks.read(getDataBlockNr(inode, fileBlockNumber));
	}
}
