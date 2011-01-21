package jext2;

import java.nio.ByteBuffer;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/* Directory entry structure for linked list directories 
 */

public class DirectoryEntry {

	private int ino;
	private short recLen;
	private byte nameLen = 0;
	private byte fileType;
	private String name;

	public final int getIno() {
		return this.ino;
	}
	public final short getRecLen() {
		return this.recLen;
	}
	public final byte getNameLen() {
		return this.nameLen;
	}
	public final byte getFileType() {
		return this.fileType;
	}
	public final String getName() {
		return this.name;
	}
	public final void setIno(int ino) {
        this.ino = ino;
    }
    public final void setFileType(byte fileType) {
        this.fileType = fileType;
    }	
    public final void setRecLen(short recLen) {
        this.recLen = recLen;
    }
    public final boolean isUnused() {
        return (this.ino == 0);
    }
    
	protected void read(ByteBuffer buf, int offset) throws IOException {
		this.ino = Ext2fsDataTypes.getLE32(buf, 0 + offset);
		this.recLen = Ext2fsDataTypes.getLE16(buf, 4 + offset);
		this.nameLen = Ext2fsDataTypes.getLE8(buf, 6 + offset);
		this.fileType = Ext2fsDataTypes.getLE8(buf, 7 + offset);
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
	    if (name.length() > Constants.EXT2_NAME_LEN) {
	        throw new RuntimeException("dirname to long");
	    }
	    
	    DirectoryEntry dir = new DirectoryEntry();
	    
	    dir.nameLen = (byte)(name.length() + (-1*name.length() % 4)); 	    
	    String namePadded = StringUtils.rightPad(name, dir.nameLen, '\0'); 	    
	    dir.name = namePadded;
	    
	    return dir;
	}

	public static DirectoryEntry fromByteBuffer(ByteBuffer buf, int offset) {				
		DirectoryEntry dir = new DirectoryEntry();
		try {
			dir.read(buf, offset);
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
	
	
    public ByteBuffer toByteBuffer() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4 + 2 + 1 + 1 + this.nameLen);
        
        Ext2fsDataTypes.putLE32(buf, this.ino, 0);
        Ext2fsDataTypes.putLE16(buf, this.recLen, 4);
        Ext2fsDataTypes.putLE8(buf, this.nameLen, 6);
        Ext2fsDataTypes.putLE8(buf, this.fileType, 7);
        Ext2fsDataTypes.putString(buf, this.name, this.nameLen, 8);
        
        return buf;
    }

}
	
	

