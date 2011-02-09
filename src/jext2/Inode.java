package jext2;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.Date;
import java.nio.ByteBuffer;

import jext2.exceptions.IoError;

public class Inode extends PartialBlock {
    private int mode = 0;
	private int gidLow = 0;
	private int uidLow = 0;
	private long size = 0;
	private Date accessTime;
	private Date changeTime;
	private Date modificationTime;
	private Date deletionTime;
	private int linksCount = 0;
	private	long flags = 0;
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
	public long getSize() {
		return this.size;
	}
	/**
     * Time of last access - atime
     */
	public final Date getAccessTime() {
		return this.accessTime;
	}
	/**
	 * Time of last status change - ctime
     */
	public final Date getStatusChangeTime() {
		return this.changeTime;
	}
	/**
	 * Time of last modification - mtime
	 */
	public final Date getModificationTime() {
		return this.modificationTime;
	}	
	public final Date getDeletionTime() {
		return this.deletionTime;
	}
	public final int getLinksCount() {
		return this.linksCount;
	}
	public final long getFlags() {
		return this.flags;
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
		return this.uidLow + (this.uidHigh << Ext2fsDataTypes.LE16_SIZE);
	}
	
	public final long getGid() {
		return this.gidLow + (this.gidHigh << Ext2fsDataTypes.LE16_SIZE);
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
	    this.uidLow = (int)(uid & 0xFFFFFFFFL);
	    this.uidHigh = (int)((uid >> Ext2fsDataTypes.LE32_SIZE) & 0xFFFFFFFF);
	}
	
	public void setGid(long gid) {
	    this.gidLow = (int)(gid & 0xFFFFFFFFL);
	    this.gidHigh = (int)((gid >> Ext2fsDataTypes.LE32_SIZE) & 0xFFFFFFFF);
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
	public void setSize(long size) {
		this.size = size;
	}
	public final void setAccessTime(Date accessTime) {
		this.accessTime = accessTime;
	}
    public final void setStatusChangeTime(Date changeTime) {
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
	public final void setFlags(long flags) {
		this.flags = flags;
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
	protected final void setDirAcl(long dirAcl) {
	    this.dirAcl = dirAcl;
	}
	public final boolean isDeleted() {
	    return (this.deletionTime.after(new Date(0))); 
	}
	
    protected void write(ByteBuffer buf) throws IoError {
		Ext2fsDataTypes.putLE16U(buf, this.mode, 0);
		Ext2fsDataTypes.putLE16U(buf, this.uidLow, 2);
		Ext2fsDataTypes.putLE32U(buf, this.size, 4);
		Ext2fsDataTypes.putDate(buf, this.accessTime, 8);
		Ext2fsDataTypes.putDate(buf, this.changeTime, 12);
		Ext2fsDataTypes.putDate(buf, this.modificationTime, 16);
		Ext2fsDataTypes.putDate(buf, this.deletionTime, 20);
		Ext2fsDataTypes.putLE16U(buf, this.gidLow, 24);
		Ext2fsDataTypes.putLE16U(buf, this.linksCount, 26);
		Ext2fsDataTypes.putLE32U(buf, this.flags, 32);
		Ext2fsDataTypes.putLE32U(buf, this.generation, 100);
		Ext2fsDataTypes.putLE32U(buf, this.fileAcl, 104);
		Ext2fsDataTypes.putLE32U(buf, this.dirAcl, 108);
		Ext2fsDataTypes.putLE32U(buf, this.fragmentAddress, 112);
		Ext2fsDataTypes.putLE16U(buf, this.uidHigh, 120);
		Ext2fsDataTypes.putLE16U(buf, this.gidHigh, 122);
		
		super.write(buf);
	}
	
	protected void read(ByteBuffer buf) throws IoError {
		this.mode = Ext2fsDataTypes.getLE16U(buf, 0 + offset);
		this.uidLow = Ext2fsDataTypes.getLE16U(buf, 2 + offset);
		this.size = Ext2fsDataTypes.getLE32U(buf, 4 + offset);
		this.accessTime = Ext2fsDataTypes.getDate(buf, 8 + offset);
		this.changeTime = Ext2fsDataTypes.getDate(buf, 12 + offset);
		this.modificationTime = Ext2fsDataTypes.getDate(buf, 16 + offset);
		this.deletionTime = Ext2fsDataTypes.getDate(buf, 20 + offset);
		this.gidLow = Ext2fsDataTypes.getLE16U(buf, 24 + offset);
		this.linksCount = Ext2fsDataTypes.getLE16U(buf, 26 + offset);
		this.flags = Ext2fsDataTypes.getLE32U(buf, 32 + offset);		
		this.generation = Ext2fsDataTypes.getLE32U(buf, 100 + offset);
		this.fileAcl = Ext2fsDataTypes.getLE32U(buf, 104 + offset);
		this.dirAcl = Ext2fsDataTypes.getLE32U(buf, 108 + offset);
		this.fragmentAddress = Ext2fsDataTypes.getLE32U(buf, 112 + offset);
		this.uidHigh = Ext2fsDataTypes.getLE16U(buf, 120 + offset);
		this.gidHigh = Ext2fsDataTypes.getLE16U(buf, 122 + offset);
	}


	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		            ToStringStyle.MULTI_LINE_STYLE,
		            false,
		            Inode.class);
	}
	
	protected Inode(long blockNr, int offset) {
		super(blockNr, offset);
	}

	public static Inode fromByteBuffer(ByteBuffer buf, int offset) throws IoError {		
		Inode inode = new Inode(-1, offset);
		inode.read(buf);
		return inode;
	}

	public boolean equals(Inode other) {
		return (this.nr == other.nr) &&
			(this.offset == other.offset);
	}
	
	/** allocate a ByteBuffer big enaugh for a Inode */
	protected ByteBuffer allocateByteBuffer() {		
		ByteBuffer buf = ByteBuffer.allocate(Superblock.getInstance().getInodeSize());
		buf.rewind();
		return buf;
	}
	
	public void write() throws IoError {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);
	}
	
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_UNKNOWN;
	}
	
	public int hashCode() {
	    return new HashCodeBuilder()
	        .appendSuper(super.hashCode())
	        .append(mode)
	        .append(getGid())
	        .append(getUid())
	        .append(size)
	        .append(accessTime)
	        .append(changeTime)
	        .append(modificationTime)
	        .append(deletionTime)
	        .append(linksCount)
	        .append(flags)
	        .append(generation)
	        .append(fileAcl)
	        .append(dirAcl)
	        .append(fragmentAddress)
	        .append(blockGroup)
	        .append(ino).toHashCode();
	}
}




