package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import jext2.exceptions.FileTooLarge;

/**
 * Inode for regular files. 
 */
public class RegularInode extends DataInode {
    protected RegularInode(long blockNr, int offset) throws IOException {
        super(blockNr, offset);
    }
	
	public static RegularInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		RegularInode inode = new RegularInode(-1, offset);
		inode.read(buf);
		return inode;
	}
	
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_REG_FILE;
	}

    /**
     * Set size and truncate.
     * @param   size    new size
     * @throws FileTooLarge 
     */
    public void setSizeAndTruncate(long size) throws IOException, FileTooLarge {
        long oldSize = getSize();
        setSize(size);
        if (oldSize > size)
            accessData().truncate(size);
    }

    /**
     * Create empty Inode. Initialize *Times, block array.
     */
    public static RegularInode createEmpty() throws IOException {
    	RegularInode inode = new RegularInode(-1, -1);
    	Date now = new Date();
    	
    	inode.setModificationTime(now);
    	inode.setAccessTime(now);
    	inode.setStatusChangeTime(now);
    	inode.setDeletionTime(new Date(0));
    	inode.setMode(Mode.IFREG);
    	inode.setBlock(new long[Constants.EXT2_N_BLOCKS]);
    	
    	return inode;
    }
}	
