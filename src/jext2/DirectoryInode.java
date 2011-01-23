package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;


/** Inode for directories */
public class DirectoryInode extends DataInode {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();

	public DirectoryIterator iterateDirectory() {
		return new DirectoryIterator();
	}
	
	/**
	 * iterates directory entries.
	 */
	public class DirectoryIterator implements Iterator<DirectoryEntry>, Iterable<DirectoryEntry> {
		private ByteBuffer block;
		private DirectoryEntry entry;
		private int offset = 0;

		private Iterator<Long> blockIter;
		
		DirectoryIterator() {
		    blockIter = accessData().iterateBlocks();
			this.entry = fetchNextEntry(null);
		}

		public boolean hasNext() {
			return (entry != null);
		}

		private DirectoryEntry fetchNextEntry(DirectoryEntry last) {
			try {
			    if (last == null && block == null) {
			        block = blocks.read(blockIter.next());
			        return DirectoryEntry.fromByteBuffer(block, 0);
			    }
			    
				if (last.getRecLen() != 0) {
					offset += last.getRecLen();
				} /*else {
				    offset = superblock.getBlocksize();
				}
				*/
				// entry was last in this block
				if (offset >= superblock.getBlocksize()) {
				    if (blockIter.hasNext())
				        block = blocks.read(blockIter.next());
				    else
				        return null;			    
					offset = 0;
				}
				
				// fetch next entry from block
				return DirectoryEntry.fromByteBuffer(block, offset);
			} catch (IOException e) {
				return null;
			}
		}
		
		public DirectoryEntry next() {
			DirectoryEntry result = this.entry;
			this.entry = fetchNextEntry(entry);
			return result;
		}

		public void remove() {
		}

        public Iterator<DirectoryEntry> iterator() {
            return this;
        }
		
	}

	public class FileExistsException extends Exception {
        private static final long serialVersionUID = 1L;
	}
	
	
	/**
	 * Add directory entry. Iterate over data blocks and check for entries with
	 * the same name on the way. 
	 * @throws FileExistsException 
	 * 
	 */
	public void addLink(DirectoryEntry newEntry) throws IOException, FileExistsException {
       ByteBuffer block;
       int offset = 0;
       
       for (long blockNr : accessData().iterateBlocks()) {
           block = blocks.read(blockNr);

           while (offset + 8 <= block.limit()) { /* space for minimum of one entry */
               DirectoryEntry currentEntry = DirectoryEntry.fromByteBuffer(block, offset);
               
               if (currentEntry.getName().equals(newEntry.getName())) 
                   throw new FileExistsException();
               
               if (currentEntry.getRecLen() == 0)
                   throw new IOException("zero-length directory entry");
               
               /* 
                * See if current directory entry is unused; if so, 
                * absorb it into this one.
                */ 
               if (currentEntry.isUnused() && 
                   currentEntry.getRecLen() >= newEntry.getRecLen()) {
                   
                   newEntry.setRecLen(currentEntry.getRecLen());                   
                   blocks.writePartial(blockNr, offset, newEntry.toByteBuffer());
                   
                   return;
               }
               
               /*
                * If entry is used, see if we can split the directory entry
                * to make room for the new one
                */
               if (currentEntry.getRecLen() >= 
                      8 + currentEntry.getNameLen() + newEntry.getRecLen()) {
                   
                   int spaceFreed = (currentEntry.getRecLen() - 
                           (8 + currentEntry.getNameLen()));
                   
                   /* truncate the old one */
                   currentEntry.setRecLen(8 + currentEntry.getNameLen());
                   blocks.writePartial(blockNr, offset, currentEntry.toByteBuffer());
                   offset += currentEntry.getRecLen();
                   
                   /* fill in the new one */
                   newEntry.setRecLen(spaceFreed);
                   blocks.writePartial(blockNr, offset, newEntry.toByteBuffer());

                   return;
               }
               
               offset += currentEntry.getRecLen();
           }               
           offset = 0;
       }
       
       /* We checked every block but didn't find any free space. 
        * Allocate next block
        */
       long blockNr = accessData().getBlocksAllocate(getBlocks()+1, 1).getFirst();
       blocks.writePartial(blockNr, 0, newEntry.toByteBuffer());
       
	}

	/** 
	 * Lookup name in directory. This is done by iterating each entry and
	 * compareing the names. 
	 * 
	 * @return     DirectoryEntry or null in case its not found
	 */
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

	protected DirectoryInode(long blockNr, int offset) throws IOException {
		super(blockNr, offset);
	}

	public static DirectoryInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		DirectoryInode inode = new DirectoryInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	
}
