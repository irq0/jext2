package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/** provide access to block group descriptors */
public class BlockGroupAccess {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();
	
	private BlockGroupDescriptor[] descriptors;
	private static BlockGroupAccess instance = null;
	
	public BlockGroupAccess() {
		if (instance != null) {
			throw new RuntimeException("singleton!");
		} 
		instance = this;	
	}
		
	/* 
	 * Read descriptors from first block group. Initialize the write-back parameters
	 * to the first descriptor table. The backup tables will therefore no be updated. 
	 */
	public void readDescriptors() throws IOException {
		int blockCount = (superblock.getGroupsCount() + 
						  superblock.getGroupDescrPerBlock() - 1) /
						  superblock.getGroupDescrPerBlock();
		int groupCount = superblock.getGroupsCount();
		long start = BlockGroupDescriptor.descriptorLocation(0);
		int groupsPerBlock = superblock.getGroupDescrPerBlock();
		int group = 0;
		descriptors = new BlockGroupDescriptor[superblock.getGroupsCount()];
		
		for (long nr=start; nr<blockCount+start; nr++) {			
			ByteBuffer buf = blocks.read(nr);
			
			for (int i=0; i<Math.min(groupCount, groupsPerBlock); i++) {				
				descriptors[group] = BlockGroupDescriptor.fromByteBuffer(buf, nr, i*32);
				descriptors[group].setBlockGroup(group);
				group++;
			}
			
			groupCount -= groupsPerBlock;
		}
	}
	
	public BlockGroupDescriptor getGroupDescriptor(int group) {
		return descriptors[group];
	}
	
	public static BlockGroupAccess getInstance() {
		return instance;
	}
	
	
	private class BlockGroupDescriptorIterator 
	implements Iterator<BlockGroupDescriptor>, Iterable<BlockGroupDescriptor> {
		private int current = 0;
		
		public BlockGroupDescriptorIterator(int start) {
		    current = start;
		}
		
		public boolean hasNext() {
			return (current < superblock.getGroupsCount());
		}

		public BlockGroupDescriptor next() {
			return descriptors[current++];			
		}

		public void remove() {
		}
		
		public BlockGroupDescriptorIterator iterator() {
			return this;
		}				
	}
			
	public BlockGroupDescriptorIterator iterateBlockGroups() {
		return new BlockGroupDescriptorIterator(0);
	}
    public BlockGroupDescriptorIterator iterateBlockGroups(int start) {
        return new BlockGroupDescriptorIterator(start);
    }
	
	
	
	private Bitmap readBitmapAtBlock(long nr) throws IOException {
		ByteBuffer buf = blocks.read(nr);
		Bitmap bmap = Bitmap.fromByteBuffer(buf, nr);
		
		return bmap;
	}
	
	public Bitmap readInodeBitmapOf(BlockGroupDescriptor group) throws IOException {
		return readBitmapAtBlock(group.getInodeBitmapPointer());
	}
	
}
