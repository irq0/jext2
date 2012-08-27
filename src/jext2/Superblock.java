/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.Date;

import jext2.exceptions.IoError;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Superblock extends Block {
	private static Superblock instance;

	private long inodesCount;
	private long blocksCount;
	private long reservedBlocksCount;
	private long freeBlocksCount;
	private long freeInodesCount;
	private long firstDataBlock;
	private long logBlockSize;
	private long logFragSize;
	private long blocksPerGroup;
	private long fragsPerGroup;
	private long inodesPerGroup;
	private Date lastMount;
	private Date lastWrite;
	private int mountCount;
	private int maxMountCount;
	private int magic;
	private int state;
	private int errors;
	private int minorRevLevel;
	private Date lastCheck;
	private long checkInterval;
	private long creatorOs;
	private long revLevel;
	private int defaultResuid;
	private int defaultResgid;
	private long firstIno;
	private int inodeSize;
	private int blockGroupNr;
	private long featuresCompat;
	private long featuresIncompat;
	private long featuresRoCompat;
	private UUID uuid;
	private String volumeName;
	private String lastMounted;

	private int blocksize;
	private int groupDescrPerBlock;
	private int groupsCount;
	private long dirsCount;
	private int groupDescrBlocks;
	private int inodeTableBlocksPerGroup;
	private int inodesPerBlock;

	private int overhead = 0;

	public final long getInodesCount() {
		return inodesCount;
	}
	public final long getBlocksCount() {
		return blocksCount;
	}
	public final long getReservedBlocksCount() {
		return reservedBlocksCount;
	}
	public final long getFreeBlocksCount() {
		return freeBlocksCount;
	}
	public final long getFreeInodesCount() {
		return freeInodesCount;
	}
	public final long getFirstDataBlock() {
		return firstDataBlock;
	}
	public final long getLogBlockSize() {
		return logBlockSize;
	}
	public final long getLogFragSize() {
		return logFragSize;
	}
	public final long getBlocksPerGroup() {
		return blocksPerGroup;
	}
	public final long getFragsPerGroup() {
		return fragsPerGroup;
	}
	public final long getInodesPerGroup() {
		return inodesPerGroup;
	}
	public final Date getLastMount() {
		return lastMount;
	}
	public final Date getLastWrite() {
		return lastWrite;
	}
	public final int getMountCount() {
		return mountCount;
	}
	public final int getMaxMountCount() {
		return maxMountCount;
	}
	public final int getMagic() {
		return magic;
	}
	public final int getState() {
		return state;
	}
	public final int getErrors() {
		return errors;
	}
	public final int getMinorRevLevel() {
		return minorRevLevel;
	}
	public final Date getLastCheck() {
		return lastCheck;
	}
	public final long getCheckInterval() {
		return checkInterval;
	}
	public final long getCreatorOs() {
		return creatorOs;
	}
	public final long getRevLevel() {
		return revLevel;
	}
	public final int getDefaultResuid() {
		return defaultResuid;
	}
	public final int getDefaultResgid() {
		return defaultResgid;
	}
	public final long getFirstIno() {
		return firstIno;
	}
	public final int getInodeSize() {
		return inodeSize;
	}
	public final int getBlockGroupNr() {
		return blockGroupNr;
	}
	public final long getFeaturesCompat() {
		return featuresCompat;
	}
	public final long getFeaturesIncompat() {
		return featuresIncompat;
	}
	public final long getFeaturesRoCompat() {
		return featuresRoCompat;
	}
	public final UUID getUuid() {
		return uuid;
	}
	public final String getVolumeName() {
		return volumeName;
	}
	public final String getLastMounted() {
		return lastMounted;
	}
	public final int getBlocksize() {
		return blocksize;
	}
	public final int getGroupDescrPerBlock() {
		return groupDescrPerBlock;
	}
	public final int getGroupDescrBlocks() {
		return groupDescrBlocks;
	}
	public final int getGroupsCount() {
		return groupsCount;
	}
	public final long getDirsCount() {
		return dirsCount;
	}
	public final int getInodeTableBlocksPerGroup() {
		return inodeTableBlocksPerGroup;
	}
	public final int getInodesPerBlock() {
		return inodesPerBlock;
	}
	/**
	 * Return number of blocks used by fs structures
	 */
	 public int getOverhead() {
		if (this.overhead == 0)
			this.overhead = calculateOverhead();
		return this.overhead;
	}
	public void setFreeBlocksCount(long freeBlocksCount) {
		this.freeBlocksCount = freeBlocksCount;
	}
	public void setFreeInodesCount(long freeInodesCount) {
		this.freeInodesCount = freeInodesCount;
	}
	public void setDirsCount(long dirsCount) {
		this.dirsCount = dirsCount;
	}

	public int getAddressesPerBlock() {
		return this.blocksize / 4;
	}

	public int getAddressesPerBlockBits() {
		return 31 - Integer.numberOfLeadingZeros(getAddressesPerBlock());
	}

	public final void setMountCount(int mountCount) {
		this.mountCount = mountCount;
	}
	public final void setMaxMountCount(int maxMountCount) {
		this.maxMountCount = maxMountCount;
	}
	public final void setLastMount(Date lastMount) {
		this.lastMount = lastMount;
	}
	public final void setLastWrite(Date lastWrite) {
		this.lastWrite = lastWrite;
	}
	public final void setLastMounted(String lastMounted) {
		this.lastMounted = lastMounted;
	}
	public boolean hasFreeBlocks() {
		long freeBlocks = this.freeBlocksCount;
		long rootBlocks = this.reservedBlocksCount;

		if (freeBlocks < rootBlocks + 1) {
			// TODO support reserve blocks
			return false;
		}

		return true;
	}

	@Override
	protected void read(ByteBuffer buf) throws IoError {
		this.inodesCount = Ext2fsDataTypes.getLE32U(buf, 0);
		this.blocksCount = Ext2fsDataTypes.getLE32U(buf, 4);
		this.reservedBlocksCount = Ext2fsDataTypes.getLE32U(buf, 8);
		this.freeBlocksCount = Ext2fsDataTypes.getLE32U(buf, 12);
		this.freeInodesCount = Ext2fsDataTypes.getLE32U(buf, 16);
		this.firstDataBlock = Ext2fsDataTypes.getLE32U(buf, 20);
		this.logBlockSize = Ext2fsDataTypes.getLE32U(buf, 24);
		this.logFragSize = Ext2fsDataTypes.getLE32U(buf, 28);
		this.blocksPerGroup = Ext2fsDataTypes.getLE32U(buf, 32);
		this.fragsPerGroup = Ext2fsDataTypes.getLE32U(buf, 36);
		this.inodesPerGroup = Ext2fsDataTypes.getLE32U(buf, 40);
		this.lastMount = Ext2fsDataTypes.getDate(buf, 44);
		this.lastWrite = Ext2fsDataTypes.getDate(buf, 48);
		this.mountCount = Ext2fsDataTypes.getLE16U(buf, 52);
		this.maxMountCount = Ext2fsDataTypes.getLE16U(buf, 54);
		this.magic = Ext2fsDataTypes.getLE16U(buf, 56);
		this.state = Ext2fsDataTypes.getLE16U(buf, 58);
		this.errors = Ext2fsDataTypes.getLE16U(buf, 60);
		this.minorRevLevel = Ext2fsDataTypes.getLE16U(buf, 62);
		this.lastCheck = Ext2fsDataTypes.getDate(buf, 64);
		this.checkInterval = Ext2fsDataTypes.getLE32U(buf, 68);
		this.creatorOs = Ext2fsDataTypes.getLE32U(buf, 72);
		this.revLevel = Ext2fsDataTypes.getLE32U(buf, 76);
		this.defaultResuid = Ext2fsDataTypes.getLE16U(buf, 80);
		this.defaultResgid = Ext2fsDataTypes.getLE16U(buf, 82);

		this.firstIno = Ext2fsDataTypes.getLE32U(buf, 84);
		this.inodeSize = Ext2fsDataTypes.getLE16U(buf, 88);
		this.blockGroupNr = Ext2fsDataTypes.getLE16U(buf, 90);
		this.featuresCompat = Ext2fsDataTypes.getLE32U(buf, 92);
		this.featuresIncompat = Ext2fsDataTypes.getLE32U(buf, 96);
		this.featuresRoCompat = Ext2fsDataTypes.getLE32U(buf, 100);
		this.uuid = Ext2fsDataTypes.getUUID(buf, 104);
		this.volumeName = Ext2fsDataTypes.getString(buf, 120, 16);
		this.lastMounted = Ext2fsDataTypes.getString(buf, 136, 64);

		this.blocksize = (1024 << this.logBlockSize);
		this.groupDescrPerBlock = this.blocksize / 32;
		this.groupsCount = (int)( ((this.blocksCount - this.firstDataBlock) - 1) /
				this.blocksPerGroup + 1);
		this.groupDescrBlocks = (this.groupsCount + this.groupDescrPerBlock -1) /
				this.groupDescrPerBlock;
		this.inodesPerBlock = this.blocksize / this.inodeSize;
		this.inodeTableBlocksPerGroup = (int)(this.inodesPerGroup) / this.inodesPerBlock;
	}


	@Override
	protected void write(ByteBuffer buf) throws IoError {
		Ext2fsDataTypes.putLE32U(buf, this.inodesCount, 0);
		Ext2fsDataTypes.putLE32U(buf, this.blocksCount, 4);
		Ext2fsDataTypes.putLE32U(buf, this.reservedBlocksCount, 8);
		Ext2fsDataTypes.putLE32U(buf, this.freeBlocksCount, 12);
		Ext2fsDataTypes.putLE32U(buf, this.freeInodesCount, 16);
		Ext2fsDataTypes.putLE32U(buf, this.firstDataBlock, 20);
		Ext2fsDataTypes.putLE32U(buf, this.logBlockSize, 24);
		Ext2fsDataTypes.putLE32U(buf, this.logFragSize, 28);
		Ext2fsDataTypes.putLE32U(buf, this.blocksPerGroup, 32);
		Ext2fsDataTypes.putLE32U(buf, this.fragsPerGroup, 36);
		Ext2fsDataTypes.putLE32U(buf, this.inodesPerGroup, 40);
		Ext2fsDataTypes.putDate(buf, this.lastMount, 44);
		Ext2fsDataTypes.putDate(buf, this.lastWrite, 48);
		Ext2fsDataTypes.putLE16U(buf, this.mountCount, 52);
		Ext2fsDataTypes.putLE16U(buf, this.maxMountCount, 54);
		Ext2fsDataTypes.putLE16U(buf, this.magic, 56);
		Ext2fsDataTypes.putLE16U(buf, this.state, 58);
		Ext2fsDataTypes.putLE16U(buf, this.errors, 60);
		Ext2fsDataTypes.putLE16U(buf, this.minorRevLevel, 62);
		Ext2fsDataTypes.putDate(buf, this.lastCheck, 64);
		Ext2fsDataTypes.putLE32U(buf, this.checkInterval, 68);
		Ext2fsDataTypes.putLE32U(buf, this.creatorOs, 72);
		Ext2fsDataTypes.putLE32U(buf, this.revLevel, 76);
		Ext2fsDataTypes.putLE16U(buf, this.defaultResuid, 80);
		Ext2fsDataTypes.putLE16U(buf, this.defaultResgid, 80);

		Ext2fsDataTypes.putLE32U(buf, this.firstIno, 84);
		Ext2fsDataTypes.putLE16U(buf, this.inodeSize, 88);
		Ext2fsDataTypes.putLE16U(buf, this.blockGroupNr, 90);
		Ext2fsDataTypes.putLE32U(buf, this.featuresCompat, 92);
		Ext2fsDataTypes.putLE32U(buf, this.featuresIncompat, 96);
		Ext2fsDataTypes.putLE32U(buf, this.featuresRoCompat, 100);
		Ext2fsDataTypes.putUUID(buf, this.uuid, 104);
		Ext2fsDataTypes.putString(buf, this.volumeName, 16, 120);
		Ext2fsDataTypes.putString(buf, this.lastMounted, 64, 136);

		super.write(buf);
	}

	// TODO add debug code that checks bitmap
	public long countFreeInodes() {
		long inodes = 0;
		for (BlockGroupDescriptor descr :
			BlockGroupAccess.getInstance().iterateBlockGroups()) {
			inodes += descr.getFreeInodesCount();
		}
		return inodes;
	}

	// TODO add debug code that checks bitmap
	public long countFreeBlocks() {
		long blocks = 0;
		for (BlockGroupDescriptor descr :
			BlockGroupAccess.getInstance().iterateBlockGroups()) {
			blocks += descr.getFreeBlocksCount();
		}
		return blocks;
	}


	/**
	 * Calculate the overhead (eg. blocks that are in use by fs structures
	 */
	private int calculateOverhead() {
		int overhead = (int)getFirstDataBlock();

		for (BlockGroupDescriptor descr :
			BlockGroupAccess.getInstance().iterateBlockGroups()) {
			overhead += descr.getOverhead();
		}

		return overhead;
	}


	@Override
	public void write() throws IoError {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);
	}

	private ByteBuffer allocateByteBuffer() {
		ByteBuffer buf = ByteBuffer.allocate(this.blocksize);
		buf.rewind();
		return buf;
	}

	public static Superblock getInstance() {
		return Superblock.instance;

	}

	protected Superblock(long blockNr) {
		super(blockNr);
	}

	/**
	 * Read superblock using a BlockAccess Object. Use this for filesystem
	 * initialization.
	 */
	public static Superblock fromBlockAccess(BlockAccess blocks) throws IoError {
		Superblock sb = new Superblock(1);
		ByteBuffer buf = blocks.read(1);
		sb.read(buf);

		Superblock.instance = sb;
		return sb;
	}

	/**
	 * Read superblock using a FileChannel. This is intended for testing and code
	 * that just needs the superblock not any file system access.
	 */
	public static Superblock fromFileChannel(FileChannel chan) throws IoError {

		Superblock sb = new Superblock(-1);
		ByteBuffer buf = ByteBuffer.allocate(Constants.EXT2_MIN_BLOCK_SIZE);
		try {
			chan.position(1024);
			chan.read(buf);
		} catch (IOException e) {
			throw new IoError();
		}
		sb.read(buf);

		Superblock.instance = sb;
		return sb;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
		.appendSuper(super.hashCode())
		.append(inodesCount)
		.append(blocksCount)
		.append(freeBlocksCount)
		.append(freeInodesCount)
		.append(firstDataBlock)
		.append(dirsCount)
		.append(uuid.getLeastSignificantBits())
		.append(uuid.getMostSignificantBits())
		.toHashCode();
	}
}
