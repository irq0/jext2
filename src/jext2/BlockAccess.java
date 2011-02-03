package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import jext2.exceptions.IoError;

/**
 * Access to the filesystem blocks  
 */
public class BlockAccess {
	private int blocksize = Constants.EXT2_MIN_BLOCK_SIZE;
	private FileChannel blockdev;
	private static BlockAccess instance;
	
	/** number of pointers in indirection block */
    private int ptrs;
    
	public BlockAccess(FileChannel blockdev) {
		if (BlockAccess.instance != null) {
			throw new RuntimeException("BlockAccess is singleton!");
		} 
		this.blockdev = blockdev;
		BlockAccess.instance = this;
	}

	/** Read a block off size specified by setBlocksize() at logical address nr */
	public ByteBuffer read(long nr) throws IoError {
		ByteBuffer buf = ByteBuffer.allocate(blocksize);
		try {
		    buf.order(ByteOrder.BIG_ENDIAN);
		    blockdev.read(buf,(((long)(nr & 0xffffffff)) * blocksize));
		} catch (IOException e) {
		    throw new IoError();
		}

		return buf; 
	}	
	
	/**
	 * Read data from device to buffer starting at postion. To use this method set the
	 * limit and position of the buffer to your needs and note that position is
	 * not the block number. This method is indened for bulk data retrieval such as
	 * the inode.read()  
	 */
	public void readToBuffer(long position, ByteBuffer buf) throws IoError {
	    try {
	        buf.order(ByteOrder.BIG_ENDIAN);
	        blockdev.read(buf, position);
	    } catch (IOException e) {
	        throw new IoError();
	    }
	}
	
	public void writeFromBuffer(long position, ByteBuffer buf) throws IoError {
	    buf.order(ByteOrder.BIG_ENDIAN);
	    try {
            blockdev.write(buf, position);
        } catch (IOException e) {
            throw new IoError();
        }
	}
	
	/**
	 * Zero out part of a block
	 * @throws IOException 
	 */
	public void zeroOut(long nr, int start, int end) throws IoError {
	    ByteBuffer zeros = ByteBuffer.allocate((end-start)+1);
	    writePartial(nr, start, zeros);
	}

	/** Write a whole block to the logical address nr on disk */
	public void write(long nr, ByteBuffer buf) throws IoError {
		buf.rewind();
		try {
		    blockdev.position(((long)(nr & 0xffffffff)) * blocksize);
		    blockdev.write(buf);
		} catch (IOException e) {
		    throw new IoError();
		}
	}	
	
    public void dumpByteBuffer(ByteBuffer buf) {
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
	public void writePartial(long nr, long offset, ByteBuffer buf) throws IoError {
        if (offset + buf.limit() > blocksize)
            throw new IllegalArgumentException("attempt to write over block boundries" + buf + ", " + offset);
        
        try {
            buf.rewind();
            blockdev.write(buf, ((((long)(nr & 0xffffffff)) * blocksize) + offset));
        } catch (IOException e) {
            throw new IoError();
        }
	}
			
	public void initialize(Superblock superblock) {
	    blocksize = superblock.getBlocksize();
		ptrs = superblock.getAddressesPerBlock();
	}

	/**
     * Read block pointers from block. 
     * Note: Zero pointers are not skipped
     * @param   dataBlock   physical block number
     * @param   start       index of first pointer to retrieve
     * @param   end         index of last pointer to retrieve
     * @return  array containing all block numbers in block 
     */
    public long[] readBlockNrsFromBlock(long dataBlock, int start, int end) throws IoError {
        long[] result = new long[(end-start)+1];
        ByteBuffer buffer;

        buffer = read(dataBlock);
        buffer.limit((end+1) * 4);

        for (int i=0; i<=(end-start); i++) {
            result[i] = Ext2fsDataTypes.getLE32U(buffer, (start+i)*4);
        }
        
        return result;
    }

    /**
     * Read block pointers from block. Skip pointers that are zero.  
     * Note: Since we return a list, you cannot use indices computed 
     * for the block with the result.
     * @param   dataBlock   physical block number
     * @param   start       index of first pointer to retrieve
     * @param   end         index of last pointer to retrieve
     * @return  list of non-zero block numbers found in block
     */
    public LinkedList<Long> 
    readBlockNrsFromBlockSkipZeros(long dataBlock, int start, int end) throws IoError {
        LinkedList<Long> result = new LinkedList<Long>();
        ByteBuffer buffer;
        
        buffer = read(dataBlock);
        buffer.limit((end+1) * 4);
        
        for (int i=0; i<=(end-start); i++) {
            long tmp = Ext2fsDataTypes.getLE32U(buffer, (start+i)*4);
            if (tmp > 0)
                result.add(tmp);
        }
        
        return result;
    }

    /**
     * Read all block pointers from block.
     * @param   dataBlock   physical block number
     * @return  array with all pointers stored in block
     */
    public long[] readAllBlockNumbersFromBlock(long dataBlock) throws IoError {
        return readBlockNrsFromBlock(dataBlock, 0, ptrs-1);
    }

    /**
     * Read all block pointers from block. Skip Zero pointers
     * @param   dataBlock   physical block number
     * @return  list with all non-zero pointers in block
     */
    public LinkedList<Long> 
    readAllBlockNumbersFromBlockSkipZero(long dataBlock) throws IoError {
        return readBlockNrsFromBlockSkipZeros(dataBlock, 0, ptrs-1);
    }

    /**
     * Read single block pointer from block.
     * @param   dataBlock   physical block number
     * @param   index       index of block number to retrieve
     */
    public long readBlockNumberFromBlock(long dataBlock, int index) throws IoError {
        return (readBlockNrsFromBlock(dataBlock, index, index))[0];
    }

    public static BlockAccess getInstance() {
		return BlockAccess.instance;
	}
	

	
	
	
}
