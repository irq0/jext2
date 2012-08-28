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
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReentrantLock;

import jext2.exceptions.IoError;

public class Bitmap extends Block {
	private boolean dirty = false;

	private ByteBuffer bmap = ByteBuffer.allocate(Superblock.getInstance().getBlocksize());
	private ReentrantLock bmapLock = new ReentrantLock();

	@Override
	protected void read(ByteBuffer buf) throws IoError {
		bmapLock.lock();
		this.bmap = buf;
		this.bmap.order(ByteOrder.LITTLE_ENDIAN);
		bmapLock.unlock();
	}

	/**
	 * Return bit position of next zero in bitmap. Search up to numBytes bytes including the
	 * byte in wich start is located. This means that a search starting at the last bit in one
	 * byte will just check a single bit.
	 */
	public int getNextZeroBitPos(int start, int numBytes) {
		bmapLock.lock();
		if ((bmap.limit() - start) == 0) {
			bmapLock.unlock();
			return -1;
		}

		int pos = -1;
		int byteNum = start / 8;
		byte offset = (byte) (start % 8);
		byte chunk;

		bmap.position(byteNum);

		/* start may not be byte aligned - mask first XX bits */
		chunk = bmap.get();

		chunk = (byte)((0xFF >> offset) ^ 0xFF | chunk);

		while(bmap.hasRemaining() && numBytes > 0) {
			if (chunk == 0) { /* is zero */

				pos = (bmap.position()-1) * 8;
				break;
			} else if (chunk != (byte)0xFF) { /* has at least one zero bit */
				pos = (bmap.position()-1)*8 + findRightModeZeroBitInByte(chunk);
				break;
			}

			chunk = bmap.get();
			numBytes--;
		}

		assert !isSet(pos);
		bmapLock.unlock();
		return pos;
	}

	public int getNextZeroBitPos(int start) {
		return getNextZeroBitPos(start, bmap.limit());
	}

	/**
	 * format a byte as bitstring as it appears on disk
	 */
	public static String formatByte(byte b) {
		return (new StringBuffer(String.format("%1$#32s",
				Integer.toBinaryString(b).replace(' ','0')).substring(24))).reverse().toString();
	}

	public String getBitStringContaining(int pos) {
		bmapLock.lock();
		byte b = bmap.get(pos/8);
		bmapLock.unlock();

		return Bitmap.formatByte(b);
	}


	/**
	 * test if bit at position pos is 1
	 */
	public boolean isSet(int pos) {
		int byteNum = pos / 8;
		byte offset = (byte) (pos % 8);

		bmapLock.lock();
		byte chunk = bmap.get(byteNum);
		bmapLock.unlock();

		byte mask = (byte)(1 << offset);

		return ((mask & chunk) != 0);
	}

	/**
	 * set bit to value
	 */
	public synchronized void setBit(int pos, boolean value) {
		int byteNum = pos / 8;
		byte offset = (byte) (pos % 8);
		bmapLock.lock();
		byte chunk = bmap.get(byteNum);

		if (value) // set to 1
			chunk = (byte) (chunk | (1 << offset));
		else  // set to 0
			chunk = (byte) (chunk & (0xFF ^ (1 << offset)));

		bmap.put(byteNum, chunk);
		bmapLock.unlock();

		if (value)
			assert isSet(pos);
		else
			assert !isSet(pos);

		this.dirty = true;
	}

	/**
	 * Find position of first zero bit from the left
	 * "00111111" -> 0
	 */
	public static int findLeftMostZeroBitInByte(byte b) {
		byte mask = -0x80; /* sign bit is 128 */
		for (int i=0; i<8; i++) {
			if ((mask & b) == 0) {
				return i;
			}
			mask = (byte) ((mask >>> 1) & ~mask);
		}
		return -1;
	}

	/**
	 * Find position of first zero bit from the right
	 * "00111111" -> 6
	 */
	public static int findRightModeZeroBitInByte(byte b) {
		byte mask = 0x01;
		for (int i=0; i<8; i++) {
			if ((mask & b) == 0) {
				return i;
			}
			mask = (byte) (mask << 1);
		}
		return -1;
	}


	protected Bitmap(long blockNr) {
		super(blockNr);
	}

	public static Bitmap fromByteBuffer(ByteBuffer buf, long blockNr) throws IoError {
		Bitmap bmap = new Bitmap(blockNr);
		bmap.read(buf);
		return bmap;
	}

	@Override
	public void write() throws IoError {
		bmapLock.lock();
		write(bmap);
		bmapLock.unlock();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass());
		sb.append("[\n");
		sb.append("  blockNr=");
		sb.append(getBlockNr());
		sb.append("\n");
		sb.append("  bitmap=\n");

		bmapLock.lock();
		bmap.rewind();
		for (int i=0; i<bmap.limit()/(Integer.SIZE/8); i++) {
			StringBuilder binstr = new StringBuilder();
			binstr.append(String.format("%1$#23s", (Integer.toBinaryString(bmap.getInt())).replace(' ','0')));
			sb.append(binstr.reverse());
			sb.append("\n");
		}
		bmapLock.unlock();

		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void cleanDirty() {
		this.dirty = false;
	}
}
