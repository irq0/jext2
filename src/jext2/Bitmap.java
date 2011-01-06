package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Bitmap extends Block {
	private ByteBuffer bmap = ByteBuffer.allocate(Superblock.getInstance().getBlocksize());
	protected void read(ByteBuffer buf) throws IOException {
		buf.position(0);
		bmap.put(buf);
	}

	public synchronized int getAndSetNextZeroBit() {
		int pos = getNextZeroBitPos(0);
		
		setBit(pos, true);		
		return pos;
		
	}	

	public int getNextZeroBitPos(int start) {
		int pos = -1;
		
		bmap.position(start);
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
}
