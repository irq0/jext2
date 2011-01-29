package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Bitmap extends Block {
	private ByteBuffer bmap = ByteBuffer.allocate(Superblock.getInstance().getBlocksize());
	protected void read(ByteBuffer buf) throws IOException {
		buf.position(0);
		bmap.put(buf);
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
				pos = (bmap.position()-1)*8 + findFirstZeroBitInByte(chunk);
				break;
			} 
						
			chunk = bmap.get();
			numBytes--;
		}
		
		return pos;
	}		

	public int getNextZeroBitPos(int start) {
	    return getNextZeroBitPos(start, bmap.limit());
	}

	/**
	 * test if bit at position pos is 1
	 */
	public boolean isSet(int pos) {
        int byteNum = pos / 8;
        byte offset = (byte) (7 - (pos % 8));
        
	    byte chunk = bmap.get(byteNum);
	    byte mask = (byte)(1 << offset);
	    
	    return ((mask & chunk) != 0);
	}
	
	/**
	 * set bit to value
	 */
	public void setBit(int pos, boolean value) {
		int byteNum = pos / 8;
		byte offset = (byte) (7 - (pos % 8));
		byte chunk = bmap.get(byteNum);

		if (value == true) // set to 1
			chunk = (byte) (chunk | (1 << offset));
		else  // set to 0
			chunk = (byte) (chunk & (0xFF ^ (1 << offset))); 

		bmap.put(byteNum, chunk);
			
	}

	/**
	 * find first zero bit in byte from the left
	 */
	private int findFirstZeroBitInByte(byte b) {
	    byte mask = -0x80; /* sign bit is 128 */
		for (int i=0; i<8; i++) {
		    if ((mask & b) == 0) {
	            return i;

			}

			mask = (byte) ((mask >>> 1) & ~mask);
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
	
	public static void test() throws IOException {
	    byte[] sequence = new byte[] { 23, 42, 1, 5, 3, 9, 7, 12 };
	    
	    ByteBuffer buf = ByteBuffer.allocate(50);
	    
	    buf.rewind();
	    
	    for (int i=0; i< buf.limit()/sequence.length; i++)
	        buf.put(sequence);
	    
	    Bitmap bmap = Bitmap.fromByteBuffer(buf, -1);
	    System.out.println(bmap);
	    
	    System.out.println(bmap.isSet(0)); // false
        System.out.println(bmap.isSet(32)); // false
        System.out.println(bmap.isSet(3)); // true
        System.out.println(bmap.isSet(38)); // true
        System.out.println(bmap.isSet(1023)); // false
	    
        System.out.println();
        
        bmap.setBit(1000, true);
        System.out.println(bmap.isSet(1000)); // true

        bmap.setBit(1000, false);
        System.out.println(bmap.isSet(1000)); // false

        System.out.println();
        
        bmap.setBit(1000, true);
        System.out.println(bmap.isSet(1000)); // true


        System.out.println();
        System.out.println(bmap.findFirstZeroBitInByte((byte)0x8F)); // 1
        System.out.println(bmap.findFirstZeroBitInByte((byte)-0x80)); // 1
        System.out.println(bmap.findFirstZeroBitInByte((byte)0xDA)); // 2
        System.out.println(bmap.findFirstZeroBitInByte((byte)0xFE)); // 7
        System.out.println(bmap.findFirstZeroBitInByte((byte)0x0F)); // 0
        System.out.println(bmap.findFirstZeroBitInByte((byte)0xFF)); // -1

        System.out.println();
        bmap.setBit(600, true);
        bmap.setBit(601, true);
        bmap.setBit(602, true);
        bmap.setBit(603, true);
        bmap.setBit(604, true);
        System.out.println(bmap.getNextZeroBitPos(600)); // 605
        System.out.println(bmap.getNextZeroBitPos(600,1)); // 605
        bmap.setBit(605, true);
        bmap.setBit(606, true);        
        bmap.setBit(607, true);
        bmap.setBit(608, true);
        System.out.println(bmap.getNextZeroBitPos(600,1)); // -1 
        System.out.println(bmap.getNextZeroBitPos(608,1)); // 609
        System.out.println(bmap.getNextZeroBitPos(607,1)); // -1
        
        System.out.println(bmap.getNextZeroBitPos(706,1)); // -1
	}
	    
	    
	
	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    sb.append(this.getClass());
	    sb.append("[\n");
	    sb.append("  blockNr=" + getBlockNr() + "\n");
	    sb.append("  bitmap=\n");
	    
	    bmap.rewind();
	    for (int i=0; i<bmap.limit()/(Integer.SIZE/8); i++) {
	        sb.append(String.format("%1$#32s", Integer.toBinaryString(bmap.getInt())).replace(' ', '0'));
	        sb.append("\n");
	    }
	    
	    sb.append("]");
	    return sb.toString();
	}
}
