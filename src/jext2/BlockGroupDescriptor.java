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

import java.nio.ByteBuffer;

import jext2.exceptions.IoError;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class BlockGroupDescriptor extends PartialBlock {
	private static Superblock superblock = Superblock.getInstance();

	private long blockBitmap;
	private long inodeBitmap;
	private long inodeTable;


	private int freeBlocksCount;
	private int freeInodesCount;
	private int usedDirsCount;

	private int blockGroup = -1;

	public final long getBlockBitmapPointer() {
		return this.blockBitmap;
	}
	public final long getInodeBitmapPointer() {
		return this.inodeBitmap;
	}
	public final long getInodeTablePointer() {
		return this.inodeTable;
	}
	public final int getFreeBlocksCount() {
		return this.freeBlocksCount;
	}
	public final int getFreeInodesCount() {
		return this.freeInodesCount;
	}
	public final int getUsedDirsCount() {
		return this.usedDirsCount;
	}
	public final int getBlockGroup() {
		return this.blockGroup;
	}
	void setBlockGroup(int blockGroup) {
		this.blockGroup = blockGroup;
	}
	public void setFreeBlocksCount(int freeBlocksCount) {
		this.freeBlocksCount = freeBlocksCount;
	}
	public void setFreeInodesCount(int freeInodesCount) {
		this.freeInodesCount = freeInodesCount;
	}
	public void setUsedDirsCount(int usedDirsCount) {
		this.usedDirsCount = usedDirsCount;
	}
	public final void setBlockBitmap(long blockBitmap) {
		this.blockBitmap = blockBitmap;
	}
	public final void setInodeBitmap(long inodeBitmap) {
		this.inodeBitmap = inodeBitmap;
	}
	public final void setInodeTable(long inodeTable) {
		this.inodeTable = inodeTable;
	}

	@Override
	protected void read(ByteBuffer buf) throws IoError {
		this.blockBitmap = Ext2fsDataTypes.getLE32U(buf, 0 + offset);
		this.inodeBitmap = Ext2fsDataTypes.getLE32U(buf, 4 + offset);
		this.inodeTable = Ext2fsDataTypes.getLE32U(buf, 8 + offset);
		this.freeBlocksCount = Ext2fsDataTypes.getLE16U(buf, 12 + offset);
		this.freeInodesCount = Ext2fsDataTypes.getLE16U(buf, 14 + offset);
		this.usedDirsCount = Ext2fsDataTypes.getLE16U(buf, 16 + offset);
	}

	@Override
	protected void write(ByteBuffer buf) throws IoError {
		Ext2fsDataTypes.putLE32U(buf, this.blockBitmap, 0);
		Ext2fsDataTypes.putLE32U(buf, this.inodeBitmap, 4);
		Ext2fsDataTypes.putLE32U(buf, this.inodeTable, 8);
		Ext2fsDataTypes.putLE16U(buf, this.freeBlocksCount, 12);
		Ext2fsDataTypes.putLE16U(buf, this.freeInodesCount, 14);
		Ext2fsDataTypes.putLE16U(buf, this.usedDirsCount, 16);
		super.write(buf);
	}

	@Override
	public void write() throws IoError {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);
	}

	protected ByteBuffer allocateByteBuffer() {
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.rewind();
		return buf;
	}

	protected BlockGroupDescriptor(long blockNr, int offset) {
		super(blockNr, offset);
	}

	public static BlockGroupDescriptor fromByteBuffer(ByteBuffer buf, long blockNr, int offset) {
		BlockGroupDescriptor b = new BlockGroupDescriptor(blockNr, offset);
		try {
			b.read(buf);
			return b;
		} catch (IoError e) {
			throw new RuntimeException("IOException in BlockGroupDescriptor->fromByteBuffer");
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}


	/**
	 * Check if block number is a valid data block in this block group
	 */
	public boolean isValidDataBlockNr(long nr) {
		return ( nr > 0 &&
				getBlockBitmapPointer() != nr &&
				getInodeBitmapPointer() != nr &&
				nr >= firstBlock() &&
				(!(getInodeTablePointer() <= nr &&
				nr < getInodeTablePointer()+superblock.getInodeTableBlocksPerGroup())));
	}

	/**
	 * Return the number of blocks used for file system structures
	 */
	public int getOverhead() {
		return (((hasSuperblock())?1:0) +
				getDescriptorTableBlocks() +
				superblock.getInodeTableBlocksPerGroup() +
				2 /* inode, block bitmap */ );
	}

	/**
	 * do not use this method. it is just for later improvement here. It does
	 * not take into account that sparse block groups have not descriptor table
	 */
	public static long descriptorLocation(int group) {
		int hasSuper = hasSuperblock(group) ? 1 : 0;

		return firstBlock(group) + hasSuper;
	}

	/**
	 * Return first block number in group
	 */
	public static long firstBlock(int group) {
		return superblock.getFirstDataBlock() +
				(group * superblock.getBlocksPerGroup());
	}

	/**
	 * Return first block number in this group
	 */
	public long firstBlock() {
		return firstBlock(this.blockGroup);
	}

	/**
	 * Return true if group contains a descriptor table (eg. is not sparse)
	 */
	public static boolean hasDescriptorTable(int group) {
		return hasSuperblock(group);
	}

	public boolean hasDescrptiorTable() {
		return hasDescriptorTable(this.blockGroup);
	}


	/**
	 * Return the number of blocks used for the descriptor table.
	 * Depends on the sparse_super feature
	 */
	public int getDescriptorTableBlocks() {
		if (hasDescrptiorTable()) {
			return superblock.getGroupDescrBlocks();
		} else {
			return 0;
		}
	}

	/**
	 * Check if group contains a superblock. Depends on the sparse super
	 * feature.
	 */
	public static boolean hasSuperblock(int group) {
		if (Feature.sparseSuper()) {
			return isSparse(group);
		} else {
			return true;
		}
	}

	public boolean hasSuperblock() {
		return hasSuperblock(this.blockGroup);
	}

	private static boolean isSparse(int group) {
		return ((group <= 1) ||
				test_root(group, 3) ||
				test_root(group, 5) ||
				test_root(group, 7));
	}

	private static boolean test_root(int a, int b) {
		long num = b;

		while (a > num)
			num *= b;
		return num == a;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
		.appendSuper(super.hashCode())
		.append(blockBitmap)
		.append(inodeBitmap)
		.append(inodeTable)
		.append(freeBlocksCount)
		.append(freeInodesCount)
		.append(usedDirsCount)
		.append(blockGroup)
		.toHashCode();
	}
}
