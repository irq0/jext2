package jext2;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.lang.builder.HashCodeBuilder;

import jext2.exceptions.FileTooLarge;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;
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
    
	public boolean hasDataBlocks() {
		return getBlocks() > 0;
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
     * @param  offset  start address in data area
     * @return buffer of size size containing data.
     * @throws FileTooLarge
     * @throws IoError
     */ 
    public ByteBuffer readData(int size, long offset) throws JExt2Exception, FileTooLarge {
        ByteBuffer result = ByteBuffer.allocateDirect(size);
    
        int blocksize = superblock.getBlocksize();
        long start = offset / blocksize;
        long max = (size/blocksize) + start;
        
        offset = offset % blocksize;
        int firstInBuffer = (int) offset;
        
        while (start < max) { 
            LinkedList<Long> b = accessData().getBlocks(start, max-start);
            int count;
            
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
                blockAccess.readToBufferUnsynchronized(b.getFirst() * blocksize + offset, result);
            }

            start += count;          
            offset = 0;
        }
    
        result.position(firstInBuffer);
        return result;
    }

    /**
     * Write data in buffer to disk. This works best when whole blocks which 
     * are a multiple of blocksize in size are written. Partial blocks are
     * written by first reading the block and then writing the new data 
     * to that buffer than write that new buffer to disk.
     * @throws NoSpaceLeftOnDevice 
     * @throws FileTooLarge 
     */    
    public int writeData(ByteBuffer buf, long offset) throws JExt2Exception, NoSpaceLeftOnDevice, FileTooLarge {
        /*
         * Note on sparse file support:
         * getBlocksAllocate does not care if there are holes. Just write as much 
         * blocks as the buffer requires at the desired location an set inode.size
         * accordingly. 
         */        
        
        int blocksize = superblock.getBlocksize();
        long start = offset/blocksize;
        long end = (buf.capacity()+blocksize)/blocksize + start;
        int startOff = (int)(offset%blocksize);
        
        if (startOff > 0)
            end++;
        
        buf.rewind();
        
        while (start < end) {
            LinkedList<Long> blockNrs = accessData().getBlocksAllocate(start, 1);
            int bytesLeft = buf.capacity() - buf.position();
            
            if (bytesLeft < blocksize || startOff > 0) { /* write partial block */
                ByteBuffer disk = blockAccess.read(blockNrs.getFirst());
                disk.position(startOff);
                
                buf.limit(Math.min(buf.position() + bytesLeft, disk.remaining()));
                disk.put(buf);
                blockAccess.write(blockNrs.getFirst(), disk);
            } else { /* write whole block */
                buf.limit(buf.position() + blocksize);
                blockAccess.writeFromBufferUnsynchronized(
                        (blockNrs.getFirst()) * blocksize, buf);
            }
            
            start += 1;
            startOff = 0;
        }
        int written = buf.position();
        
        /* increase inode.size if we grew the file */
        if (offset + written > getSize()) { /* file grew */
            setStatusChangeTime(new Date());
            setSize(offset + written);
        }

        assert buf.position() == buf.limit();
        
        return buf.position();
    }  
    
    protected DataInode(long blockNr, int offset) {
        super(blockNr, offset);
    }
    
    protected void read(ByteBuffer buf) throws IoError {
        super.read(buf);
        this.blocks = Ext2fsDataTypes.getLE32U(buf, 28 + offset);
        
        if (!isFastSymlink()) {            
            this.block = new long[Constants.EXT2_N_BLOCKS];
            for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
                this.block[i] = Ext2fsDataTypes.getLE32U(buf, 40 + (i*4) + offset);
            }
        }
    }
    
    
    protected void write(ByteBuffer buf) throws IoError {
        if (!isFastSymlink()) {
            for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
                Ext2fsDataTypes.putLE32U(buf, this.block[i], 40 + (i*4));
            }
        }
        Ext2fsDataTypes.putLE32U(buf, this.blocks, 28);
        super.write(buf);
    }
    
    public void write() throws IoError {
        ByteBuffer buf = allocateByteBuffer();
        write(buf);
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(blocks)
            .append(block)
            .toHashCode();
    }
    
}
