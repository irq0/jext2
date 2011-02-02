package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Bitmap extends Block {
	private ByteBuffer bmap = ByteBuffer.allocate(Superblock.getInstance().getBlocksize());
	protected void read(ByteBuffer buf) throws IOException {
	    this.bmap = buf;
	    this.bmap.order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Return bit position of next zero in bitmap. Search up to numBytes bytes including the 
	 * byte in wich start is located. This means that a search starting at the last bit in one 
	 * byte will just check a single bit.
	 */
	public int getNextZeroBitPos(int start, int numBytes) {
	    if ((bmap.limit() - start) == 0)
	        return -1;
	    
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
	    return Bitmap.formatByte(bmap.get(pos/8));
	}
	
	
	/**
	 * test if bit at position pos is 1 
	 */
	public boolean isSet(int pos) {
        int byteNum = pos / 8;
        byte offset = (byte) (pos % 8);
        
	    byte chunk = bmap.get(byteNum);
	    byte mask = (byte)(1 << offset);
	    
	    return ((mask & chunk) != 0);
	}
	
	/**
	 * set bit to value
	 */
	public void setBit(int pos, boolean value) {
		int byteNum = pos / 8;
		byte offset = (byte) (pos % 8);
		byte chunk = bmap.get(byteNum);
		
		if (value == true) // set to 1
			chunk = (byte) (chunk | (1 << offset));
		else  // set to 0
			chunk = (byte) (chunk & (0xFF ^ (1 << offset))); 

		bmap.put(byteNum, chunk);

		if (value)
		    assert isSet(pos);
		else
		    assert !isSet(pos);
	}

	/**
	 * Find position of first zero bit from the left
	 * "00111111" -> 0
	 */
	public int findLeftMostZeroBitInByte(byte b) {
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
    public int findRightModeZeroBitInByte(byte b) {
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
	
	public static Bitmap fromByteBuffer(ByteBuffer buf, long blockNr) throws IOException {
		Bitmap bmap = new Bitmap(blockNr);
		bmap.read(buf);
		return bmap;
	}
	
	public void write() throws IOException {
		write(bmap);
	}	    
	
	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    sb.append(this.getClass());
	    sb.append("[\n");
	    sb.append("  blockNr=" + getBlockNr() + "\n");
	    sb.append("  bitmap=\n");
	    
	    bmap.rewind();
	    for (int i=0; i<bmap.limit()/(Integer.SIZE/8); i++) {
	        StringBuffer binstr = new StringBuffer();
	        binstr.append(String.format("%1$#23s", (Integer.toBinaryString(bmap.getInt())).replace(' ','0')));
	        sb.append(binstr.reverse());
	        sb.append("\n");
	    }
	    
	    sb.append("]");
	    return sb.toString();
	}
}
