package jext2;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.Date;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Inode extends Block {
    private int mode = 0;
	private int gidLow = 0;
	private int uidLow = 0;
	private long size = 0;
	private Date accessTime;
	private Date changeTime;
	private Date modificationTime;
	private Date deletionTime;
	private int linksCount = 0;
	private long blocks = 0;
	private	long flags = 0;
	private long[] block;
	private	long generation = 0;
	private	long fileAcl = 0;
	private	long dirAcl = 0;
	private	long fragmentAddress = 0;

	// Linux OS dependent values
	//private int frag = 0;
	//private int fsize = 0;
	private int uidHigh = 0;
	private int gidHigh = 0;

	// in memory data (ext2_inode_info)
	private int blockGroup = -1;
	private long ino = -1;
	
	
	public final int getMode() {
		return this.mode;
	}
	public final int getGidLow() {
		return this.gidLow;
	}
	public final int getUidLow() {
		return this.uidLow;
	}
	public final long getSize() {
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
	public final int getLinksCount() {
		return this.linksCount;
	}
	public final long getBlocks() {
		return this.blocks;
	}
	public final long getFlags() {
		return this.flags;
	}
	public final long[] getBlock() {
		return this.block;
	}
	public final long getGeneration() {
		return this.generation;
	}
	public final long getFileAcl() {
		return this.fileAcl;
	}
	public final long getDirAcl() {
		return this.dirAcl;
	}
	public final long getFragmentAddress() {
		return this.fragmentAddress;
	}
	public final int getUidHigh() {
		return this.uidHigh;
	}
	public final int getGidHigh() {
		return this.gidHigh;
	}

	public final long getUid() {
		return this.uidLow + (16 << this.uidHigh);
	}
	
	public final long getGid() {
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
	
	public void setUid(long uid) {
	    this.uidLow = (int)(uid & 0xFFFFFFFF);
	    this.uidHigh = (int)((uid >> Integer.SIZE) & 0xFFFFFFFF);
	}
	
	public void setGid(long gid) {
	    this.gidLow = (int)(gid & 0xFFFFFFFF);
	    this.gidHigh = (int)((gid >> Integer.SIZE) & 0xFFFFFFFF);
	}
	
	/** OR mode onto */
	public final void orMode(int mode) {
	    this.mode |=  mode;
	}
	public final void setMode(int mode) {
		this.mode = mode;
	}
	public final void setGidLow(int gidLow) {
		this.gidLow = gidLow;
	}
	public final void setUidLow(int uidLow) {
		this.uidLow = uidLow;
	}
	public final void setSize(long size) {
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
	public final void setLinksCount(int linksCount) {
		this.linksCount = linksCount;
	}
	public final void setBlocks(long blocks) {
		this.blocks = blocks;
	}
	public final void setFlags(long flags) {
		this.flags = flags;
	}
	public final void setBlock(long[] block) {
		this.block = block;
	}
	public final void setGeneration(long generation) {
		this.generation = generation;
	}
	public final void setUidHigh(int uidHigh) {
		this.uidHigh = uidHigh;
	}
	public final void setGidHigh(int gidHigh) {
		this.gidHigh = gidHigh;
	}
	
    protected void write(ByteBuffer buf) throws IOException {
		Ext2fsDataTypes.putLE16U(buf, this.mode, 0);
		Ext2fsDataTypes.putLE16U(buf, this.uidLow, 2);
		Ext2fsDataTypes.putLE32U(buf, this.size, 4);
		Ext2fsDataTypes.putDate(buf, this.accessTime, 8);
		Ext2fsDataTypes.putDate(buf, this.changeTime, 12);
		Ext2fsDataTypes.putDate(buf, this.modificationTime, 16);
		Ext2fsDataTypes.putDate(buf, this.deletionTime, 20);
		Ext2fsDataTypes.putLE16U(buf, this.gidLow, 24);
		Ext2fsDataTypes.putLE16U(buf, this.linksCount, 26);
		Ext2fsDataTypes.putLE32U(buf, this.blocks, 28);
		Ext2fsDataTypes.putLE32U(buf, this.flags, 32);
		//		this.osd1 = Ext2fsDataTypes.getLE32U(buf, 36 + offset);
 		
		for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
			Ext2fsDataTypes.putLE32U(buf, this.block[i], 40 + (i*4));
		}

		Ext2fsDataTypes.putLE32U(buf, this.generation, 100);
		Ext2fsDataTypes.putLE32U(buf, this.fileAcl, 104);
		Ext2fsDataTypes.putLE32U(buf, this.dirAcl, 108);
		Ext2fsDataTypes.putLE32U(buf, this.fragmentAddress, 112);
		
		// this.frag = Ext2fsDataTypes.getLE8(buf, 116 + offset);
		Ext2fsDataTypes.putLE16U(buf, this.uidHigh, 120);
		Ext2fsDataTypes.putLE16U(buf, this.gidHigh, 122);
		
		super.write(buf);
	}
	
	protected void read(ByteBuffer buf) throws IOException {
		this.mode = Ext2fsDataTypes.getLE16U(buf, 0 + offset);
		this.uidLow = Ext2fsDataTypes.getLE16U(buf, 2 + offset);
		this.size = Ext2fsDataTypes.getLE32U(buf, 4 + offset);
		this.accessTime = Ext2fsDataTypes.getDate(buf, 8 + offset);
		this.changeTime = Ext2fsDataTypes.getDate(buf, 12 + offset);
		this.modificationTime = Ext2fsDataTypes.getDate(buf, 16 + offset);
		this.deletionTime = Ext2fsDataTypes.getDate(buf, 20 + offset);
		this.gidLow = Ext2fsDataTypes.getLE16U(buf, 24 + offset);
		this.linksCount = Ext2fsDataTypes.getLE16U(buf, 26 + offset);
		this.blocks = Ext2fsDataTypes.getLE32U(buf, 28 + offset);
		this.flags = Ext2fsDataTypes.getLE32U(buf, 32 + offset);
		//		this.osd1 = Ext2fsDataTypes.getLE32U(buf, 36 + offset);
 		
		this.block = new long[Constants.EXT2_N_BLOCKS];
		for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
			this.block[i] = Ext2fsDataTypes.getLE32U(buf, 40 + (i*4) + offset);
		}
		
		this.generation = Ext2fsDataTypes.getLE32U(buf, 100 + offset);
		this.fileAcl = Ext2fsDataTypes.getLE32U(buf, 104 + offset);
		this.dirAcl = Ext2fsDataTypes.getLE32U(buf, 108 + offset);
		this.fragmentAddress = Ext2fsDataTypes.getLE32U(buf, 112 + offset);
		
		// this.frag = Ext2fsDataTypes.getLE8(buf, 116 + offset);
		this.uidHigh = Ext2fsDataTypes.getLE16U(buf, 120 + offset);
		this.gidHigh = Ext2fsDataTypes.getLE16U(buf, 122 + offset);
	}


	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
	
	protected Inode(long blockNr, int offset) {
		super(blockNr, offset);
	}

	public static Inode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {		
		Inode inode = new Inode(-1, offset);
		inode.read(buf);
		return inode;
	}

	public boolean equals(Inode other) {
		return (this.blockNr == other.blockNr) &&
			(this.offset == other.offset);
	}

	public int hashCode() {
		return (int)(this.blockNr ^ this.offset);
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
	
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_UNKNOWN;
	}
	
}




