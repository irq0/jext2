package jext2;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.nio.ByteOrder;
import java.io.IOException;
import java.util.Iterator;

public class DataBlockAccess {
	protected static Superblock superblock = Superblock.getInstance();
	protected static BlockAccess blocks = BlockAccess.getInstance();
	protected static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	protected Inode inode = null;

	// used by findGoal 
    private long lastAllocLogicalBlock = 0; 
    private long lastAllocPhysicalBlock = 0;
	
	/** number of pointers in indirection block */
	private static int ptrs = superblock.getAddressesPerBlock();
    
    /** number of direct Blocks */
    public static final long directBlocks = Constants.EXT2_NDIR_BLOCKS;
    /** number of indirect Blocks */
    public static final long indirectBlocks = ptrs;
    /** number of double indirect blocks */
    public static final long doubleBlocks = ptrs*ptrs;
    /** number of triple indirect blocks */
    public static final long trippleBlocks = ptrs*ptrs*ptrs;
		
	public static long readBlockNumberFromBlock(long dataBlock, int index) throws IOException{
		ByteBuffer buffer = blocks.read(dataBlock);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long nr = Ext2fsDataTypes.getLE32U(buffer, index*4);
		return nr;
	}
	
	public static long readIndirect(long ind, int nr) throws IOException {
		long block = readBlockNumberFromBlock(ind, nr);
		return block;
	}

	public static long readDoubleIndirect(long dint, int nr) throws IOException {
		long addrPerBlock = superblock.getBlocksize()/4;
		long ind = readIndirect(dint, (int)(nr / addrPerBlock));
		long block = readIndirect(ind, (int)(nr % addrPerBlock));
		return block;
	}

	public static long  readTripleIndirect(long tind, int nr) throws IOException{
		long addrPerBlock = superblock.getBlocksize()/4;
		long dind = readDoubleIndirect(tind, (int)(nr / addrPerBlock));
		long block = readIndirect(dind, (int)(nr % addrPerBlock));
		return block;
	}

	class DataBlockIterator implements Iterator<Long>, Iterable<Long>{
		Inode inode;
		long remaining; 
		long current;
		LinkedList<Long> blocks; /* cache for block nrs */

		DataBlockIterator(Inode inode, long start) {
			this.inode = inode;
			this.current = start;
			this.remaining = inode.getBlocks();
		}
			
		DataBlockIterator(Inode inode) {
		    this(inode , -1);
		}

		public boolean hasNext() {
		    fetchNext();
		    return (remaining > 0);
		}
		
		private void fetchNext() {		    
			try {
			   if (remaining > 0) { /* still blocks to fetch */
			      if (blocks == null || blocks.size() == 0) { /* blockNr cache empty */
			           blocks = getBlocks(current + 1, remaining);
			           if (blocks == null) {
			               remaining = 0;
			               return;
			           }
			           remaining -= blocks.size();
			           current += blocks.size();
			       }
			   }
			} catch (IOException e) {
			}
		}

		public Long next() {
		    fetchNext();
		    return (blocks.removeFirst());
		}

		public void remove() {
		}
		
		public DataBlockIterator iterator() {
			return this;
		}
	}
	
	public DataBlockIterator iterateBlocks() {
		return new DataBlockIterator(inode);
	}

	public DataBlockIterator iterateBlocksStartAt(long start) {
		return new DataBlockIterator(inode, start);
	}
	
