package jext2;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.nio.ByteOrder;
import java.io.IOException;
import java.util.Iterator;

public class DataBlockAccess {
	protected static Superblock superblock = Superblock.getInstance();
	protected static BlockAccess blocks = BlockAccess.getInstance();
	protected static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	
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
				} 
		}
				
		return blockNrs;
	}
	
	/** Find a preferred place for allocation
	 * @param  inode   owner
	 * @param  block   block we want
	 * @return Preferrred place for a block (the goal)
	 */
	public static long findGoal(Inode inode, long block, long[] blockNrs, long[] offsets) throws IOException {
	    if (block == (inode.getLastAllocLogicalBlock() + 1) 
	        && (inode.getLastAllocPhysicalBlock() != 0)) {
	            return (inode.getLastAllocPhysicalBlock() + 1);
	        }
	    return findNear(inode, blockNrs, offsets);
	}

	// XXX put somewhere sane. 
	private static long getPID() {
	    String appName = ManagementFactory.getRuntimeMXBean().getName();
	    long pid = Long.parseLong(appName);
	    return pid;	    
	}
	    
	    
	
	/** Find a place for allocation with sufficient locality
	 * @param  inode   owner
	 * @return Preferred place for block allocation. It is used when the heuristic for sequential allocation fails.
	 * 
	 * Rules are:
	 *     - if there is a block to the left of our position - allocate near it
	 *     - if pointer will live in indirect block - allocate near that block
	 *     - if pointer will live in inode - allocate in the same cylinder group
	 */
	public static long findNear(Inode inode, long[] blockNrs, long[] offsets) throws IOException {
	    int depth = blockNrs.length;
	    
	    /* Try to find previous block */
	    if (depth == 1)  { /* search direct blocks */
	        int[] directBlocks = inode.getBlock();
	        for (int i=(int)(blockNrs[0] - 1); i >= 0; i--) {
	            if (directBlocks[i] != 0) 
	                return directBlocks[i];
	        }
	    } else { /* search last indirect block */
	        ByteBuffer indirectBlock = blocks.read(blockNrs[depth-1]);
	        indirectBlock.position((int)(offsets[depth-1]*4));
	        
	        for (int i=(int)(blockNrs[depth-1] -1); i>= 0; i--) {
	            int pointer = Ext2fsDataTypes.getLE32(indirectBlock, i*4);
	            if (pointer != 0) 
	                return pointer;
	        }
	    }

	    /* No such thing, so let's try location of indirect block */
	    if (depth > 1) 
	        return blockNrs[depth-1];
	    
	    
	    /* It is going to be refered from inode itself? OK just put i into
	     * the same cylinder group then
	     */
	    int bgStart = BlockGroupDescriptor.firstBlock(inode.getBlockGroup());
	    long colour = (getPID() % 16) * (superblock.getBlocksPerGroup() / 16);
	    return bgStart + colour;
	    
	}
	 
	/** Allocate and set up a chain of blocks. 
	 * @param  inode   owner
	 * @param  num     depth of the chain (number of blocks to allocate)
	 * @param  goal    gloal block
	 * @param  offsets offsets in the indirection chain
	 * @param  blockNrs    chain of allready allocated blocks
	 * 
	 * This function allocates num blocks, zeros out all but the last one, links 
	 * them into a chain and writes them to disk.
	 */
	public static LinkedList<Long> allocBranch(Inode inode, int num, long goal, 
	                                           long[] offsets, long[] blockNrs) 
	                                           throws IOException {

	    int n = 0;
	    
	    LinkedList<Long> result = new LinkedList<Long>();
        ByteBuffer buf = ByteBuffer.allocate(superblock.getBlocksize());

	    
	    long parent = newBlock(goal);
	    
	    if (parent > 0) {
	        for (n=1; n < num; n++) {
	            /* allocate the next block */
	            long nr = newBlock(parent);
	            if (nr > 0) {
	                result.addLast(nr);
	                buf.clear();
	                Ext2fsDataTypes.putLE32(buf, (int)nr, (int)offsets[n]);
	                blocks.write((int)parent, buf);	        
	            } else {	                
	                break;
	            }
	            
	            parent = nr;
	        }
	    }
	    
	    if (num == n) 
	        return result;
	            
	    /* Allocation failed, free what we already allocated */
	    // TODO implement free_blocks
	    return null;
	}
	
	
	/**
	 * Splice the allocated branch onto inode
	 * @throws IOException 
	 */
	public static void spliceBranch(Inode inode, long logicalBlock, 
	                                long[] offsets, long[] blockNrs, LinkedList<Long> newBlockNrs) 
	                                throws IOException {
	    
	    int existDepth = blockNrs.length;
	    
	    if (existDepth == 0) { /* add direct block */
	        int[] directBlocks = inode.getBlock();
	        directBlocks[(int)offsets[0]] = newBlockNrs.getFirst().intValue();
	    } else {
	        ByteBuffer buf = blocks.read(blockNrs[existDepth-1]);
	        Ext2fsDataTypes.putLE32(buf, newBlockNrs.getFirst().intValue(), (int)offsets[existDepth]);
	        blocks.write((int)blockNrs[existDepth-1], buf);	        
	    }
	    
	    inode.setLastAllocLogicalBlock(logicalBlock);
	    inode.setLastAllocPhysicalBlock(newBlockNrs.getLast().intValue());
	    
	    inode.setChangeTime(new Date());
	    inode.write();
	}
	
	
	
	/** 
	 * Allocate a new block. Uses a goal block to assist allocation. If 
	 * the goal is free, or there is a free block within 32 blocks of the gloal, that block is
	 * allocated. Otherwise a forward search is made for a free block.
	 * @param  goal    the goal block
	 * @return     pointer to allocated block
	 */
	public static long newBlock(long goal) throws IOException {
	    
	    if (! superblock.hasFreeBlocks()) {
	        return -1;
	    }
	    	    
	    if (goal < superblock.getFirstDataBlock() ||
	            goal >= superblock.getBlocksCount())
	        goal = superblock.getFirstDataBlock();

	    long groupNr = Calculations.groupOfBlk((int)goal);
	    BlockGroupDescriptor groupDescr = blockGroups.getGroupDescriptor((int)groupNr);
	    Bitmap bitmap = Bitmap.fromByteBuffer(blocks.read(groupDescr.getBlockBitmap()),
	                                          groupDescr.getBlockBitmap());
	    
	    long allocatedBlock = -1;
	    if (groupDescr.getFreeBlocksCount() > 0) {
	        int localGoal = (int) ((goal - superblock.getFirstDataBlock()) %
	                                    superblock.getBlocksPerGroup());
	        
	        if (!bitmap.isSet(localGoal)) { /* got goal block */
	            bitmap.setBit(localGoal, true);
                allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
	        }
	        
	        /* the goal was occupied; search forward for a free block 
	         * within the next XX blocks. */
	        int end = (localGoal + 63) & - 63;
	        localGoal = bitmap.getNextZeroBitPos(localGoal, end);
	        if (localGoal < end) {
	            bitmap.setBit(localGoal, true);
                allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
	        }
	        
	        
	        /* There has been no free block found in the near vicinity of the goal:
	         * do a search forward through the block groups */
	        
	        /* Search first in the remainder of th current group */
	        localGoal = bitmap.getNextZeroBitPos(localGoal);
	        if (localGoal > 0) {
	            bitmap.setBit(localGoal, true);
	            allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
	        }
	    }

	    if (allocatedBlock == -1) {
	        /* Now search the rest of the groups */
	        for (int k=0; k < superblock.getGroupsCount(); k++) {
	            groupNr = (groupNr + 1) % superblock.getGroupsCount();
	            groupDescr = blockGroups.getGroupDescriptor((int)groupNr);

	            if (groupDescr.getFreeBlocksCount() > 0) 
	                break;
	        }

	        bitmap = Bitmap.fromByteBuffer(blocks.read(groupDescr.getBlockBitmap()),
	                groupDescr.getBlockBitmap());

	        int localGoal = bitmap.getNextZeroBitPos(0);
	        if (localGoal > 0) {
	            bitmap.setBit(localGoal, true);
	            allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
	        }
	    }

	    /* Finally return pointer to allocated block or an error */
	    if (allocatedBlock > 0) { /* block was allocated in group */
            // GOT IT !!!
            groupDescr.setFreeBlocksCount((short)(groupDescr.getFreeBlocksCount() - 1));
            superblock.setFreeBlocksCount(superblock.getFreeBlocksCount() - 1);

            groupDescr.write();
            superblock.write();
            
            return allocatedBlock;
	    } else {
	        return -1;
	    }
	}
	


	/** Get up to maxBlocks BlockNrs for the logical fileBlockNr. The Blocks returned are sequential.
	 * @param inode    Inode of the data block
	 * @param fileBlockNr  the logical block address
	 * @param maxBlocks    maximum blocks returned
	 * @param create       true: create blocks if nesseccary; false: just read
	 * @return             list of block nrs; if create=false null is returned if block does not exist
	 */
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
				else /* direct blocks */
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

		/* Okay, we need to do block allocation. */
		long goal = findGoal(inode, fileBlockNr, blockNrs, offsets);
		int count = depth - existDepth;

		LinkedList<Long> newBlockNrs = allocBranch(inode, count, goal, offsets, blockNrs);
		spliceBranch(inode, fileBlockNr, offsets, blockNrs, newBlockNrs);
		
		result.add(newBlockNrs.getLast());
		return result;
	}
	
	
}









