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
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.commons.lang.builder.HashCodeBuilder;

import jext2.exceptions.FileTooLarge;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NoSpaceLeftOnDevice;

/**
 * Base class for inodes with data blocks. Like Symlinks, Directories, Regular Files
 */
public class DataInode extends Inode {
	Superblock superblock = Superblock.getInstance();
	BlockAccess blockAccess = BlockAccess.getInstance();
	DataBlockAccess dataAccess = null;

	private long[] block;
	private long blocks = 0;

	public final long getBlocks() {
		return this.blocks;
	}
	public final long[] getBlock() {
		return this.block;
	}
	public final void setBlocks(long blocks) {
		this.blocks = blocks;
	}
	public final void setBlock(long[] block) {
		this.block = block;
	}

	@Override
	public boolean hasDataBlocks() {
		return getBlocks() > 0;
	}

	/**
	 * Get the data access provider to read and write to the data area of this
	 * inode
	 */
	public DataBlockAccess accessData() {
		if (dataAccess == null)
			dataAccess = DataBlockAccess.fromInode(this);
		return dataAccess;
	}

	/**
	 * Read Inode data
	 * @param  size    size of the data to be read
	 * @param  offset  start address in data area
	 * @return buffer of size size containing data.
	 * @throws FileTooLarge
	 * @throws IoError
	 */
	public ByteBuffer readData(int size, long fileOffset) throws JExt2Exception, FileTooLarge {
		/* Returning null may break things somewhere..
		 * Zero length buffer breaks something in jlowfuse's c code */
		if (getSize() == 0)
			return ByteBuffer.allocateDirect(1);

		/*
		 * size may be larger than the inode.size, it doesn't make sense to return
		 * 4k of zeros
		 */
		if (size > getSize())
			size = (int)getSize();


		ByteBuffer buf = ByteBuffer.allocateDirect(size);

		int blocksize = superblock.getBlocksize();

		long i = 0;
		long firstBlock = fileOffset / blocksize;
		long offset = fileOffset % blocksize;

		/*
		 * just as size may be larger than the inode's data, the number of blocks
		 * may also be.
		 */
		long approxBlocks = (size / blocksize) + 1;
		long maxBlocks = this.getBlocks()/(superblock.getBlocksize()/512);
		if (approxBlocks > maxBlocks)
			approxBlocks = maxBlocks;

		while (i < approxBlocks) {
			long start = firstBlock + i;
			long stop = firstBlock + approxBlocks;

			LinkedList<Long> b = accessData().getBlocks(start, stop);
			int blocksRead;

			/*
			 * Note on the sparse file support:
			 * getBlocks will return null if there is no data block for this
			 * logical address. So just move the position count blocks forward.
			 */

			if (b == null) { /* hole */
				blocksRead = 1;

				int unboundedLimit = buf.position() + blocksize;
				int limit = Math.min(unboundedLimit, buf.capacity());

				assert limit <= buf.capacity() :
					"New position, limit " + limit + " is beyond buffer's capacity, " + buf;

				buf.limit(limit);
				buf.position(limit);

				assert buf.limit() == buf.position();

			} else { /* blocks */
				blocksRead = b.size();

				long pos = b.getFirst() * blocksize + offset;
				int unboundedLimit = buf.position() + blocksRead * blocksize;
				int limit = Math.min(unboundedLimit, buf.capacity());

				assert limit <= buf.capacity() :
					"New limit " + limit + " is beyond buffer's capacity, " + buf;

				buf.limit(limit);
				blockAccess.readToBufferUnsynchronized(pos, buf);
			}

			i += blocksRead;
			offset = 0;

			/* This should be removed soon. IllegalMonitorStateException happen
			 * occasionally for unknown reasons.
			 */
			try {
				accessData().getHierarchyLock().readLock().unlock();
			} catch (IllegalMonitorStateException e) {
				Logger log = Filesystem.getLogger();
				log.warning("IllegalMonitorStateException encountered in readData, inode=" + this);
				log.warning(String.format("context for exception: blocks=%s i=%d approxBlocks=%d off=%d buf=%s readlock=%s lock.readlock.holds=%s",
													b, i, approxBlocks, fileOffset, buf, accessData().getHierarchyLock(), accessData().getHierarchyLock().getReadHoldCount()));
			}

			if (buf.capacity() == buf.limit())
				break;
		}

		assert buf.position() == buf.limit() : "Buffer wasn't filled completely";
		assert buf.limit() == size : "Read buffer size does not match request size";

		if (buf.limit() > getSize())
			buf.limit((int)getSize());


		buf.rewind();
		return buf;
	}

