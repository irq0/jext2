package jext2;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.Date;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Inode extends Block {
	private short mode;
	private short gidLow;
	private short uidLow;
	private int size;
	private Date accessTime;
	private Date changeTime;
	private Date modificationTime;
	private Date deletionTime;
	private short linksCount;
	private int blocks;
	private	int flags;
	private int[] block;
	private	int generation;
	private	int fileAcl;
	private	int dirAcl;
	private	int fragmentAddress;

	// Linux OS dependent values
	private byte frag = 0;
	private byte fsize = 0;
	private short uidHigh;
	private short gidHigh;
	
	public final short getMode() {
		return this.mode;
	}
	public final short getGidLow() {
		return this.gidLow;
	}
	public final short getUidLow() {
		return this.uidLow;
	}
	public final int getSize() {
		return this.size;
	}
	public final Date getAccessTime() {
		return this.accessTime;
	}
	public final Date getChangeTime() {
		return this.changeTime;
	}
	public final Date getModificationTime() {
		return this.modificationTime;
	}
	public final Date getDeletionTime() {
		return this.deletionTime;
	}
	public final short getLinksCount() {
		return this.linksCount;
	}
	public final int getBlocks() {
		return this.blocks;
	}
	public final int getFlags() {
		return this.flags;
	}
	public final int[] getBlock() {
		return this.block;
	}
	public final int getGeneration() {
		return this.generation;
	}
	public final int getFileAcl() {
		return this.fileAcl;
	}
	public final int getDirAcl() {
		return this.dirAcl;
	}
	public final int getFragmentAddress() {
		return this.fragmentAddress;
	}
	public final byte getFrag() {
		return this.frag;
	}
	public final byte getFsize() {
		return this.fsize;
	}
	public final short getUidHigh() {
		return this.uidHigh;
	}
	public final short getGidHigh() {
		return this.gidHigh;
	}

	public final int getUid() {
		return this.uidLow + (16 << this.uidHigh);
	}
	
	protected void read(ByteBuffer buf) throws IOException {
		this.mode = Ext2fsDataTypes.getLE16(buf, 0 + offset);
		this.uidLow = Ext2fsDataTypes.getLE16(buf, 2 + offset);
		this.size = Ext2fsDataTypes.getLE32(buf, 4 + offset);
		this.accessTime = Ext2fsDataTypes.getDate(buf, 8 + offset);
		this.changeTime = Ext2fsDataTypes.getDate(buf, 12 + offset);
		this.modificationTime = Ext2fsDataTypes.getDate(buf, 16 + offset);
		this.deletionTime = Ext2fsDataTypes.getDate(buf, 20 + offset);
		this.gidLow = Ext2fsDataTypes.getLE16(buf, 24 + offset);
		this.linksCount = Ext2fsDataTypes.getLE16(buf, 26 + offset);
		this.blocks = Ext2fsDataTypes.getLE32(buf, 28 + offset);
		this.flags = Ext2fsDataTypes.getLE32(buf, 32 + offset);
		//		this.osd1 = Ext2fsDataTypes.getLE32U(buf, 36 + offset);
 		
		this.block = new int[Constants.EXT2_N_BLOCKS];
		for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
			this.block[i] = Ext2fsDataTypes.getLE32(buf, 40 + (i*4) + offset);
		}
		
		this.generation = Ext2fsDataTypes.getLE32(buf, 100 + offset);
		this.fileAcl = Ext2fsDataTypes.getLE32(buf, 104 + offset);
		this.dirAcl = Ext2fsDataTypes.getLE32(buf, 108 + offset);
		this.fragmentAddress = Ext2fsDataTypes.getLE32(buf, 112 + offset);
		
		// this.frag = Ext2fsDataTypes.getLE8(buf, 116 + offset);
		this.uidHigh = Ext2fsDataTypes.getLE16(buf, 120 + offset);
		this.gidHigh = Ext2fsDataTypes.getLE16(buf, 122 + offset);
	}


	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
	
	protected Inode(int blockNr, int offset) {
		super(blockNr, offset);
	}

	public static Inode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {		
		Inode inode = new Inode(-1, offset);
		inode.read(buf);
		return inode;
	}
}
