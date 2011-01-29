package jext2;

import java.nio.ByteBuffer;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Directory entry structure for linked list directories. 
 */

public class DirectoryEntry extends PartialBlock {

	private long ino;
	private int recLen;
	private short nameLen = 0;
	private short fileType;
	private String name;

	public static final int MAX_NAME_LEN = 255;
	public static final short FILETYPE_UNKNOWN  = 0;
    public static final short FILETYPE_REG_FILE = 1;
    public static final short FILETYPE_DIR      = 2;
    public static final short FILETYPE_CHRDEV   = 3;
    public static final short FILETYPE_BLKDEV   = 4;
    public static final short FILETYPE_FIFO     = 5;
    public static final short FILETYPE_SOCK     = 6;
    public static final short FILETYPE_SYMLINK  = 7;
    public static final short FILETYPE_MAX      = 8;

    // EXT2_DIR_PAD defines the directory entries boundaries
    // NOTE: It must be a multiple of 4
    public static final int DIR_PAD     = 4;
    public static final int DIR_ROUND   = (DIR_PAD - 1);
    public static final int MAX_REC_LEN = ((1<<16)-1);
    
	public DirectoryEntry(long blockNr, int offset) {
	    super(blockNr, offset);
    }
	public DirectoryEntry() {
	    this(-1,-1);
	}

    public final long getIno() {
		return this.ino;
	}
	public final int getRecLen() {
		return this.recLen;
	}
	public final short getNameLen() {
		return this.nameLen;
	}
	public final short getFileType() {
		return this.fileType;
	}
	public final String getName() {
		return this.name;
	}
	public final void setIno(long ino) {
        this.ino = ino;
    }
    public final void setFileType(short fileType) {
        this.fileType = fileType;
    }	
    public final void setRecLen(int recLen) {
        this.recLen = recLen;
    }
    /** set entry length to minimum required */ 
    public final void truncateRecord() {
        this.recLen = minSizeNeeded(this.nameLen);
    }
    
    
    public final boolean isUnused() {
        return (this.ino == 0);
    }
    
	protected void read(ByteBuffer buf) throws IOException {
		this.ino = Ext2fsDataTypes.getLE32U(buf, 0 + offset);
		this.recLen = Ext2fsDataTypes.getLE16U(buf, 4 + offset);
		this.nameLen = Ext2fsDataTypes.getLE8U(buf, 6 + offset);
		this.fileType = Ext2fsDataTypes.getLE8U(buf, 7 + offset);
		this.name = Ext2fsDataTypes.getString(buf, 8 + offset, this.nameLen);
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
	
	/**
	 * Create a new directory entry. Note that the name is mandatory because
	 * it dictates the record length on disk
	 */
	public static DirectoryEntry create(String name) {
	    if (name.length() > MAX_NAME_LEN) {
	        throw new RuntimeException("dirname to long");
	    }
	    
	    DirectoryEntry dir = new DirectoryEntry();
	    
	    /*
	     * The directory entry must be divisible by 4, so the name 
	     * gets zero padded
	     */
	    short nameLen = (short)(name.length());
	    short padNameLen = (short)(nameLen + (DIR_PAD - (nameLen % DIR_PAD))); 	    

	    String namePadded = StringUtils.rightPad(name, padNameLen, (char)(0x00)); 	    
	    
	    dir.recLen = (short)(8 + padNameLen);
	    dir.nameLen = nameLen;
	    dir.name = namePadded;

	    if (dir.recLen > MAX_REC_LEN)
	        throw new RuntimeException("MAX_REC_LEN");
	    
	    return dir;
	}
	
	/**
	 * Create the dummy directory entry which occupies the unused part of a 
	 * block.
	 */
	public static DirectoryEntry createRestDummy(DirectoryEntry last) {
	    DirectoryEntry dir = new DirectoryEntry();
	    dir.fileType = (short)0;
	    dir.ino = 0;
	    dir.nameLen = 0;
	    dir.recLen = Superblock.getInstance().getBlocksize() - last.getRecLen();       
	    
	    return dir;
	}
	
	public static int numPadBytes(int nameLen) {
	    return 4 - (nameLen % 4);
	}
	
	/**
	 * Return entry size based on name length
	 */
	public static int minSizeNeeded(int nameLen) {
	    return 8 + nameLen + numPadBytes(nameLen);
	}

	public static DirectoryEntry fromByteBuffer(ByteBuffer buf, long blockNr, int offset) {				
		DirectoryEntry dir = new DirectoryEntry(blockNr, offset);
		try {
			dir.read(buf);
			return dir;
		} catch (IOException e) {
			// XXX don't ignore
			return null;
		}
	}
	
	public static int readRecLen(ByteBuffer buf, int offset) {
	    int recLen;
	    try {
	        recLen = Ext2fsDataTypes.getLE16(buf, offset + 4);
	    } catch (IOException e) {
	        recLen = 0;
	    }	    
	    return recLen;
	}		
	
	/**
	 * Export data structure to ByteBuffer which in turn can be written
	 * to disk
	 */
    public ByteBuffer toByteBuffer() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4 + 2 + 1 + 1 + this.nameLen);
        
        Ext2fsDataTypes.putLE32U(buf, this.ino, 0);
        Ext2fsDataTypes.putLE16U(buf, this.recLen, 4);
        Ext2fsDataTypes.putLE8U(buf, this.nameLen, 6);
        Ext2fsDataTypes.putLE8U(buf, this.fileType, 7);
        Ext2fsDataTypes.putString(buf, this.name, this.nameLen, 8);
        
        return buf;
    }

    @Override
    public void write() throws IOException {
        // TODO Auto-generated method stub
        
    }

}
	
	

