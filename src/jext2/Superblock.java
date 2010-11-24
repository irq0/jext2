/**
 * EXT2 Superblock class
 *
 * @author Marcel Lauhoff <ml@irq0.org>
 */

package jext2;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
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
		return (1024 << this.logBlockSize);
	}

	public final int getBlocksizeBits() {
		return (1024 << this.logBlockSize) * 8;
	}
	
	private Superblock() {
		
	}

	private void readSuperblock() throws IOException{		
		this.inodesCount = getLE32(0);
		this.blocksCount = getLE32(4);
		this.resevedBlocksCount = getLE32(8);
		this.freeBlocksCount = getLE32(12);
		this.freeInodesCount = getLE32(16);
		this.firstDataBlock = getLE32(20);
		this.logBlockSize = getLE32(24);
		this.logFragSize = getLE32(28);
		this.blocksPerGroup = getLE32(32);
		this.fragsPerGroup = getLE32(36);
		this.inodesPerGroup = getLE32(40);
		this.lastMount = getDate(44);
		this.lastWrite = getDate(48);
		this.mountCount = getLE16(52);
		this.maxMountCount = getLE16(54);
		this.magic = getLE16(56);
		this.state = getLE16(58);
		this.errors = getLE16(60);
		this.minorRevLevel = getLE16(62);
		this.lastCheck = getDate(64);
		this.checkInterval = getLE32(68);
		this.creatorOs = getLE32(72);
		this.revLevel = getLE32(76);
		this.defaultResuid = getLE16(80);
		this.defaultResgid = getLE16(82);

		this.firstIno = getLE32(84);
		this.inodeSize = getLE16(88);
		this.blockGroupNr = getLE16(90);
		this.featuresCompat = getLE32(92);
		this.featuresIncompat = getLE32(96);
		this.featuresRoCompat = getLE32(100);
		this.uuid = getUUID(104);
		this.volumeName = getString(120, 16);
		this.lastMounted = getString(136, 64);	   		
	}


	public static Superblock getInstance() {
		return Superblock.instance;
	}
	
	public static Superblock fromBlockAccess(BlockAccess blocks) throws IOException {
		Superblock sb = new Superblock();
		ByteBuffer rawBlock = blocks.getAtOffset(1);
		sb.buffer = rawBlock;
		sb.readSuperblock();

		Superblock.instance = sb;
		
		return sb;
	}		
	
	public static Superblock fromFileChannel(FileChannel chan) throws IOException {
		
		Superblock sb = new Superblock();
		ByteBuffer buf = ByteBuffer.allocate(Constants.EXT2_MIN_BLOCK_SIZE);
		chan.position(1024);
		chan.read(buf);

		sb.buffer = buf;
		sb.readSuperblock();

	    Superblock.instance = sb;
		
		return sb;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
}