	/** 
	 * get the logical block number of file block *
	 * This is actually more or less the same as getBlocks except 
	 * that only one blockNr is returned. I wrote this first and kind of 
	 * like it ;) 
	 */
	public static long getDataBlockNr(Inode inode, long fileBlockNumber) throws IOException {
		long[] directBlocks = inode.getBlock();
		int addrPerBlock = superblock.getBlocksize()/4;
			
		// direct
		if (fileBlockNumber < Constants.EXT2_NDIR_BLOCKS) {
			return directBlocks[(int)fileBlockNumber];
		}

		// indirect
		fileBlockNumber -= Constants.EXT2_NDIR_BLOCKS;
		if (fileBlockNumber < addrPerBlock) {
			return readIndirect(directBlocks[Constants.EXT2_IND_BLOCK],
			                   (int)fileBlockNumber);
		}
		
		// double indirect
		fileBlockNumber -= addrPerBlock;
		if (fileBlockNumber < addrPerBlock*addrPerBlock) {
			return readDoubleIndirect(directBlocks[Constants.EXT2_DIND_BLOCK],
			                         (int)fileBlockNumber);
		}
		
		// triple indirect
		fileBlockNumber -= addrPerBlock*addrPerBlock;
		return readTripleIndirect(directBlocks[Constants.EXT2_TIND_BLOCK],
		                         (int)fileBlockNumber);
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
	public static int[] blockToPath(long fileBlockNr) {
	    // fileBlockNr will allways be less than blockSize -> int is ok
		if (fileBlockNr < 0) {
			throw new RuntimeException("blockToPath: file block number < 0");
		} else if (fileBlockNr < Constants.EXT2_NDIR_BLOCKS) {
			return new int[] { (int)fileBlockNr };			
		} else if ((fileBlockNr -= directBlocks) < indirectBlocks) {
			return new int[] { Constants.EXT2_IND_BLOCK,
							    (int)fileBlockNr };
		} else if ((fileBlockNr -= indirectBlocks) < doubleBlocks) {
			return new int[] { Constants.EXT2_DIND_BLOCK,
							    (int)fileBlockNr / ptrs,
							    (int)fileBlockNr % ptrs };
		} else if ((fileBlockNr -= doubleBlocks)  < trippleBlocks) {
			return new int[] { Constants.EXT2_TIND_BLOCK, 
							    (int)fileBlockNr / (ptrs*ptrs),
							    ((int)fileBlockNr / ptrs) % ptrs ,
							    (int)fileBlockNr % ptrs };
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
	public long[] getBranch(int[] offsets) throws IOException {
		int depth = offsets.length;
		long[] blockNrs = new long[depth];
		
		blockNrs[0] = inode.getBlock()[offsets[0]];
		
		if (blockNrs[0] == 0) {
			return new long[] {};
		} 
				
		for (int i=1; i<depth; i++) {
				long nr = readBlockNumberFromBlock(blockNrs[i-1], 
					    					       offsets[i]);

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
	public long findGoal(long block, long[] blockNrs, int[] offsets) throws IOException {
	    if (block == (lastAllocLogicalBlock + 1) 
	        && (lastAllocPhysicalBlock != 0)) {
	            return (lastAllocPhysicalBlock + 1);
	        }
	    return findNear(inode, blockNrs, offsets);
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
	public static long findNear(Inode inode, long[] blockNrs, int[] offsets) throws IOException {
	    int depth = blockNrs.length;
	    
	    /* Try to find previous block */
	    if (depth == 0)  { /* search direct blocks */
	        long[] directBlocks = inode.getBlock();
	        for (int i=directBlocks.length-1; i >= 0; i--) {
	            if (directBlocks[i] != 0) 
	                return directBlocks[i];
	        }
	    } else { /* search last indirect block */
	        ByteBuffer indirectBlock = blocks.read(blockNrs[depth-1]);
	        
	        for (int i=offsets[depth-1]-1; i>= 0; i--) {
	            long pointer = Ext2fsDataTypes.getLE32U(indirectBlock, i*4);
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
	    long bgStart = BlockGroupDescriptor.firstBlock(inode.getBlockGroup());
	    long colour = (Filesystem.getPID() % 16) * 
	                    (superblock.getBlocksPerGroup() / 16);
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
	public LinkedList<Long> allocBranch(int num, long goal, 
	                                    int[] offsets, long[] blockNrs) 
	                                           throws IOException {

	    int n = 0;
	    
	    LinkedList<Long> result = new LinkedList<Long>();
        ByteBuffer buf = ByteBuffer.allocate(superblock.getBlocksize());
	    
	    long parent = newBlock(goal);
	    result.addLast(parent);
	    
	    if (parent > 0) {
	        for (n=1; n < num; n++) {
	            /* allocate the next block */
	            long nr = newBlock(parent);
	            if (nr > 0) {
	                result.addLast(nr);
	                buf.clear();
	                Ext2fsDataTypes.putLE32U(buf, nr, offsets[n]);
	                blocks.write(parent, buf);	        
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
	public void spliceBranch(long logicalBlock, 
	                         int[] offsets, long[] blockNrs, LinkedList<Long> newBlockNrs) 
	                         throws IOException {
	    
	    int existDepth = blockNrs.length;
	    
	    if (existDepth == 0) { /* add direct block */
	        long[] directBlocks = inode.getBlock();
	        directBlocks[offsets[0]] = newBlockNrs.getFirst().longValue();
	    } else {
	        ByteBuffer buf = blocks.read(blockNrs[existDepth-1]);
	        Ext2fsDataTypes.putLE32(buf, newBlockNrs.getFirst().intValue(), 
	                                     offsets[existDepth]);
	        blocks.write(blockNrs[existDepth-1], buf);	        
	    }
	    
	    lastAllocLogicalBlock = logicalBlock;
	    lastAllocPhysicalBlock = newBlockNrs.getLast().intValue();
	    
	    inode.setBlocks(inode.getBlocks() + newBlockNrs.size());
	    inode.setChangeTime(new Date());
	    inode.write();
	}
	
	
	/** 
	 * Get up to maxBlocks block numbers for the logical block number. Do not allocate new blocks
	 * @see    getBlocksAllocate
	 * @param  fileBlockNr  logical block address
	 * @param  maxBlocks    maximum blocks returned
	 * @return list of block nrs or null if logical block not found
	 */
	public LinkedList<Long> getBlocks(long fileBlockNr, long maxBlocks) throws IOException {
	    return getBlocks(fileBlockNr, maxBlocks, false);
	}

	/** 
     * Get up to maxBlocks block numbers for the logical block number. Allocate new blocks if
     * necessary. In case of block allocation only one blockNr returns. 
     * @see    getBlocksAllocate
     * @param  fileBlockNr  logical block address
     * @param  maxBlocks    maximum blocks returned
     * @return list of block nrs
     */
    public LinkedList<Long> getBlocksAllocate(long fileBlockNr, long maxBlocks) throws IOException {
        return getBlocks(fileBlockNr, maxBlocks, true);
    }	


	/** Get up to maxBlocks BlockNrs for the logical fileBlockNr. I dont really like to change behavior
	 * by specifing a flag variable but this is more or less like the linux implementation. Use getBlocks
	 * or getBlocksAllocate.
	 * @param inode    Inode of the data block
	 * @param fileBlockNr  the logical block address
	 * @param maxBlocks    maximum blocks returned
	 * @param create       true: create blocks if nesseccary; false: just read
	 * @return             list of block nrs; if create=false null is returned if block does not exist
	 */
	private LinkedList<Long> getBlocks(long fileBlockNr, long maxBlocks, boolean create) throws IOException {
		if (fileBlockNr < 0 || maxBlocks < 1) 
			throw new IllegalArgumentException();
		
		
		LinkedList<Long> result = new LinkedList<Long>();
		int[] offsets;
		long[] blockNrs;
		int depth;
		int existDepth;
				
		offsets = blockToPath(fileBlockNr);
		depth = offsets.length;
		
		blockNrs = getBranch(offsets);
		existDepth = blockNrs.length;
		
		/* Simplest case - block found, no allocation needed */
		if (depth == existDepth) {			
			long firstBlockNr = blockNrs[depth-1];			
			result.addFirst(firstBlockNr);
			
			long blocksToBoundary = 0;
			if (depth >= 2) /* indirect blocks */
				blocksToBoundary = 
					superblock.getAddressesPerBlock() - offsets[depth-1] - 1; 
			else /* direct blocks */
				blocksToBoundary = 
					Constants.EXT2_NDIR_BLOCKS - offsets[0] - 1;
			
			int count = 1;
			while(count < maxBlocks && count <= blocksToBoundary) {
								
				long nextByNumber = firstBlockNr + count;
				long nextOnDisk = -1;
				if (depth >= 2) /* indirect blocks */
					nextOnDisk = readBlockNumberFromBlock(
						blockNrs[depth-2], offsets[depth-1] + count);
				else /* direct blocks */
					nextOnDisk = inode.getBlock()[offsets[0] + count];
				
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
		long goal = findGoal(fileBlockNr, blockNrs, offsets);
		int count = depth - existDepth;

		LinkedList<Long> newBlockNrs = allocBranch(count, goal, offsets, blockNrs);
		spliceBranch(fileBlockNr, offsets, blockNrs, newBlockNrs);
		
		result.add(newBlockNrs.getLast());
		return result;
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
        Bitmap bitmap = Bitmap.fromByteBuffer(blocks.read(groupDescr.getBlockBitmapPointer()),
                                              groupDescr.getBlockBitmapPointer());
        
        long allocatedBlock = -1;
        do { /* uargh... */
        if (groupDescr.getFreeBlocksCount() > 0) {
            int localGoal = (int) ((goal - superblock.getFirstDataBlock()) %
                            superblock.getBlocksPerGroup());
            
            if (!bitmap.isSet(localGoal)) { /* got goal block */
                bitmap.setBit(localGoal, true);
                bitmap.write();
                allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
                break;
            }
            
            /* the goal was occupied; search forward for a free block 
             * within the next XX blocks. */
            int end = 64/8 + 1;
            localGoal = bitmap.getNextZeroBitPos(localGoal, end);
            if (localGoal < end) {
                bitmap.setBit(localGoal, true);
                bitmap.write();
                allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
                break;
            }
            
            
            /* There has been no free block found in the near vicinity of the goal:
             * do a search forward through the block groups */
            
            /* Search first in the remainder of th current group */
            localGoal = bitmap.getNextZeroBitPos(localGoal);
            if (localGoal > 0) {
                bitmap.setBit(localGoal, true);
                bitmap.write();
                allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
                break;
            }
        }
        } while (false);
    
        if (allocatedBlock == -1) {
            /* Now search the rest of the groups */
            for (int k=0; k < superblock.getGroupsCount(); k++) {
                groupNr = (groupNr + 1) % superblock.getGroupsCount();
                groupDescr = blockGroups.getGroupDescriptor((int)groupNr);
    
                if (groupDescr.getFreeBlocksCount() > 0) 
                    break;
            }
    
            bitmap = Bitmap.fromByteBuffer(blocks.read(groupDescr.getBlockBitmapPointer()),
                    groupDescr.getBlockBitmapPointer());
    
            int localGoal = bitmap.getNextZeroBitPos(0);
            if (localGoal > 0) {
                bitmap.setBit(localGoal, true);
                bitmap.write();
                allocatedBlock = Calculations.blockNrOfLocal(localGoal, groupNr);
            }
        }
    
        /* Finally return pointer to allocated block or an error */
        if (allocatedBlock > 0) { /* block was allocated in group */
            groupDescr.setFreeBlocksCount(groupDescr.getFreeBlocksCount() - 1);
            superblock.setFreeBlocksCount(superblock.getFreeBlocksCount() - 1);
    
            groupDescr.write();
            superblock.write();
            
            return allocatedBlock;
        } else {
            return -1;
        }
    }

    private DataBlockAccess(Inode inode) {
	    this.inode = inode;
	}
	
	/** 
	 * Create access provider to inode data 
	 */ 
	public static DataBlockAccess fromInode(Inode inode) {
	    DataBlockAccess access = new DataBlockAccess(inode);
	    return access;
	}
    


	
}









