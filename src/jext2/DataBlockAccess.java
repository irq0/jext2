package jext2;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.nio.ByteOrder;
import java.io.IOException;
import java.util.Iterator;

public class DataBlockAccess {
	protected static Superblock superblock = Superblock.getInstance();
	protected static BlockAccess blocks = BlockAccess.getInstance();

	/** number of pointers in indirection block */
	private static int ptrs = superblock.getAddressesPerBlock();
	/** number of bits a pointer in an indirection block takes */
    private static int ptrs_bits = superblock.getAddressesPerBlockBits();
    
    /** number of direct Blocks */
    public static final long directBlocks = Constants.EXT2_NDIR_BLOCKS;
    /** number of indirect Blocks */
    public static final long indirectBlocks = ptrs;
    /** number of double indirect blocks */
    public static final long doubleBlocks = ptrs*ptrs;
    /** number of tripple indirect blocks */
    public static final long trippleBlocks = ptrs*ptrs*ptrs;
	
	
	
	
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

	static class DataBlockIterator implements Iterator<Integer>, Iterable<Integer>{
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
		
		public DataBlockIterator iterator() {
			return this;
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
	
	/** 
	 * parse the block number into array of offsets
	 * 
	 * @param	fileBlockNr		block number to be parsed
	 * @return	array of offsets, length is the path depth 
	 */
	public static long[] blockToPath(long fileBlockNr) {
		if (fileBlockNr < 0) {
			throw new RuntimeException("blockToPath: file block number < 0");
		} else if (fileBlockNr < Constants.EXT2_NDIR_BLOCKS) {
			return new long[] { fileBlockNr };			
		} else if ((fileBlockNr -= directBlocks) < indirectBlocks) {
			return new long[] { Constants.EXT2_IND_BLOCK,
							    fileBlockNr };
		} else if ((fileBlockNr -= indirectBlocks) < doubleBlocks) {
			return new long[] { Constants.EXT2_DIND_BLOCK,
							    fileBlockNr / ptrs,
							    fileBlockNr % ptrs };
		} else if ((fileBlockNr -= doubleBlocks)  < trippleBlocks) {
			return new long[] { Constants.EXT2_TIND_BLOCK, 
							    fileBlockNr / (ptrs*ptrs),
							    (fileBlockNr / ptrs) % ptrs ,
							    fileBlockNr % ptrs };
		} else {
			throw new RuntimeException("blockToPath: block is to big");
		}
	}

	/**
	 * Read the chain of indirect blocks leading to data
	 * @param inode 	Inode in question
	 * @param offsets	offsets of pointers in inode/indirect blocks
	 * @return	array of length depth with block numbers on the path. 
	 * 	  array[depth-1] is data block number rest is the indirection on the path.
	 * 
	 * If the chain is incomplete return.length < offsets.length
	 */
	public static long[] getBranch(Inode inode, long[] offsets) throws IOException {
		int depth = offsets.length;
		long[] blockNrs = new long[depth];
		
		
		blockNrs[0] = inode.getBlock()[(int)offsets[0]];
		
		if (blockNrs[0] == 0) {
			return new long[] {blockNrs[0]};
		} 
				
		for (int i=1; i<depth; i++) {
				long nr = readBlockNumberFromBlock((int)blockNrs[i-1], 
					    					       (int)offsets[i]);

				blockNrs[i] = nr;
				if (nr == 0) { /* chain is incomplete */
					long[] result = new long[i];
					for (int k=0; k<i; k++)
						result[k] = blockNrs[k];
					return result;
				} else { /* add to chain */
				}

		}
				
		return blockNrs;
	}
	
	
	public static LinkedList<Long> getBlocks(Inode inode, long fileBlockNr, int maxBlocks, boolean create) throws IOException {
		if (fileBlockNr < 0 || maxBlocks < 1) 
			throw new IllegalArgumentException();
		
		
		LinkedList<Long> result = new LinkedList<Long>();
		long[] offsets;
		long[] blockNrs;
		int depth;
		int existDepth;
				
		offsets = blockToPath(fileBlockNr);
		depth = offsets.length;
		
		blockNrs = getBranch(inode, offsets);
		existDepth = blockNrs.length;
		
		/* Simplest case - block found, no allocation needed */
		if (depth == existDepth) {			
			long firstBlockNr = blockNrs[depth-1];			
			result.addFirst(firstBlockNr);
			
			int blocksToBoundary = 0;
			if (depth >= 2) /* indirect blocks */
				blocksToBoundary = 
					(int)(superblock.getAddressesPerBlock() - offsets[depth-1] - 1); 
			else /* direct blocks */
				blocksToBoundary = 
					(int)(Constants.EXT2_NDIR_BLOCKS - offsets[0] - 1);
			
			int count = 1;
			while(count < maxBlocks && count <= blocksToBoundary) {
								
				long nextByNumber = firstBlockNr + count;
				long nextOnDisk = -1;
				if (depth >= 2) /* indirect blocks */
					nextOnDisk = readBlockNumberFromBlock(
						(int)blockNrs[depth-2], (int)offsets[depth-1] + count);
				else 
					nextOnDisk = inode.getBlock()[(int)(offsets[0] + count)];
				
						
						
				/* check if next neighbor block belongs to inode */
				if (nextByNumber == nextOnDisk) {
					result.addLast(nextByNumber);
					count++;
				} else {
					return result;
				}
			}
			return result;
		}
		
		/* Next simple case - plain lookup */
		if (!create) {
			return null;
		}

		
		// TODO block allocation
		return null;
	}
	
	
}
