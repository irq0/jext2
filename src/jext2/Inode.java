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

	// in memory data (ext2_inode_info)
	private int blockGroup = -1;
	private long ino = -1;
	
	
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
	
	public final int getGid() {
		return this.gidLow + (16 << this.gidHigh);
	}

	public final int getBlockGroup() {
		return this.blockGroup;
	}
	public void setBlockGroup(int blockGroup) {
		this.blockGroup = blockGroup;
	}
	public final long getIno() {
		return this.ino;
	}
	public void setIno(long ino) {
		this.ino = ino;
	}

	public final void setMode(short mode) {
		this.mode = mode;
	}
	public final void setGidLow(short gidLow) {
		this.gidLow = gidLow;
	}
	public final void setUidLow(short uidLow) {
		this.uidLow = uidLow;
	}
	public final void setSize(int size) {
		this.size = size;
	}
	public final void setAccessTime(Date accessTime) {
		this.accessTime = accessTime;
	}
	public final void setChangeTime(Date changeTime) {
		this.changeTime = changeTime;
	}
	public final void setModificationTime(Date modificationTime) {
		this.modificationTime = modificationTime;
	}
	public final void setDeletionTime(Date deletionTime) {
		this.deletionTime = deletionTime;
	}
	public final void setLinksCount(short linksCount) {
		this.linksCount = linksCount;
	}
	public final void setBlocks(int blocks) {
		this.blocks = blocks;
	}
	public final void setFlags(int flags) {
		this.flags = flags;
	}
	public final void setBlock(int[] block) {
		this.block = block;
	}
	public final void setGeneration(int generation) {
		this.generation = generation;
	}
	public final void setUidHigh(short uidHigh) {
		this.uidHigh = uidHigh;
	}
	public final void setGidHigh(short gidHigh) {
		this.gidHigh = gidHigh;
	}
	
    protected void write(ByteBuffer buf) throws IOException {
		Ext2fsDataTypes.putLE16(buf, this.mode, 0);
		Ext2fsDataTypes.putLE16(buf, this.uidLow, 2);
		Ext2fsDataTypes.putLE32(buf, this.size, 4);
		Ext2fsDataTypes.putDate(buf, this.accessTime, 8);
		Ext2fsDataTypes.putDate(buf, this.changeTime, 12);
		Ext2fsDataTypes.putDate(buf, this.modificationTime, 16);
		Ext2fsDataTypes.putDate(buf, this.deletionTime, 20);
		Ext2fsDataTypes.putLE16(buf, this.gidLow, 24);
		Ext2fsDataTypes.putLE16(buf, this.linksCount, 26);
		Ext2fsDataTypes.putLE32(buf, this.blocks, 28);
		Ext2fsDataTypes.putLE32(buf, this.flags, 32);
		//		this.osd1 = Ext2fsDataTypes.getLE32U(buf, 36 + offset);
 		
		for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
			Ext2fsDataTypes.putLE32(buf, this.block[i], 40 + (i*4));
		}

		Ext2fsDataTypes.putLE32(buf, this.generation, 100);
		Ext2fsDataTypes.putLE32(buf, this.fileAcl, 104);
		Ext2fsDataTypes.putLE32(buf, this.dirAcl, 108);
		Ext2fsDataTypes.putLE32(buf, this.fragmentAddress, 112);
		
		// this.frag = Ext2fsDataTypes.getLE8(buf, 116 + offset);
		Ext2fsDataTypes.putLE16(buf, this.uidHigh, 120);
		Ext2fsDataTypes.putLE16(buf, this.gidHigh, 122);
		
		super.write(buf);
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

	public static Inode createEmpty() {
		Inode inode = new Inode(-1, -1);
		Date now = new Date();
		
		inode.modificationTime = now;
		inode.accessTime = now;
		inode.changeTime = now;
		inode.fragmentAddress = 0;
		inode.frag = 0;
		inode.deletionTime = new Date(0);
		inode.blocks = 0;
		inode.block = new int[Constants.EXT2_N_BLOCKS];
		
		
		return inode;
	}
	
	public boolean equals(Inode other) {
		return (this.blockNr == other.blockNr) &&
			(this.offset == other.offset);
	}

	public int hashCode() {
		return this.blockNr ^ this.offset;
	}
	
	/** allocate a ByteBuffer big enaugh for a Inode */
	protected ByteBuffer allocateByteBuffer() {		
		ByteBuffer buf = ByteBuffer.allocate(Superblock.getInstance().getInodeSize());
		buf.rewind();
		return buf;
	}
	
	public void write() throws IOException {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);
	}
	
}




