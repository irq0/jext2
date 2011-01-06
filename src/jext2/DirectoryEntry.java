package jext2;

import java.nio.ByteBuffer;
import java.io.IOException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/* directory structure for linked list directories */

public class DirectoryEntry extends Block {

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

	protected void read(ByteBuffer buf) throws IOException {
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

	protected DirectoryEntry(int blockNr, int offset) {
		super(blockNr, offset);
	}
	
	public static DirectoryEntry fromByteBuffer(ByteBuffer buf, int offset) {
				
		DirectoryEntry dir = new DirectoryEntry(-1, offset);
		try {
			dir.offset = offset;
			dir.read(buf);
			return dir;
		} catch (IOException e) {
			// XXX dont irgnore
			return null;
		}
	}
}
	
	

