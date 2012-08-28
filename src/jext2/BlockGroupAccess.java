/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

package jext2;

import java.nio.ByteBuffer;
import java.util.Iterator;

import jext2.exceptions.IoError;

/** provide access to block group descriptors */
public class BlockGroupAccess {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();

	private BlockGroupDescriptor[] descriptors;
	private static BlockGroupAccess instance = new BlockGroupAccess();

	private BlockGroupAccess() {
	}

	/*
	 * Read descriptors from first block group. Initialize the write-back parameters
	 * to the first descriptor table. The backup tables will therefore no be updated.
	 */
	// TODO reimplement with block iterator
	public void readDescriptors() throws IoError {
		int blockCount = (superblock.getGroupsCount() +
				superblock.getGroupDescrPerBlock() - 1) /
				superblock.getGroupDescrPerBlock();
		int groupCount = superblock.getGroupsCount();
		long start = BlockGroupDescriptor.descriptorLocation(0);
		int groupsPerBlock = superblock.getGroupDescrPerBlock();
		int group = 0;
		descriptors = new BlockGroupDescriptor[groupCount+1];

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

	public void syncDescriptors() throws IoError {
		for (BlockGroupDescriptor descr : iterateBlockGroups()) {
			descr.sync();
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

		@Override
		public boolean hasNext() {
			return (current < superblock.getGroupsCount());
		}

		@Override
		public BlockGroupDescriptor next() {
			return descriptors[current++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<BlockGroupDescriptor> iterator() {
			return this;
		}
	}

	public BlockGroupDescriptorIterator iterateBlockGroups() {
		return new BlockGroupDescriptorIterator(0);
	}
	public BlockGroupDescriptorIterator iterateBlockGroups(int start) {
		return new BlockGroupDescriptorIterator(start);
	}

}
