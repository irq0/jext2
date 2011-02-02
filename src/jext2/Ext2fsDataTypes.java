package jext2;

import java.io.IOException;
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

	    CharsetDecoder decoder = Filesystem.getCharset().newDecoder();
	    buffer.limit(offset+len);
	    buffer.position(offset);
	     
	    CharBuffer cbuf = decoder.decode(buffer);
	        
	    buffer.limit(buffer.capacity());  
	        
		return cbuf.toString();
	
	}

	public static UUID getUUID(ByteBuffer buffer, int offset) throws IOException{
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
	
	public static void putLE64(ByteBuffer buffer, long value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(offset, value);
	}
	
	public static void putLE32(ByteBuffer buffer, int value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(offset, value);
	}

	public static void putLE32U(ByteBuffer buffer, long value, int offset) {
	    if (value < 0) 
	        throw new IllegalArgumentException("Attempt to put negative number");
	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	    buffer.putInt(offset, (int)(value & 0xffffffffL));
	}
	
	public static void putLE16(ByteBuffer buffer, short value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort(offset, value);
	}

	public static void putLE16U(ByteBuffer buffer, int value, int offset) {
        if (value < 0) 
            throw new IllegalArgumentException("Attempt to put negative number");
	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	    buffer.putShort(offset, (short)(value & 0xffff));
	}
	
	public static void putLE8(ByteBuffer buffer, byte value, int offset) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(offset, value);
	}

	public static void putLE8U(ByteBuffer buffer, short value, int offset) {
	    if (value < 0) 
	        throw new IllegalArgumentException("Attempt to put negative number");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(offset, (byte)(value & 0xff));
    }	
}
