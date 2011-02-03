package jext2;

import java.nio.ByteBuffer;
import java.util.Date;

import jext2.exceptions.FileTooLarge;
import jext2.exceptions.IoError;
import jext2.exceptions.NoSpaceLeftOnDevice;

public class SymlinkInode extends DataInode {
	private String symlink = "";

	public int FAST_SYMLINK_MAX=Constants.EXT2_N_BLOCKS * 4;

	  
    private String readSlowSymlink() throws IoError, FileTooLarge {
        ByteBuffer buf = readData((int)getSize(), 0);
        return Ext2fsDataTypes.getString(buf, 0, buf.limit());
    }       
    private void writeSlowSymlink(String link) throws IoError, NoSpaceLeftOnDevice, FileTooLarge {
        ByteBuffer buf = ByteBuffer.allocate(Ext2fsDataTypes.getStringByteLength(link));
        Ext2fsDataTypes.putString(buf, link, buf.capacity(), 0);
        buf.rewind();
        writeData(buf, 0);
    }
    
    private String readFastSymlink(ByteBuffer buf) throws IoError {
        return Ext2fsDataTypes.getString(buf, 40 + offset, (int)getSize());
    }
    
	public final String getSymlink() throws IoError, FileTooLarge {
	    if (isFastSymlink())
	        return symlink;
	    else
	        return readSlowSymlink();
	}
	
	/**
	 * Set new symlink. We either write data blocks or characters to the 
	 * data block pointer depending on the symlink length.
	 * @throws FileTooLarge This is unlikely to happen because is requires 
	 *     a symlink spanning thousands of blocks. 
	 */
	public void setSymlink(String link) throws NoSpaceLeftOnDevice, IoError, FileTooLarge {
	    int newSize = Ext2fsDataTypes.getStringByteLength(link);
	    
	    setSize(0);

	    /* slow to fast (or vice versa) cleanup */ 
	    if (!isFastSymlink() && newSize >= FAST_SYMLINK_MAX) {
	        try {
	            accessData().truncate(0);
	        } catch (FileTooLarge e) {
	            throw new RuntimeException("should not happen");
	        }
	    } else {
	        this.symlink = "";
	    }
	            
	    if (newSize < FAST_SYMLINK_MAX) { /* fast symlink */
	        this.symlink = link;
	        setSize(newSize);
	        setBlocks(0);
	        write();
	    } else { /* slow symlink */
	        writeSlowSymlink(link);
	    }

	    setStatusChangeTime(new Date());
	}
	
	protected SymlinkInode(long blockNr, int offset) throws IoError {
		super(blockNr, offset);
	}

	/**
	 * Check if inode is a fast symlink. A fast symlink stores the link in the
	 * data block pointers instead of data blocks itself
	 * 
	 * @return true if Inode is fast symlink; false otherwise.
	 */
	public boolean isFastSymlink() {
	    return (getBlocks() == 0);
	}
    
	protected void read(ByteBuffer buf) throws IoError {
        super.read(buf);
        
        if (isFastSymlink()) 
            this.symlink = readFastSymlink(buf); 
    }
    
    protected void write(ByteBuffer buf) throws IoError {
        if (isFastSymlink() && getSize() > 0) 
            Ext2fsDataTypes.putString(buf, symlink, (int)getSize(), 40);
        super.write(buf);
    }
    
    public void write() throws IoError {
        ByteBuffer buf = allocateByteBuffer();
        write(buf);
    }
	
	public static SymlinkInode fromByteBuffer(ByteBuffer buf, int offset) throws IoError {
		SymlinkInode inode = new SymlinkInode(-1, offset);
		inode.read(buf);
		return inode;
	}	
	
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_SYMLINK;
	}
	
    /**
     * Create empty Inode. Initialize *Times, block array.
     */
    public static SymlinkInode createEmpty() throws IoError {
        SymlinkInode inode = new SymlinkInode(-1, -1);
        Date now = new Date();
        
        inode.setModificationTime(now);
        inode.setAccessTime(now);
        inode.setStatusChangeTime(now);
        inode.setDeletionTime(new Date(0));
        inode.setMode(Mode.IFLNK | 0777);
        inode.setBlock(new long[Constants.EXT2_N_BLOCKS]);
        inode.setBlocks(0);
        inode.symlink = "";
        inode.setSize(0);
        
        return inode;
    }
}