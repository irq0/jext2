package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.UUID;

public class Ext2fsDataTypes {

	public static Date getDate(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long unixTime = buffer.getInt(offset);
		return new Date(unixTime * 1000);
	}

	public static short getLE16(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);				
		return buffer.getShort(offset);
	}

	public static int getLE16U(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);				
		return (int)buffer.getShort(offset) & 0xffff;
	}

	public static int getLE32(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.getInt(offset);
	}

	public static long getLE32U(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return (long)buffer.getInt(offset) & 0xffffffffL;
	}

	public static long getLE64(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.getLong(offset);
	}

	public static byte getLE8(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.get(offset);
	}

	public static short getLE8U(ByteBuffer buffer, int offset) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return (short)(buffer.get(offset) & (short)0xff);
	}

	public static String getString(ByteBuffer buffer, int offset, int len) throws IOException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(offset);
		StringBuffer result = new StringBuffer();
		for (int i=0; i<len; i++) {
			result.append((char)buffer.get());
		}
	
		return result.toString();
	
	}

	public static UUID getUUID(ByteBuffer buffer, int offset) throws IOException{
		buffer.order(ByteOrder.BIG_ENDIAN);
		long mostSig = buffer.getLong(offset);
		long leastSig = buffer.getLong(offset + 8);
		return new UUID(mostSig, leastSig);
	}

}