	/**
	 * Write data in buffer to disk. This works best when whole blocks which
	 * are a multiple of blocksize in size are written. Partial blocks are
	 * written by first reading the block and then writing the new data
	 * to that buffer than write that new buffer to disk.
	 * @throws NoSpaceLeftOnDevice
	 * @throws FileTooLarge
	 */
	public int writeData(ByteBuffer buf, long offset) throws JExt2Exception, NoSpaceLeftOnDevice, FileTooLarge {
		/*
		 * Note on sparse file support:
		 * getBlocksAllocate does not care if there are holes. Just write as much
		 * blocks as the buffer requires at the desired location an set inode.size
		 * accordingly.
		 */

		int blocksize = superblock.getBlocksize();
		long start = offset/blocksize;
		long end = (buf.capacity()+blocksize)/blocksize + start;
		int startOff = (int)(offset%blocksize);

		if (startOff > 0)
			end += 1;

		buf.rewind();

		while (start < end) {
			LinkedList<Long> blockNrs = accessData().getBlocksAllocate(start, 1);
			int bytesLeft = buf.capacity() - buf.position();

			if (bytesLeft < blocksize || startOff > 0) { /* write partial block */
				ByteBuffer onDisk = blockAccess.read(blockNrs.getFirst());

				onDisk.position(startOff);

				assert onDisk.limit() == blocksize;

				buf.limit(buf.position() + Math.min(bytesLeft,
													onDisk.remaining()));

				onDisk.put(buf);

				onDisk.position(startOff);
				blockAccess.writeFromBufferUnsynchronized((blockNrs.getFirst() & 0xffffffff) * blocksize, onDisk);
			} else { /* write whole block */
				buf.limit(buf.position() + blocksize);

				blockAccess.writeFromBufferUnsynchronized(
						(blockNrs.getFirst() & 0xffffffff) * blocksize, buf);
			}

			start += 1;
			startOff = 0;
			accessData().unlockHierarchyChanges();

		}
		int written = buf.position();
		assert written == buf.capacity();

		/* increase inode.size if we grew the file */
		if (offset + written > getSize()) { /* file grew */
			setStatusChangeTime(new Date());
			setSize(offset + written);
		}

		return written;
	}

	protected DataInode(long blockNr, int offset) {
		super(blockNr, offset);
	}

	@Override
	protected void read(ByteBuffer buf) throws IoError {
		super.read(buf);
		this.blocks = Ext2fsDataTypes.getLE32U(buf, 28 + offset);

		if (!isFastSymlink()) {
			this.block = new long[Constants.EXT2_N_BLOCKS];
			for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
				this.block[i] = Ext2fsDataTypes.getLE32U(buf, 40 + (i*4) + offset);
			}
		}
	}


	@Override
	protected void write(ByteBuffer buf) throws IoError {
		if (!isFastSymlink()) {
			for (int i=0; i<Constants.EXT2_N_BLOCKS; i++) {
				Ext2fsDataTypes.putLE32U(buf, this.block[i], 40 + (i*4));
			}
		}
		Ext2fsDataTypes.putLE32U(buf, this.blocks, 28);
		super.write(buf);
	}

	@Override
	public void write() throws IoError {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
		.appendSuper(super.hashCode())
		.append(blocks)
		.append(block)
		.toHashCode();
	}

}
