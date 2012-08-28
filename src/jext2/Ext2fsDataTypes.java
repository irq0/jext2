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
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Date;
import java.util.UUID;

/**
 * Read/Write EXT2 related on disk data types to/from ByteBuffer
 *
 * There are signed and unsigned variants. The unsigned variant uses a type
 * larger than the real one because java lacks unsigned types.
 */

public class Ext2fsDataTypes {
	public static final int LE8_MAX = 255;
	public static final int LE8_MIN = 0;
	public static final int LE8_SIZE = 8;
	public static final int LE16_MAX = 65535; /* 2^16 - 1 */
	public static final int LE16_MIN = 0;
	public static final int LE16_SIZE = 16;
	public static final long LE32_MAX = 4294967295L; /* 2^32 - 1 */
	public static final int LE32_MIN = 0;
	public static final int LE32_SIZE = 32;


	public static Date getDate(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long unixTime = buffer.getInt(offset);
		return new Date(unixTime * 1000);
	}

	public static short getLE16(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.getShort(offset);
	}

	public static int getLE16U(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int result = buffer.getShort(offset) & 0xffff;
		assert result >= LE16_MIN && result <= LE16_MAX;
		return result;
	}

	public static int getLE32(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.getInt(offset);
	}

	public static long getLE32U(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long result = buffer.getInt(offset) & 0xffffffffL;
		assert result >= LE32_MIN && result <= LE32_MAX;
		return result;
	}

	public static byte getLE8(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.get(offset);
	}

	public static short getLE8U(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		short result = (short)(buffer.get(offset) & (short)0xff);
		assert result >= LE8_MIN && result <= LE8_MAX;
		return result;
	}

	public static String getString(ByteBuffer buffer, int offset, int len)  {
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		CharsetDecoder decoder = Filesystem.getCharset().newDecoder();
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		decoder.onMalformedInput(CodingErrorAction.REPLACE);

		buffer.limit(offset+len);
		buffer.position(offset);

		CharBuffer cbuf;
		try {
			cbuf = decoder.decode(buffer);
		} catch (CharacterCodingException e) {
			return "";
		}

		buffer.limit(buffer.capacity());

		return cbuf.toString();

	}

	public static UUID getUUID(ByteBuffer buffer, int offset) {
		buffer.order(ByteOrder.BIG_ENDIAN);
		long mostSig = buffer.getLong(offset);
		long leastSig = buffer.getLong(offset + 8);
		return new UUID(mostSig, leastSig);
	}

	public static void putUUID(ByteBuffer buffer, UUID value, int offset) {
		buffer.order(ByteOrder.BIG_ENDIAN);
		long mostSig = value.getMostSignificantBits();  buffer.getLong(offset);
		long leastSig = value.getLeastSignificantBits(); buffer.getLong(offset + 8);

		buffer.putLong(offset, mostSig);
		buffer.putLong(offset + 8, leastSig);
	}

	public static void putDate(ByteBuffer buffer, Date value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int unixTime = (int) (value.getTime() / 1000);
		buffer.putInt(offset, unixTime);
	}

	/**
	 * Get the length of the java string as it would be encoded on disk.
	 * Use this because string.length will return the length in characters, which
	 * happen to be 16Bit unsigned.
	 */
	public static int getStringByteLength(String string) {
		CharsetEncoder encoder = Filesystem.getCharset().newEncoder();
		try {
			return encoder.encode(CharBuffer.wrap(string.toCharArray())).limit();
		} catch (CharacterCodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void putString(ByteBuffer buffer, String value, int len, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		CharBuffer cbuf = CharBuffer.wrap(value.toCharArray());
		CharsetEncoder encoder = Filesystem.getCharset().newEncoder();
		encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

		cbuf.limit(Math.min(len, cbuf.limit()));
		buffer.limit(offset+len);
		buffer.position(offset);

		encoder.encode(cbuf, buffer, true);

		buffer.limit(buffer.capacity());
	}

	public static void putLE32(ByteBuffer buffer, int value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(offset, value);
		assert (value == getLE32(buffer, offset));
	}

	public static void putLE32U(ByteBuffer buffer, long value, int offset) {
		if (!(value >= LE32_MIN  && value <= LE32_MAX))
			throw new IllegalArgumentException("Value " + value + " out of Range: " +
					"for x: " + LE32_MIN + " <= x <= " + LE32_MAX);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(offset, (int)(value & 0xffffffffL));
		assert (value == getLE32U(buffer, offset));
	}

	public static void putLE16(ByteBuffer buffer, short value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort(offset, value);
		assert (value == getLE16U(buffer, offset));
	}

	public static void putLE16U(ByteBuffer buffer, int value, int offset) {
		if (!(value >= LE16_MIN  && value <= LE16_MAX))
			throw new IllegalArgumentException("Value " + value + " out of Range: " +
					"for x: " + LE16_MIN + " <= x <= " + LE16_MAX);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort(offset, (short)(value & 0xffff));
		assert (value == getLE16U(buffer, offset));
	}

	public static void putLE8(ByteBuffer buffer, byte value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(offset, value);

		assert (value == getLE8(buffer, offset));
	}

	public static void putLE8U(ByteBuffer buffer, short value, int offset) {
		if (!(value >= LE8_MIN  && value <= LE8_MAX))
			throw new IllegalArgumentException("Value " + value + " out of Range: " +
					"for x: " + LE8_MIN + " <= x <= " + LE8_MAX);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(offset, (byte)(value & 0xff));

		assert (value == getLE8U(buffer, offset));
	}
}
