package jext2;

import java.nio.ByteBuffer;
import java.io.IOException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class BlockGroup extends Block {
	private static Superblock superblock = Superblock.getInstance();
	
	private int blockBitmap;
	private int inodeBitmap;
	private int inodeTable;


	private short freeBlocksCount;
	private short freeInodesCount;
	private short usedDirsCount;

	// in memory data
	private int blockGroup = -1;
	
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
	public final int getBlockGroup() {
		return this.blockGroup;
	}
	void setBlockGroup(int blockGroup) {
		this.blockGroup = blockGroup;
	}
	public void setFreeBlocksCount(short freeBlocksCount) {
		this.freeBlocksCount = freeBlocksCount;
	}
	public void setFreeInodesCount(short freeInodesCount) {
		this.freeInodesCount = freeInodesCount;
	}
	public void setUsedDirsCount(short usedDirsCount) {
		this.usedDirsCount = usedDirsCount;
	}
	
	protected void read(ByteBuffer buf) throws IOException {
		this.blockBitmap = Ext2fsDataTypes.getLE32(buf, 0 + offset);
		this.inodeBitmap = Ext2fsDataTypes.getLE32(buf, 4 + offset);
		this.inodeTable = Ext2fsDataTypes.getLE32(buf, 8 + offset);
		this.freeBlocksCount = Ext2fsDataTypes.getLE16(buf, 12 + offset);
		this.freeInodesCount = Ext2fsDataTypes.getLE16(buf, 14 + offset);
		this.usedDirsCount = Ext2fsDataTypes.getLE16(buf, 16 + offset);
	}

	protected BlockGroup(int blockNr, int offset) {
		super(blockNr, offset);
	}
	
	public static BlockGroup fromByteBuffer(ByteBuffer buf, int blockNr, int offset) {
		BlockGroup b = new BlockGroup(blockNr, offset);
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
	
	public static int descriptorLocation(int group) {
		 int hasSuper = hasSuperblock(group) ? 1 : 0;
		 
		 return firstBlock(group) + hasSuper;	 
	}
		 
	
	public static int firstBlock(int group) {
		return superblock.getFirstDataBlock() +
			(group * superblock.getBlocksPerGroup());
	}

	public static boolean hasSuperblock(int group) {
		if (Feature.sparseSuper()) {
			return isSparse(group);
		} else {
			return true;
		}
	}
	
	private static boolean isSparse(int group) {
		return ((group <= 1) ||
				test_root(group, 3) || 
				test_root(group, 5) ||
				test_root(group, 7));
	}
	
	private static boolean test_root(int a, int b) {
		int num = b;
		
		while (a > num)
			num *= b;
		return num == a;
	}

}		








