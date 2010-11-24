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

	
	protected void read(ByteBuffer buf) throws IOException {
		this.blockBitmap = Ext2fsDataTypes.getLE32(buf, 0);
		this.inodeBitmap = Ext2fsDataTypes.getLE32(buf, 4);
		this.inodeTable = Ext2fsDataTypes.getLE32(buf, 8);
		this.freeBlocksCount = Ext2fsDataTypes.getLE16(buf, 12);
		this.freeInodesCount = Ext2fsDataTypes.getLE16(buf, 14);
		this.usedDirsCount = Ext2fsDataTypes.getLE16(buf, 16);
	}

	protected BlockGroupDescriptor(int blockNr, int offset) {
		super(blockNr, offset);
	}
	
	public static BlockGroupDescriptor fromByteBuffer(ByteBuffer buf) {
		BlockGroupDescriptor b = new BlockGroupDescriptor(-1, -1);
		try {
			b.read(buf);
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
