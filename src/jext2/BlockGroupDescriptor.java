package jext2;

import java.nio.ByteBuffer;
import java.io.IOException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class BlockGroupDescriptor extends Block {
	private int blockBitmap;
	private int inodeBitmap;
	private int inodeTable;
	private short freeBlocksCount;
	private short freeInodesCount;
	private short usedDirsCount;

	public final int getBlockBitmap() {
		return this.blockBitmap;
	}
	public final int getInodeBitmap() {
		return this.inodeBitmap;
	}
	public final int getInodeTable() {
		return this.inodeTable;
	}
	public final short getFreeBlocksCount() {
		return this.freeBlocksCount;
	}
	public final short getFreeInodesCount() {
		return this.freeInodesCount;
	}
	public final short getUsedDirsCount() {
		return this.usedDirsCount;
	}

	
	private void readBlockGroupDescriptor() throws IOException {
		this.blockBitmap = getLE32(0);
		this.inodeBitmap = getLE32(4);
		this.inodeTable = getLE32(8);
		this.freeBlocksCount = getLE16(12);
		this.freeInodesCount = getLE16(14);
		this.usedDirsCount = getLE16(16);
	}

	public static BlockGroupDescriptor fromByteBuffer(ByteBuffer buf) {
		BlockGroupDescriptor b = new BlockGroupDescriptor();
		try {
			b.buffer = buf;
			b.readBlockGroupDescriptor();
			return b;
		} catch (IOException e) {
			// XXX dont irgnore
			return b;
		}
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}

}		
