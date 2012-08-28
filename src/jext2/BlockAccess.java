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
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import jext2.annotations.NotThreadSafe;
import jext2.exceptions.IoError;

/**
 * Access to the filesystem blocks
 */
public class BlockAccess {
	private int blocksize = Constants.EXT2_MIN_BLOCK_SIZE;
	private FileChannel blockdev;
	private static BlockAccess instance;

	/** number of pointers in indirection block */
	private int ptrs;

	public BlockAccess(FileChannel blockdev) {
		if (BlockAccess.instance != null) {
			throw new RuntimeException("BlockAccess is singleton!");
		}
		this.blockdev = blockdev;
		BlockAccess.instance = this;
	}

	/** Read a block off size specified by setBlocksize() at logical address nr */
	public ByteBuffer read(long nr) throws IoError {
		ByteBuffer buf = ByteBuffer.allocate(blocksize);
		try {
			buf.order(ByteOrder.BIG_ENDIAN);
			blockdev.read(buf,((nr & 0xffffffff) * blocksize));
		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}

		return buf;
	}

	public void readToBuffer(long nr, long offsetInBlock, ByteBuffer buf) throws IoError {
		try {
			buf.order(ByteOrder.BIG_ENDIAN);
			blockdev.read(buf, ((nr & 0xffffffff) * blocksize) + offsetInBlock);
		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}
	}


	/**
	 * Read data from device to buffer starting at postion. To use this method set the
	 * limit and position of the buffer to your needs and note that position is
	 * not the block number. This method is indened for bulk data retrieval such as
	 * the inode.read()
	 *
	 * Big fat waring: Aquire a lock for the corresponding block or hell breaks loose...
	 */
	@NotThreadSafe(useLock=true)
	public void readToBufferUnsynchronized(long position, ByteBuffer buf) throws IoError {
		try {
			buf.order(ByteOrder.BIG_ENDIAN);
			blockdev.read(buf, position);
		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}
	}

	@NotThreadSafe(useLock=true)
	public void writeFromBufferUnsynchronized(long position, ByteBuffer buf) throws IoError {
		buf.order(ByteOrder.BIG_ENDIAN);
		try {
			blockdev.write(buf, position);
		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}
	}

	/**
	 * Zero out part of a block
	 * @throws IOException
	 */
	public void zeroOut(long nr, int start, int end) throws IoError {
		ByteBuffer zeros = ByteBuffer.allocate((end-start)+1);
		writePartial(nr, start, zeros);
	}

	/** Write a whole block to the logical address nr on disk */
	public void write(long nr, ByteBuffer buf) throws IoError {
		buf.rewind();
		try {
			blockdev.write(buf,(nr & 0xffffffff) * blocksize); 
		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}
	}

	public void dumpByteBuffer(ByteBuffer buf) {
		try {
			while (buf.hasRemaining()) {
				for (int i=0; i<8; i++) {
					System.out.print(buf.get());
					System.out.print("\t\t");
				}
				System.out.println();
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * Force changes to disc
	 */
	public void sync() throws IoError {
		try {
			blockdev.force(false);
		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}
	}


	/**
	 * Write partial buffer to a disk block. It is not possible to write over
	 * the blocksize boundry.
	 * @throws IOException
	 */
	public void writePartial(long nr, long offset, ByteBuffer buf) throws IoError {
		if (offset + buf.limit() > blocksize)
			throw new IllegalArgumentException("attempt to write over block boundries" + buf + ", " + offset);

		try {
			buf.rewind();
			blockdev.write(buf, (((nr & 0xffffffff) * blocksize) + offset));

		} catch (IOException e) {
			throw new IoError(e.getMessage());
		}
	}

	public void initialize(Superblock superblock) {
		blocksize = superblock.getBlocksize();
		ptrs = superblock.getAddressesPerBlock();
	}

	/**
	 * Read block pointers from block.
	 * Note: Zero pointers are not skipped
	 * @param   dataBlock   physical block number
	 * @param   start       index of first pointer to retrieve
	 * @param   end         index of last pointer to retrieve
	 * @return  array containing all block numbers in block
	 */
	public long[] readBlockNrsFromBlock(long dataBlock, int start, int end) throws IoError {
		int numEntries = (end-start)+1;
		long[] result = new long[numEntries];
		ByteBuffer buffer = ByteBuffer.allocate(numEntries*4);

		try {
			readToBufferUnsynchronized(dataBlock*blocksize + start*4, buffer);
		} catch (IoError e) {
			throw e;
		}

		for (int i=0; i<numEntries; i++) {
			result[i] = Ext2fsDataTypes.getLE32U(buffer, i*4);
		}

		return result;
	}

	/**
	 * Read block pointers from block. Skip pointers that are zero.
	 * Note: Since we return a list, you cannot use indices computed
	 * for the block with the result.
	 * @param   dataBlock   physical block number
	 * @param   start       index of first pointer to retrieve
	 * @param   end         index of last pointer to retrieve
	 * @return  list of non-zero block numbers found in block
	 */
	public LinkedList<Long>
	readBlockNrsFromBlockSkipZeros(long dataBlock, int start, int end) throws IoError {
		int numEntries = (end-start)+1;
		LinkedList<Long> result = new LinkedList<Long>();
		ByteBuffer buffer = ByteBuffer.allocate(numEntries*4);

		try {
			readToBufferUnsynchronized(dataBlock*blocksize + start*4, buffer);
		} catch (IoError e) {
			throw e;
		}

		for (int i=0; i<numEntries; i++) {
			long tmp = Ext2fsDataTypes.getLE32U(buffer, i*4);
			if (tmp > 0)
				result.add(tmp);
		}

		return result;
	}

	/**
	 * Read all block pointers from block.
	 * @param   dataBlock   physical block number
	 * @return  array with all pointers stored in block
	 */
	public long[] readAllBlockNumbersFromBlock(long dataBlock) throws IoError {
		return readBlockNrsFromBlock(dataBlock, 0, ptrs-1);
	}

	/**
	 * Read all block pointers from block. Skip Zero pointers
	 * @param   dataBlock   physical block number
	 * @return  list with all non-zero pointers in block
	 */
	public LinkedList<Long>
	readAllBlockNumbersFromBlockSkipZero(long dataBlock) throws IoError {
		return readBlockNrsFromBlockSkipZeros(dataBlock, 0, ptrs-1);
	}

	/**
	 * Read single block pointer from block.
	 * @param   dataBlock   physical block number
	 * @param   index       index of block number to retrieve
	 */
	public long readBlockNumberFromBlock(long dataBlock, int index) throws IoError {
		return (readBlockNrsFromBlock(dataBlock, index, index))[0];
	}

	public static BlockAccess getInstance() {
		return BlockAccess.instance;
	}
}
