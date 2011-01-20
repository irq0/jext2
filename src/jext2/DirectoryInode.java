package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;


/** Extends Inode with directory access methods */
public class DirectoryInode extends DataInode {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();

	public DirectoryIterator iterateDirectory() {
		return new DirectoryIterator(this);
	}
	
	class DirectoryIterator implements Iterator<DirectoryEntry>, Iterable<DirectoryEntry> {
		private DirectoryInode inode;
		private int fileBlockNr = 0;
		private ByteBuffer block;
		private DirectoryEntry entry;
		private int offset = 0;
		
		DirectoryIterator(DirectoryInode inode) {
			this.inode = inode;

			try {
				int blockNr = DataBlockAccess.getDataBlockNr(inode, fileBlockNr);
				block = blocks.read(blockNr);
			} catch (IOException e) {
				entry = null;
			}
				
			fetchNextEntry();
		}

		public boolean hasNext() {
			return (entry != null);
		}

		private void fetchNextEntry() {
			try {
				if (entry != null) {
					offset += entry.getRecLen();
				} 	

				// entry was last in this block
				if (offset == superblock.getBlocksize()) {
					fileBlockNr += 1;
					int blockNr = DataBlockAccess.getDataBlockNr(inode, fileBlockNr);
				
					if (blockNr == 0) { // entry was last
						block = null;
					} else { // start with new block
						block = blocks.read(blockNr);
					}
					
					offset = 0;
				}
				
				// fetch next entry from block
				if (block != null) {
					entry = DirectoryEntry.fromByteBuffer(block, offset);
				} else {
					entry = null;
				}
			} catch (IOException e) {
				entry = null;
			}
		}
		
		public DirectoryEntry next() {
			DirectoryEntry result = this.entry;
			fetchNextEntry();
			return result;
		}

		public void remove() {
		}

        public Iterator<DirectoryEntry> iterator() {
            return this;
        }
		
	}

	public DirectoryEntry lookup(String name) {
		for (DirectoryEntry dir : iterateDirectory()) {
			if (name.equals(dir.getName())) {
				return dir;
			}
		}
		return null;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append(" DIRECTORY={");		
		for (DirectoryEntry dir : iterateDirectory()) {
			sb.append("   ");
			sb.append(dir.getName());
			sb.append(",");
			sb.append("\n");
		}
		sb.append("}");

		return sb.toString();
	}

	protected DirectoryInode(int blockNr, int offset) throws IOException {
		super(blockNr, offset);
	}

	public static DirectoryInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		DirectoryInode inode = new DirectoryInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	
}
