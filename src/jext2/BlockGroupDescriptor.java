package jext2;

import java.nio.ByteBuffer;
import java.io.IOException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class BlockGroupDescriptor extends Block {
	private static Superblock superblock = Superblock.getInstance();
	
	private long blockBitmap;
	private long inodeBitmap;
	private long inodeTable;


	private int freeBlocksCount;
	private int freeInodesCount;
	private int usedDirsCount;

	private long blockGroup = -1;
	
	public final long getBlockBitmapPointer() {
		return this.blockBitmap;
	}
	public final long getInodeBitmapPointer() {
		return this.inodeBitmap;
	}
	public final long getInodeTablePointer() {
		return this.inodeTable;
	}
	public final int getFreeBlocksCount() {
		return this.freeBlocksCount;
	}
	public final int getFreeInodesCount() {
		return this.freeInodesCount;
	}
	public final int getUsedDirsCount() {
		return this.usedDirsCount;
	}
	public final long getBlockGroup() {
		return this.blockGroup;
	}
	void setBlockGroup(long blockGroup) {
		this.blockGroup = blockGroup;
	}
	public void setFreeBlocksCount(int freeBlocksCount) {
		this.freeBlocksCount = freeBlocksCount;
	}
	public void setFreeInodesCount(int freeInodesCount) {
		this.freeInodesCount = freeInodesCount;
	}
	public void setUsedDirsCount(int usedDirsCount) {
		this.usedDirsCount = usedDirsCount;
	}	
	public final void setBlockBitmap(long blockBitmap) {
		this.blockBitmap = blockBitmap;
	}
	public final void setInodeBitmap(long inodeBitmap) {
		this.inodeBitmap = inodeBitmap;
	}
	public final void setInodeTable(long inodeTable) {
		this.inodeTable = inodeTable;
	}
	
	protected void read(ByteBuffer buf) throws IOException {
		this.blockBitmap = Ext2fsDataTypes.getLE32U(buf, 0 + offset);
		this.inodeBitmap = Ext2fsDataTypes.getLE32U(buf, 4 + offset);
		this.inodeTable = Ext2fsDataTypes.getLE32U(buf, 8 + offset);
		this.freeBlocksCount = Ext2fsDataTypes.getLE16U(buf, 12 + offset);
		this.freeInodesCount = Ext2fsDataTypes.getLE16U(buf, 14 + offset);
		this.usedDirsCount = Ext2fsDataTypes.getLE16U(buf, 16 + offset);
	}

	protected void write(ByteBuffer buf) throws IOException {
		Ext2fsDataTypes.putLE32U(buf, this.blockBitmap, 0);
		Ext2fsDataTypes.putLE32U(buf, this.inodeBitmap, 4);
		Ext2fsDataTypes.putLE32U(buf, this.inodeTable, 8);
		Ext2fsDataTypes.putLE16U(buf, this.freeBlocksCount, 12);
		Ext2fsDataTypes.putLE16U(buf, this.freeInodesCount, 14);
		Ext2fsDataTypes.putLE16U(buf, this.usedDirsCount, 16);
		super.write(buf);
	}
	
	public void write() throws IOException {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);		
	}
	
	protected ByteBuffer allocateByteBuffer() {		
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.rewind();
		return buf;
	}
	
	protected BlockGroupDescriptor(long blockNr, int offset) {
		super(blockNr, offset);
	}
	
	public static BlockGroupDescriptor fromByteBuffer(ByteBuffer buf, long blockNr, int offset) {
		BlockGroupDescriptor b = new BlockGroupDescriptor(blockNr, offset);
		try {
			b.read(buf);
			return b;
		} catch (IOException e) {
			throw new RuntimeException("IOException in BlockGroupDescriptor->fromByteBuffer");
		}
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
	
	public static long descriptorLocation(int group) {
		 int hasSuper = hasSuperblock(group) ? 1 : 0;
		 
		 return firstBlock(group) + hasSuper;	 
	}
		 
	
	public static long firstBlock(long group) {
		return superblock.getFirstDataBlock() +
			(group * superblock.getBlocksPerGroup());
	}

	public static boolean hasSuperblock(long group) {
		if (Feature.sparseSuper()) {
			return isSparse(group);
		} else {
			return true;
		}
	}
	
	private static boolean isSparse(long group) {
		return ((group <= 1) ||
				test_root(group, 3) || 
				test_root(group, 5) ||
				test_root(group, 7));
	}
	
	private static boolean test_root(long a, long b) {
		long num = b;
		
		while (a > num)
			num *= b;
		return num == a;
	}
	
}		