package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/** provide access to block group descriptors */
public class BlockGroupAccess {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();
	
	private BlockGroup[] descriptors;
	private static BlockGroupAccess instance = null;
	
	public BlockGroupAccess() {
		if (instance != null) {
			throw new RuntimeException("singleton!");
		} 
		instance = this;	
	}
		
	public void readDescriptors() throws IOException {
		int blockCount = (superblock.getGroupsCount() + 
						  superblock.getGroupDescrPerBlock() - 1) /
						  superblock.getGroupDescrPerBlock();
		int groupCount = superblock.getGroupsCount();
		int start = BlockGroup.descriptorLocation(0);
		int groupsPerBlock = superblock.getGroupDescrPerBlock();
		int group = 0;
		descriptors = new BlockGroup[superblock.getGroupsCount()];
		
		for (int nr=start; nr<blockCount+start; nr++) {			
			ByteBuffer buf = blocks.read(nr);
			
			for (int i=0; i<Math.min(groupCount, groupsPerBlock); i++) {				
				descriptors[group++] = BlockGroup.fromByteBuffer(buf, nr, i*32);
			}
			
			groupCount -= groupsPerBlock;
		}
	}
	
	public BlockGroup getGroupDescriptor(int group) {
		return descriptors[group];
	}
	
	public static BlockGroupAccess getInstance() {
		return instance;
	}
	
	
	private class BlockGroupDescriptorIterator 
	implements Iterator<BlockGroup>, Iterable<BlockGroup> {
		private int current = 0;
		
		public boolean hasNext() {
			return (current < superblock.getGroupsCount());
		}

		public BlockGroup next() {
			return descriptors[current++];			
		}

		public void remove() {
		}
		
		public BlockGroupDescriptorIterator iterator() {
			// TODO Auto-generated method stub
			return this;
		}				
	}
			
	public BlockGroupDescriptorIterator iterateBlockGroups() {
		return new BlockGroupDescriptorIterator();
	}
		
}
