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

import static org.junit.Assert.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import jext2.*;

import org.junit.*;


public class BitmapTest {
	Bitmap bmap;
	String filename = "tests" + File.separator + "data" + File.separator + "ext2fs";

	private void setupSuperblock() throws Exception {
		RandomAccessFile blockDevFile = new RandomAccessFile(filename, "rw");
		FileChannel blockDev = blockDevFile.getChannel();

		Superblock.fromFileChannel(blockDev);
	}

	private void setupBitmap() throws Exception {

        byte[] sequence = new byte[] { 23, 42, 1, 5, 3, 9, 7, 12 };

        ByteBuffer buf = ByteBuffer.allocate(128);

        buf.rewind();

        for (int i=0; i< buf.limit()/sequence.length; i++)
            buf.put(sequence);

        buf.rewind();
        bmap = Bitmap.fromByteBuffer(buf, -1);
  	}

	@Before
	public void setup() throws Exception {
		setupSuperblock();
		setupBitmap();
	}

	@Test
	public void setBitIsSet() {
		bmap.setBit(42, true);
		assertTrue(bmap.isSet(42));

		bmap.setBit(42, false);
		assertFalse(bmap.isSet(42));


		bmap.setBit(23, true);
		assertTrue(bmap.isSet(23));

		bmap.setBit(23, true);
		assertTrue(bmap.isSet(23));

		bmap.setBit(127, false);
		assertFalse(bmap.isSet(127));
	}

	@Test
	public void getNextZeroBitPos() {
		for (int i=0; i<23; i++) {
			bmap.setBit(i, true);
		}
		bmap.setBit(23, false);

		assertEquals(bmap.getNextZeroBitPos(0), 23);
		assertEquals(bmap.getNextZeroBitPos(0, 1), -1);
	}

	@Test
	public void findRightModeZeroBitInByte() {
		assertEquals(Bitmap.findRightModeZeroBitInByte((byte)(Integer.parseInt("00111111", 2))), 6);
		assertEquals(Bitmap.findRightModeZeroBitInByte((byte)(Integer.parseInt("10101010", 2))), 0);
		assertEquals(Bitmap.findRightModeZeroBitInByte((byte)(Integer.parseInt("11000011", 2))), 2);
		assertEquals(Bitmap.findRightModeZeroBitInByte((byte)(Integer.parseInt("00000000", 2))), 0);
		assertEquals(Bitmap.findRightModeZeroBitInByte((byte)(Integer.parseInt("11111111", 2))), -1);
	}

	@Test
	public void findLeftMostZeroBitInByte() {
		assertEquals(Bitmap.findLeftMostZeroBitInByte((byte)(Integer.parseInt("00111111", 2))), 0);
		assertEquals(Bitmap.findLeftMostZeroBitInByte((byte)(Integer.parseInt("10101010", 2))), 1);
		assertEquals(Bitmap.findLeftMostZeroBitInByte((byte)(Integer.parseInt("11000011", 2))), 2);
		assertEquals(Bitmap.findLeftMostZeroBitInByte((byte)(Integer.parseInt("00000000", 2))), 0);
		assertEquals(Bitmap.findLeftMostZeroBitInByte((byte)(Integer.parseInt("11111111", 2))), -1);
	}

}
