/**
 * EXT2 Superblock class
 *
 * @author Marcel Lauhoff <ml@irq0.org>
 */

package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Superblock extends Block {
	private static Superblock instance;
	
	private int inodesCount;
	private int blocksCount;
	private int resevedBlocksCount;
	private int freeBlocksCount;
	private int freeInodesCount;
	private int firstDataBlock;
	private int logBlockSize;
	private int logFragSize;
	private int blocksPerGroup;
	private int fragsPerGroup;
	private int inodesPerGroup;
	private Date lastMount;
	private Date lastWrite;
	private short mountCount;
	private short maxMountCount;
	private short magic;
	private short state;
	private short errors;
	private short minorRevLevel;
	private Date lastCheck;
	private int checkInterval;
	private int creatorOs;
	private int revLevel;
	private short defaultResuid;
	private short defaultResgid;
	private int firstIno;
	private short inodeSize;
	private short blockGroupNr;
	private int featuresCompat;
	private int featuresIncompat;
	private int featuresRoCompat;
	private UUID uuid;
	private String volumeName;
	private String lastMounted;
	
	private int blocksize;
	private int groupDescPerBlock;
	private int groupsCount;

	// sb_info fields
	private int dirsCount;
	
	public final int getInodesCount() {
		return this.inodesCount;
	}
	public final int getBlocksCount() {
		return this.blocksCount;
	}
	public final int getResevedBlocksCount() {
		return this.resevedBlocksCount;
	}
	public final int getFreeBlocksCount() {
		return this.freeBlocksCount;
	}
	public final int getFreeInodesCount() {
		return this.freeInodesCount;
	}
	public final int getFirstDataBlock() {
		return this.firstDataBlock;
	}
	public final int getLogBlockSize() {
		return this.logBlockSize;
	}
	public final int getLogFragSize() {
		return this.logFragSize;
	}
	public final int getBlocksPerGroup() {
		return this.blocksPerGroup;
	}
	public final int getFragsPerGroup() {
		return this.fragsPerGroup;
	}
	public final int getInodesPerGroup() {
		return this.inodesPerGroup;
	}
	public final Date getLastMount() {
		return this.lastMount;
	}
	public final Date getLastWrite() {
		return this.lastWrite;
	}
	public final short getMountCount() {
		return this.mountCount;
	}
	public final short getMaxMountCount() {
		return this.maxMountCount;
	}
	public final short getMagic() {
		return this.magic;
	}
	public final short getState() {
		return this.state;
	}
	public final short getErrors() {
		return this.errors;
	}
	public final short getMinorRevLevel() {
		return this.minorRevLevel;
	}
	public final Date getLastCheck() {
		return this.lastCheck;
	}
	public final int getCheckInterval() {
		return this.checkInterval;
	}
	public final int getCreatorOs() {
		return this.creatorOs;
	}
	public final int getRevLevel() {
		return this.revLevel;
	}
	public final short getDefaultResuid() {
		return this.defaultResuid;
	}
	public final short getDefaultResgid() {
		return this.defaultResgid;
	}
	public final int getFirstIno() {
		return this.firstIno;
	}
	public final short getInodeSize() {
		return this.inodeSize;
	}
	public final short getBlockGroupNr() {
		return this.blockGroupNr;
	}
	public final int getFeaturesCompat() {
		return this.featuresCompat;
	}
	public final int getFeaturesIncompat() {
		return this.featuresIncompat;
	}
	public final int getFeaturesRoCompat() {
		return this.featuresRoCompat;
	}
	public final UUID getUuid() {
		return this.uuid;
	}
	public final String getVolumeName() {
		return this.volumeName;
	}
	public final String getLastMounted() {
		return this.lastMounted;
	}
	public final int getBlocksize() {
		return this.blocksize;
	}
	public final int getBlocksizeBits() {
		return this.blocksize * 8;
	}
	public final int getGroupDescrPerBlock() {
		return this.groupDescPerBlock;
	}
	public final int getGroupsCount() {
		return this.groupsCount;
	}
	public int getDirsCount() {
		return this.dirsCount;
	}
	
	public void setFreeBlocksCount(int freeBlocksCount) {
		this.freeBlocksCount = freeBlocksCount;
	}
	public void setFreeInodesCount(int freeInodesCount) {
		this.freeInodesCount = freeInodesCount;
	}
	public void setDirsCount(int dirsCount) {
		this.dirsCount = dirsCount;
	}
	
	protected void read(ByteBuffer buf) throws IOException {		
		this.inodesCount = Ext2fsDataTypes.getLE32(buf, 0);
		this.blocksCount = Ext2fsDataTypes.getLE32(buf, 4);
		this.resevedBlocksCount = Ext2fsDataTypes.getLE32(buf, 8);
		this.freeBlocksCount = Ext2fsDataTypes.getLE32(buf, 12);
		this.freeInodesCount = Ext2fsDataTypes.getLE32(buf, 16);
		this.firstDataBlock = Ext2fsDataTypes.getLE32(buf, 20);
		this.logBlockSize = Ext2fsDataTypes.getLE32(buf, 24);
		this.logFragSize = Ext2fsDataTypes.getLE32(buf, 28);
		this.blocksPerGroup = Ext2fsDataTypes.getLE32(buf, 32);
		this.fragsPerGroup = Ext2fsDataTypes.getLE32(buf, 36);
		this.inodesPerGroup = Ext2fsDataTypes.getLE32(buf, 40);
		this.lastMount = Ext2fsDataTypes.getDate(buf, 44);
		this.lastWrite = Ext2fsDataTypes.getDate(buf, 48);
		this.mountCount = Ext2fsDataTypes.getLE16(buf, 52);
		this.maxMountCount = Ext2fsDataTypes.getLE16(buf, 54);
		this.magic = Ext2fsDataTypes.getLE16(buf, 56);
		this.state = Ext2fsDataTypes.getLE16(buf, 58);
		this.errors = Ext2fsDataTypes.getLE16(buf, 60);
		this.minorRevLevel = Ext2fsDataTypes.getLE16(buf, 62);
		this.lastCheck = Ext2fsDataTypes.getDate(buf, 64);
		this.checkInterval = Ext2fsDataTypes.getLE32(buf, 68);
		this.creatorOs = Ext2fsDataTypes.getLE32(buf, 72);
		this.revLevel = Ext2fsDataTypes.getLE32(buf, 76);
		this.defaultResuid = Ext2fsDataTypes.getLE16(buf, 80);
		this.defaultResgid = Ext2fsDataTypes.getLE16(buf, 82);

		this.firstIno = Ext2fsDataTypes.getLE32(buf, 84);
		this.inodeSize = Ext2fsDataTypes.getLE16(buf, 88);
		this.blockGroupNr = Ext2fsDataTypes.getLE16(buf, 90);
		this.featuresCompat = Ext2fsDataTypes.getLE32(buf, 92);
		this.featuresIncompat = Ext2fsDataTypes.getLE32(buf, 96);
		this.featuresRoCompat = Ext2fsDataTypes.getLE32(buf, 100);
		this.uuid = Ext2fsDataTypes.getUUID(buf, 104);
		this.volumeName = Ext2fsDataTypes.getString(buf, 120, 16);
		this.lastMounted = Ext2fsDataTypes.getString(buf, 136, 64);	   	
		
		this.blocksize = (1024 << this.logBlockSize);
		this.groupDescPerBlock = this.blocksize / 32; // 32 = sizeof (struct ext2_group_desc);
		this.groupsCount = ((this.blocksCount - this.firstDataBlock) - 1) /
						   this.blocksPerGroup;
	}


	public static Superblock getInstance() {
		return Superblock.instance;

	}
	
	protected Superblock(int blockNr, int offset) {
		super(blockNr, offset);
	}
	
	public static Superblock fromBlockAccess(BlockAccess blocks) throws IOException {
		Superblock sb = new Superblock(-1, -1);
		ByteBuffer buf = blocks.read(1);
		sb.read(buf);

		Superblock.instance = sb;		
		return sb;
	}		
	
	public static Superblock fromFileChannel(FileChannel chan) throws IOException {
		
		Superblock sb = new Superblock(-1, -1);
		ByteBuffer buf = ByteBuffer.allocate(Constants.EXT2_MIN_BLOCK_SIZE);
		chan.position(1024);
		chan.read(buf);
		sb.read(buf);
		
	    Superblock.instance = sb;		
		return sb;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
}




