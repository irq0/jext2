package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Base class for inodes with data blocks. Like Symlinks, Directories, Regular Files
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
        int max = Math.min(size / blocksize + start,
                           (int)(getBlocks()/(blocksize/512)));
        offset = offset % blocksize;
        int firstInBuffer = offset;
        
        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocks(start, max-start);
    
            // getBlocks returns null in case create=false and the block 
            // does not exist. FUSE can and will request not existing blocks. 
            if (b == null) {
                break;
            }
    
            int count = b.size();
            result.limit(result.position() + count * blocksize);
            blocks.readToBuffer(
                    (((long)(b.getFirst() & 0xffffffff)) * blocksize)
                    + offset, result);
            start += b.size();          
            offset = 0;
        }
    
        result.position(firstInBuffer);
        return result;
    }

    /**
     * Old implementation of readData. Dont use this. Its just for reference how 
     * not to do it
     */
    public ByteBuffer readDataSlow(int size, int offset) throws IOException {
        ByteBuffer result = ByteBuffer.allocateDirect(size);
    
        int start = offset / superblock.getBlocksize();
        int max = size / superblock.getBlocksize() + start;
        offset = offset % superblock.getBlocksize();        
        LinkedList<Long> blockNrs = new LinkedList<Long>();   
        
        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocks(start, max-start);
            
            // getBlocks returns null in case create=false and the block 
            // does not exist. FUSE can and will request not existing blocks. 
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

    /**
     * Write data in buffer to disk. May trigger an write() when data size 
     * grows.
     */    
    public int writeData(ByteBuffer buf, int offset) throws IOException {
        int blocksize = superblock.getBlocksize();
        int start = offset / blocksize;
        int max = buf.limit() / blocksize + start;
        int bufOffset = offset % blocksize;        

        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocksAllocate(start, max-start);
        
            if (b == null) {
                throw new IOException();
            }
        
            int count = b.size();
            buf.limit(Math.min(buf.position() + count * blocksize,
                               buf.capacity()));
            blocks.writeFromBuffer(
                    (((long)(b.getFirst() & 0xffffffff)) * blocksize) + bufOffset,
                    buf);
            
            start += b.size();          
            bufOffset = 0;
        }
        
        int written = buf.position();
        
        /* increase inode.size if we grew the file */
        if (offset + written > getSize()) { /* file grew */
            setSize(offset + written);
            write();
        } 
        
        return buf.position();
    }  
    
    protected DataInode(long blockNr, int offset) {
        super(blockNr, offset);
    }

}
