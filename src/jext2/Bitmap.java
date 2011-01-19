package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Bitmap extends Block {
	private ByteBuffer bmap = ByteBuffer.allocate(Superblock.getInstance().getBlocksize());
	protected void read(ByteBuffer buf) throws IOException {
		buf.position(0);
		bmap.put(buf);
	}

	public int getNextZeroBitPos(int start) {
		int pos = -1;
		
		bmap.position(start/8);
		while(bmap.hasRemaining()) {
			byte chunk = bmap.get();
			
			if (chunk != 0xFF) { // has zero bit
				pos = bmap.position()*8 + findFirstBitInByte(chunk);
				break;
			}
		}
		
		if (pos == -1) return -1;
		return pos;
	}		
	
	public int getNextZeroBitPos(int start, int end) {
	        int pos = -1;
	        int iter = end - start;
	        
	        bmap.position(start/8);
	        while(bmap.hasRemaining() && iter-- > 0 ) {
	            byte chunk = bmap.get();
	            
	            if (chunk != 0xFF) { // has zero bit
	                pos = bmap.position()*8 + findFirstBitInByte(chunk);
	                break;
	            }
	        }
	        
	        if (pos == -1) return -1;
	        return pos;
	    }       

	
	
	public boolean isSet(int pos) {
        int byteNum = pos / 8;
        byte offset = (byte) (pos % 8);
        
	    byte b = bmap.get(byteNum);
	    byte mask = (byte)(1 << offset);
	    
	    return ((mask & b) != 0);
	}
	
	public void setBit(int pos, boolean value) {
		int byteNum = pos / 8;
		int offset = pos % 8;
		byte chunk = bmap.get(byteNum);

		if (value == true) // set to 1
			chunk = (byte) (chunk | (1 << offset));
		else  // set to 0
			chunk = (byte) (chunk & (0xFF ^ (1 << offset))); 

		bmap.put(byteNum, chunk);
			
	}
	
	private int findFirstBitInByte(byte b) {
		byte mask = 1;
		
		for (int i=0; i<7; i++) {
			if ((mask & b) == 0) {
				return i;
			}

			mask = (byte) (mask << 1);
			
		}
		return -1;
	}
		
	protected Bitmap(int blockNr, int offset) {
		super(blockNr, offset);
	}
	
	public static Bitmap fromByteBuffer(ByteBuffer buf, int blockNr) throws IOException {
		Bitmap bmap = new Bitmap(blockNr, 0);
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
	    sb.append("  offset=" + getOffset() + "\n");
	    sb.append("  bitmap=\n");
	    
	    bmap.rewind();
	    for (int i=0; i<bmap.limit()/Integer.SIZE; i++) {
	        sb.append(String.format("%1$#32s", Integer.toBinaryString(bmap.getInt())).replace(' ', '0'));
	        sb.append("\n");
	    }
	    
	    sb.append("]");
	    return sb.toString();
	}
}
