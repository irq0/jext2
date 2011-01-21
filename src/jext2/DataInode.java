package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Base class for inodes with a data area. Like Symlinks, Directories, Regular Files
 */
public class DataInode extends Inode {
    Superblock superblock = Superblock.getInstance();
    BlockAccess blocks = BlockAccess.getInstance();
    DataBlockAccess dataAccess = null;

    /**
     * Get the data access provider to read and write to the data area of this
     * inode
     */
    public DataBlockAccess accessData() {
        if (dataAccess == null)
            dataAccess = DataBlockAccess.fromInode(this);
        return dataAccess;
    }
    
    
    
    
    
    
    /**
     * Read Inode data 
     * @param  size    size of the data to be read
     * @param  offset  start adress in data area
     * @return buffer of size size containing data.
     */ 
    public ByteBuffer readData(int size, int offset) throws IOException {
        ByteBuffer result = ByteBuffer.allocateDirect(size);
    
        int blocksize = superblock.getBlocksize();
        int start = offset / blocksize;
        int max = size / blocksize + start;
        offset = offset % blocksize;        
    
        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocks(start, max-start);
    
            // getBlocks returns null in case create=false and the block does not exist. FUSE can
            // and will request not existing blocks. 
            if (b == null) {
                break;
            }
    
            int count = b.size();
            result.limit(result.position() + count * blocksize);
            blocks.readToBuffer((((long)(b.getFirst() & 0xffffffff)) * blocksize) + offset, result);
            start += b.size();          
            offset = 0;
        }
    
        result.position(offset);
        return result;
    }

    /**
     * Old implementation of readData. Dont use this. Its just for reference how not to do it
     */
    public ByteBuffer readDataSlow(int size, int offset) throws IOException {
        ByteBuffer result = ByteBuffer.allocateDirect(size);
    
        int start = offset / superblock.getBlocksize();
        int max = size / superblock.getBlocksize() + start;
        offset = offset % superblock.getBlocksize();        
        LinkedList<Long> blockNrs = new LinkedList<Long>();   
        
        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocks(start, max-start);
            
            // getBlocks returns null in case create=false and the block does not exist. FUSE can
            // and will request not existing blocks. 
            if (b == null) {
                break;
            }
                        
            start += b.size();          
            blockNrs.addAll(b);
        }
        
        for (long nr : blockNrs) {
            ByteBuffer block = blocks.read((int)nr);
            block.position(offset);
    
            while (result.hasRemaining() && block.hasRemaining()) {
                    result.put(block.get());
            }
    
            offset = 0;
        }
        
        result.rewind();
        return result;
    }

    public int writeData(ByteBuffer buf, int offset) throws IOException {
        System.out.println("WRITE DATA: " + buf + " AT: " + offset);
        
        int start = offset / superblock.getBlocksize();
        int max = buf.capacity() / superblock.getBlocksize() + start + 1;
        buf.rewind();
    
        System.out.println("START: " + start + " MAX: " + max);
        
        LinkedList<Long> blockNrs = new LinkedList<Long>();
        
        /* get all the blocks needed to hold buf */
        while (start < max) {
            LinkedList<Long> b = accessData().getBlocksAllocate(start, max-start);
            
            if (b == null) 
                break;
            
            start += b.size();
            blockNrs.addAll(b);
        }
    
        /* iterate blocks and write buf */
        int blocksize = superblock.getBlocksize();
        int blockOffset = offset % blocksize;
        int bufOffset = 0;
        int remaining = buf.capacity();
        for (long nr : blockNrs) {
            int bytesToWrite = remaining;
            if (bytesToWrite > blocksize)
                bytesToWrite = blocksize - blockOffset;
            
            blocks.writePartial((int) nr, blockOffset, buf, bufOffset, bytesToWrite); 
            
            remaining -= bytesToWrite;
            bufOffset += bytesToWrite;
            blockOffset = 0;
        }
        return bufOffset;
    }
    
    protected DataInode(long blockNr, int offset) throws IOException {
        super(blockNr, offset);
    }

}
