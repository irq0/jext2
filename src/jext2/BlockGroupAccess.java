package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/** provide access to block group descriptors */
public class BlockGroupAccess {
	private static BlockAccess blocks = BlockAccess.getInstance();
	
	private BlockGroupDescriptor[] descriptors;
	private BlockGroupAccess instance;
	
	public BlockGroupAccess() {
		if (this.instance != null) {
			throw new RuntimeException("singleton!");
		} 
		instance = this;	
	}
	
	private BlockGroupDescriptor readDescriptorByGroupNr(int nr) throws IOException {
		int blockNr = Calculations.groupFirstBlock(nr);
		ByteBuffer block = blocks.read(blockNr);
		
		return BlockGroupDescriptor.fromByteBuffer(block);
	}
	
	public void readDescriptors() throws IOException {
		int groupCount = Calculations.groupCount();
		descriptors = new BlockGroupDescriptor[groupCount];
		
		for (int i=0; i<groupCount; i++) {
			descriptors[i] = readDescriptorByGroupNr(i);
		}
	}
	
	public BlockGroupDescriptor getGroupDescriptor(int group) {
		return descriptors[group];
	}
	
	public BlockGroupAccess getInstance() {
		return this.instance;
	}
	
	
	private class BlockGroupDescriptorIterator 
	implements Iterator<BlockGroupDescriptor>, Iterable<BlockGroupDescriptor> {
		private int current = 0;
		
		public boolean hasNext() {
			return (current < Calculations.groupCount());
		}

		public BlockGroupDescriptor next() {
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
