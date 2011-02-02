package jext2;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.IOException;

import jext2.exceptions.FileTooLarge;
import jext2.exceptions.NoSpaceLeftOnDevice;

public class DataBlockAccess {
	protected static Superblock superblock = Superblock.getInstance();
	protected static BlockAccess blocks = BlockAccess.getInstance();
	protected static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();
	protected DataInode inode = null;

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
		
	class DataBlockIterator implements Iterator<Long>, Iterable<Long>{
		Inode inode;
		long remaining; 
		long current;
		LinkedList<Long> blocks; /* cache for block nrs */

		DataBlockIterator(DataInode inode, long start) {
			this.inode = inode;
			this.current = start;
			this.remaining = inode.getBlocks()/(superblock.getBlocksize()/512);
		}
			
		DataBlockIterator(DataInode inode) {
		    this(inode , -1);
		}

		public boolean hasNext() {
		    fetchNext();
		    return ((remaining > 0) || ((blocks != null) && (blocks.size() > 0)));
		}
		
		private void fetchNext() {		    
			try {
			   if (remaining > 0) { /* still blocks to fetch */
			      if (blocks == null || blocks.size() == 0) { /* blockNr cache empty */
			           try {
			               blocks = getBlocks(current + 1, remaining);
			           } catch (FileTooLarge e) {
			               blocks = null;
			           }
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
	 * parse the block number into array of offsets
	 * 
	 * @param	fileBlockNr		block number to be parsed
	 * @return	array of offsets, length is the path depth 
	 * @throws FileTooLarge When the offset calculated from fileBlockNr is larger 
	 *     than the last possible triple indirection offset for this blocksize
	 */
	public static int[] blockToPath(long fileBlockNr) throws FileTooLarge {
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
		    throw new FileTooLarge();
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
				long nr = blocks.readBlockNumberFromBlock(blockNrs[i-1], 
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
	public long findNear(DataInode inode, long[] blockNrs, int[] offsets) throws IOException {
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
	 * @throws NoSpaceLeftOnDevice 
	 * 
	 * This function allocates num blocks, zeros out all but the last one, links 
	 * them into a chain and writes them to disk.
	 */
	public LinkedList<Long> allocBranch(int num, long goal, 
	                                    int[] offsets, long[] blockNrs) 
	                                           throws IOException, NoSpaceLeftOnDevice {

	    int n = 0;
	    LinkedList<Long> result = new LinkedList<Long>();

        try {
            long parent = allocateBlock(goal); 
            result.addLast(parent);

            if (parent > 0) {
                for (n=1; n < num; n++) {
                    /* allocate the next block */
                    long nr = allocateBlock(parent);
                    if (nr > 0) {
                        result.addLast(nr);
                        
                        ByteBuffer buf = ByteBuffer.allocate(superblock.getBlocksize());
                        Ext2fsDataTypes.putLE32U(buf, nr, offsets[n]*4);                        
                        blocks.write(parent, buf);	        
                    } else {	                
                        break;
                    }

                    parent = nr;
                }
            }
        } catch (NoSpaceLeftOnDevice e) {
            for (long nr : result) {
                freeBlocks(new long[] {nr});
            }
        }
	    
	    if (num == n) 
	        return result;
	            
	    /* Allocation failed, free what we already allocated */
	    throw new NoSpaceLeftOnDevice();
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
	        Ext2fsDataTypes.putLE32U(buf, newBlockNrs.getFirst().longValue()*4, 
	                                     offsets[existDepth]);
	        blocks.write(blockNrs[existDepth-1], buf);	        
	    }
	    
	    lastAllocLogicalBlock = logicalBlock;
	    lastAllocPhysicalBlock = newBlockNrs.getLast().intValue();
	    
	    inode.setBlocks(inode.getBlocks() + 
	            newBlockNrs.size() * (superblock.getBlocksize()/ 512));
	    inode.setModificationTime(new Date());
	    inode.write();
	}
	
	
	/** 
	 * Get up to maxBlocks block numbers for the logical block number. Do not allocate new blocks
	 * @see    getBlocksAllocate
	 * @param  fileBlockNr  logical block address
	 * @param  maxBlocks    maximum blocks returned
	 * @return list of block nrs or null if logical block not found
     * @throws FileTooLarge fileBlockNr bigger than maximum indirection offset 
     * @throws IOException 
	 */
	public LinkedList<Long> getBlocks(long fileBlockNr, long maxBlocks) 
	        throws IOException, FileTooLarge {
	    try {
	        return getBlocks(fileBlockNr, maxBlocks, false);
	    } catch (NoSpaceLeftOnDevice e) {
	        throw new RuntimeException("should not happen");
	    }
	}

	/** 
     * Get up to maxBlocks block numbers for the logical block number. Allocate new blocks if
     * necessary. In case of block allocation only one is blockNr returned. 
     * @see    getBlocksAllocate
     * @param  fileBlockNr  logical block address
     * @param  maxBlocks    maximum blocks returned
     * @return list of block nrs
     * @throws NoSpaceLeftOnDevice 
     * @throws FileTooLarge fileBlockNr bigger than maximum indirection offset
     * @throws IOException
     * @see #getBlocks(long fileBlockNr, long maxBlocks, boolean create) 
     * @see #getBlocks(long fileBlockNr, long maxBlocks) 
     */
    public LinkedList<Long> getBlocksAllocate(long fileBlockNr, long maxBlocks) 
            throws IOException, NoSpaceLeftOnDevice, FileTooLarge {
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
	 * @throws NoSpaceLeftOnDevice 
	 * @throws FileTooLarge 
	 * @throws IOException
	 */
	private LinkedList<Long> getBlocks(long fileBlockNr, long maxBlocks, boolean create) 
	    throws IOException, NoSpaceLeftOnDevice, FileTooLarge {
	    
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
					nextOnDisk = blocks.readBlockNumberFromBlock(
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
	 * Try to allocate a block by looping over block groups and calling 
	 * newBlockInGroup
	 */ 
    private static long newBlock(long goal) throws IOException, NoSpaceLeftOnDevice {        
        if (! superblock.hasFreeBlocks()) {
            throw new NoSpaceLeftOnDevice();
        }
        	    
        if (goal < superblock.getFirstDataBlock() ||
                goal >= superblock.getBlocksCount())
            goal = superblock.getFirstDataBlock();
    
        int goalGroup = Calculations.groupOfBlk(goal);
        int start = (int) ((goal - superblock.getFirstDataBlock()) %
                superblock.getBlocksPerGroup());

        /* start at the goalGroup and search forward for first free block */
        for (BlockGroupDescriptor descr : blockGroups.iterateBlockGroups(goalGroup)) {
            long blockNr =  newBlockInGroup(start, descr);
            if (blockNr > 0) {
                return blockNr;
            }
            start = 0;
        }

        /* start at first group and search the rest */
        for (BlockGroupDescriptor descr : blockGroups.iterateBlockGroups()) {
            long blockNr =  newBlockInGroup(0, descr);
            if (blockNr > 0) {
                return blockNr;
            }
        }
        
        throw new NoSpaceLeftOnDevice();
    }
     
    /** 
     * Allocate a new block. Uses a goal block to assist allocation. If 
     * the goal is free, or there is a free block within 32 blocks of the gloal, that block is
     * allocated. Otherwise a forward search is made for a free block.
     * @param  goal    the goal block
     * @return     pointer to allocated block
     * @throws IOException 
     * @throws NoSpaceLeftOnDevice 
     */    
    public static long allocateBlock(long goal) throws NoSpaceLeftOnDevice, IOException {
        long blockNr = newBlock(goal);
        
        /* Finally return pointer to allocated block or an error */
        superblock.setFreeBlocksCount(superblock.getFreeBlocksCount() - 1);
        superblock.write();
            
        return blockNr;
    }

    /**
     * Find free block in single block group
     * @param       start       bitmap index to begin (think of goal)
     * @param       descr       group descriptor to search
     * @return      block number or -1 
     */
    private static long newBlockInGroup(int start, BlockGroupDescriptor descr) throws IOException {
        Bitmap bitmap = Bitmap.fromByteBuffer(blocks.read(descr.getBlockBitmapPointer()),
                descr.getBlockBitmapPointer());

        if (descr.getFreeBlocksCount() > 0) {

            int freeIndex = bitmap.getNextZeroBitPos(start);
            if (freeIndex > 0) {
                long blockNr = descr.firstBlock() + freeIndex;
                
                /* Check to see if we are trying to allocate a system block */
                if (!(descr.isValidDataBlockNr(blockNr))) {
                    throw new RuntimeException("Trying to allocate in system zone" 
                                            + " blockNr=" + blockNr 
                                            + " group=" + descr.getBlockGroup()
                                            + " index=" + freeIndex);
                }  
                
                bitmap.setBit(freeIndex, true);
                bitmap.write();

                descr.setFreeBlocksCount(descr.getFreeBlocksCount() - 1);
                descr.write();            

                return Calculations.blockNrOfLocal(freeIndex, descr.getBlockGroup());
            } else {
                start = 0;
            }
        }        
        return -1;
    }
    

    private DataBlockAccess(DataInode inode) {
        this.inode = inode;
    }

	/** 
	 * Create access provider to inode data 
	 */ 
	public static DataBlockAccess fromInode(DataInode inode) {
	    DataBlockAccess access = new DataBlockAccess(inode);
	    return access;
	}
    
	/**
	 * Free single data block. Update inode.blocks.
	 * @param  blockNr physical block to free
	 */
	public void freeDataBlock(long blockNr) throws IOException {
	    freeDataBlocksContiguous(blockNr, 1);
	    
	}
	
	/**
	 * Free count blocks. Update inode.blocks.
	 * @param  blockNr   start physical block to free
	 * @param  count   number of blocks to free
	 * @throws IOException 
	 */
	public void freeDataBlocksContiguous(long blockNr, long count) throws IOException {
	    /* counter to set {superblock|blockgroup}.freeBlocksCount */
	    int groupFreed = 0; 
	    int freed = 0;
	                        
	    if (blockNr < superblock.getFirstDataBlock() ||
	        blockNr + count < blockNr ||
	        blockNr + count > superblock.getBlocksCount()) {
	        throw new RuntimeException("Free blocks not in datazone" +
	        		" blockNr=" + blockNr +
	        		" count=" + count);
	    }
	    
	    long overflow; 
	    do {
	        overflow = 0;
	        int groupNr = Calculations.groupOfBlk(blockNr);
	        int groupIndex = Calculations.groupIndexOfBlk(blockNr);
	        BlockGroupDescriptor groupDescr = blockGroups.getGroupDescriptor(groupNr);
	   
	        /* Check to see if we are freeing blocks across a group boundary. */
	        if (groupIndex + count > superblock.getBlocksPerGroup()) {
	            overflow = groupIndex + count - superblock.getBlocksPerGroup();
	            count -= overflow;
	        }
	   
	        Bitmap bitmap = Bitmap.fromByteBuffer(
	                blocks.read(groupDescr.getBlockBitmapPointer()),
	                groupDescr.getBlockBitmapPointer());
	   
	        /* Check to see if we are trying to free a system block */
	        if (!(groupDescr.isValidDataBlockNr(blockNr) &&
	              groupDescr.isValidDataBlockNr(blockNr + count-1))) {
	            throw new RuntimeException("Freeing blocks in system zones");
	        }  
	    
	        /* Set block bits to "free" */
	        groupFreed = 0;
	        for (int i=0; i<count; i++) {
	            if (!(bitmap.isSet(groupIndex + i))) {
	                throw new RuntimeException("Bit allready cleared for block" +
	                        " nr=" + (blockNr+i) +
	                        " groupIndex=" + (groupIndex+i) +
	                        " bitmap=" + bitmap.getBitStringContaining(groupIndex + i));
	            } else if (groupIndex + i > superblock.getBlocksPerGroup()) {
	                groupFreed++;
	            } else {
	                groupFreed++;
	                bitmap.setBit(groupIndex + i, false);
	            }
	        }
	        bitmap.write();
	    
	        groupDescr.setFreeBlocksCount(groupDescr.getFreeBlocksCount() + groupFreed);
	        groupDescr.write();
	        freed += groupFreed;
	        
	        blockNr += count;
	        count = overflow;
	    } while (overflow > 0);
	    
	    
	    inode.setBlocks(inode.getBlocks() - freed * (superblock.getBlocksize()/ 512));
	    inode.setModificationTime(new Date());
	    superblock.setFreeBlocksCount(superblock.getFreeBlocksCount() + freed);
	    superblock.write();
	}
	
	/**
	 * Free array of data blocks and update inode.blocks appropriately. 
	 * @param  blockNrs       array of block numbers
	 */
	public void freeBlocks(long[] blockNrs) throws IOException {
	    long blockToFree = 0;
	    long count = 0;
	    
	    for (long nr : blockNrs) {
	        if (nr > 0) {
	            if (count == 0) {
	                blockToFree = nr;
	                count = 1;
	            } else if (blockToFree == nr - count) {
	                count++;
	            } else {
	                freeDataBlocksContiguous(blockToFree, count);
	                blockToFree = nr;
	                count = 1;
	            }
	        }
	    }

	    if (count > 0) {
	        freeDataBlocksContiguous(blockToFree, count);
	    }
	}
	
	
	
	/**
	 * Free branches starting at the blocks in blockNrs.
	 * @param  depth   depth of the free recursion
	 * @param  blockNrs    blockNrs to start
	 * 
	 */
	private void freeBranches(int depth, long[] blockNrs) throws IOException {	    
	    if (depth > 0) { /* indirection exists -> go down */
	        depth -= 1;
	        for (long nr : blockNrs) {
	            if (nr == 0) continue;
	            
	            long[] nextBlockNrs = blocks.readAllBlockNumbersFromBlock(nr);
	            
	            freeBranches(depth, nextBlockNrs);
	            freeDataBlock(nr);
	        }
	    } else { /* just data pointers left */
	        freeBlocks(blockNrs);
	    }
	}
	

	/**
	 * Truncate data blocks toSize. 
	 * @throws IOException 
	 * @throws FileTooLarge When you try to truncate to a size 
	 *     beyond the max. blocks count
	 */
	void truncate(long toSize) throws IOException, FileTooLarge {
	    if (inode instanceof SymlinkInode && 
	            ((SymlinkInode)inode).isFastSymlink())
	        return;
	    
	    // TODO check inode flags for append or immutable 

        long[] directBlocks = inode.getBlock();
	    
	    int blocksize = superblock.getBlocksize();
	    long blockToKill = (toSize + blocksize-1) / blocksize;

	    int[] offsets = blockToPath(blockToKill);
	    int depth = offsets.length;;

        /* kill direct blocks */
	    if (depth == 1) { 
	        long[] blocksToFree = Arrays.copyOfRange(directBlocks, 
	                offsets[0], Constants.EXT2_NDIR_BLOCKS); 
	        
	        for (int i=offsets[0]; i<Constants.EXT2_NDIR_BLOCKS; i++) {
	            directBlocks[i] = 0;
	        }
	        freeBlocks(blocksToFree);
	    }
	    
	    /* kill partial branches */
	    long[] branchNrs = getBranch(offsets);
	    int existDepth = branchNrs.length;
	    
	    for (int i=existDepth-1; i>0; i--) {
	        long nr = branchNrs[i-1];
	        int start = offsets[i];
	        
	        long[] blockNrs = blocks.readBlockNrsFromBlock(nr, start, ptrs-1);
	        blocks.zeroOut(nr, start*4, (ptrs-1)*4);
	        freeBranches((existDepth-2)-i, blockNrs);
	    }
	    directBlocks[offsets[0]] = 0;
	        
	    /* kill the remaining (whole) subtrees */
	    long nr = -1;
	    
	    switch(offsets[0]) {
	    default:
	        nr = directBlocks[Constants.EXT2_IND_BLOCK];
	        if (nr > 0) {
	            directBlocks[Constants.EXT2_IND_BLOCK] = 0;
	            freeBranches(1, new long[] {nr});
	        }
	    case Constants.EXT2_IND_BLOCK:
            nr = directBlocks[Constants.EXT2_DIND_BLOCK];
            if (nr > 0) {
                directBlocks[Constants.EXT2_DIND_BLOCK] = 0;
                freeBranches(2, new long[] {nr});
            }	        
	    case Constants.EXT2_DIND_BLOCK:
            nr = directBlocks[Constants.EXT2_TIND_BLOCK];
            if (nr > 0) {
                directBlocks[Constants.EXT2_TIND_BLOCK] = 0;
                freeBranches(3, new long[] {nr});
            }
	    case Constants.EXT2_TIND_BLOCK:
	        ;
	    }
	    
	    inode.write();
	}
}









