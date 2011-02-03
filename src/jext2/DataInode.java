package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;

import jext2.exceptions.FileTooLarge;
import jext2.exceptions.NoSpaceLeftOnDevice;

/**
 * Base class for inodes with data blocks. Like Symlinks, Directories, Regular Files
 */
public class DataInode extends Inode {
    Superblock superblock = Superblock.getInstance();
    BlockAccess blockAccess = BlockAccess.getInstance();
    DataBlockAccess dataAccess = null;

    private long[] block;
    private long blocks = 0;
    
    
    public final long getBlocks() {
        return this.blocks;
    }  
    public final long[] getBlock() {
        return this.block;
    }
    public final void setBlocks(long blocks) {
        this.blocks = blocks;
    }
    public final void setBlock(long[] block) {
        this.block = block;
    }
    
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
     * @throws FileTooLarge 
     * @throws InvalidArgument 
     */ 
    public ByteBuffer readData(int size, long offset) throws IOException, FileTooLarge {
        ByteBuffer result = ByteBuffer.allocateDirect(size);
    
        int blocksize = superblock.getBlocksize();
        long start = offset / blocksize;
        long max = (size/blocksize) + start;
        
        offset = offset % blocksize;
        int firstInBuffer = (int) offset;
        
        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocks(start, max-start);
            int count = 0;
            
            /**
             * Sparse file support:
             * getBlocks will return null if there is no data block for this 
             * logical address. So just move the position count blocks forward.
             */
            
            if (b == null) { /* hole */
                count = 1;
                result.limit(result.position() + count * blocksize);                
                result.position(result.position() + count * blocksize);
            } else { /* blocks */
                count = b.size();
                result.limit(result.position() + count * blocksize);
                blockAccess.readToBuffer(
                        (((long)(b.getFirst() & 0xffffffff)) * blocksize)
                        + offset, result);
            }

            start += count;          
            offset = 0;
        }
    
        result.position(firstInBuffer);
        return result;
    }

    /**
     * Write data in buffer to disk. May trigger an write() when data size 
     * grows.
     * @throws NoSpaceLeftOnDevice 
     * @throws FileTooLarge 
     */    
    public int writeData(ByteBuffer buf, long offset) throws IOException, NoSpaceLeftOnDevice, FileTooLarge {
        /*
         * Note on sparse file support:
         * getBlocksAllocate does not care if there are holes. Just write as much 
         * blocks as the buffer requires at the desired location an set inode.size
         * accordingly. 
         */        
        int blocksize = superblock.getBlocksize();
        long start = offset / blocksize;
        long max = Math.max(start+1, (long)Math.ceil((double)buf.limit() / (double)blocksize) + start);        
        long bufOffset = offset % blocksize;        

        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocksAllocate(start, max-start);
        
            int count = b.size();
            buf.limit(Math.min(buf.position() + count * blocksize,
                               buf.capacity()));
            
            blockAccess.writeFromBuffer(b.getFirst() * blocksize + bufOffset, buf);
                        
            start += b.size();          
            bufOffset = 0;
        }
        
        int written = buf.position();
        
        /* increase inode.size if we grew the file */
        if (offset + written > getSize()) { /* file grew */
            setStatusChangeTime(new Date());
            setSize(offset + written);
            write();
        } 

        return buf.position();
    }  
    
    protected DataInode(long blockNr, int offset) {
        super(blockNr, offset);
    }
    
    protected void read(ByteBuffer buf) throws IOException {
        super.read(buf);
        this.blocks = Ext2fsDataTypes.getLE32U(buf, 28 + offset);
        
        if (!(this instanceof SymlinkInode && ((SymlinkInode)this).isFastSymlink())) {            
            this.block = new long[Constants.EXT2_N_BLOCKS];
            for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
                this.block[i] = Ext2fsDataTypes.getLE32U(buf, 40 + (i*4) + offset);
            }
        }
    }
    
    
    protected void write(ByteBuffer buf) throws IOException {
        if (!(this instanceof SymlinkInode)) {
            for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
                Ext2fsDataTypes.putLE32U(buf, this.block[i], 40 + (i*4));
            }
        }
        Ext2fsDataTypes.putLE32U(buf, this.blocks, 28);
        super.write(buf);
    }
    
    public void write() throws IOException {
        ByteBuffer buf = allocateByteBuffer();
        write(buf);
    }

}
