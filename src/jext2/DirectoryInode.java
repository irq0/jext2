package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import jext2.exceptions.DirectoryNotEmpty;
import jext2.exceptions.FileExists;
import jext2.exceptions.InvalidArgument;
import jext2.exceptions.NoSpaceLeftOnDevice;


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
		private long blockNr;
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
			        blockNr = blockIter.next();
			        block = blocks.read(blockNr);
			        return DirectoryEntry.fromByteBuffer(block, blockNr, 0);
			    }
			    
				if (last.getRecLen() != 0) {
					offset += last.getRecLen();
				} /*else {
				    offset = superblock.getBlocksize();
				}
				*/
				// entry was last in this block
				if (offset >= superblock.getBlocksize()) {
				    if (blockIter.hasNext()) {
				        blockNr = blockIter.next();
				        block = blocks.read(blockNr);
				    } else {
				        return null;
				    }
					offset = 0;
				}
				
				// fetch next entry from block
				return DirectoryEntry.fromByteBuffer(block, blockNr, offset);
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

	/**
	 * Add directory entry for given inode and name.
	 * @throws InvalidArgument 
	 * @throws NoSpaceLeftOnDevice 
	 * @see addLink(DirectoryEntry newEntry) 
	 */
	public void addLink(Inode inode, String name) throws IOException, FileExists, InvalidArgument, NoSpaceLeftOnDevice {
        DirectoryEntry newDir = DirectoryEntry.create(name);
        newDir.setIno(inode.getIno());
        newDir.setFileType(inode.getFileType());

        addLink(newDir);
	}
	
	/**
	 * Add directory entry. Iterate over data blocks and check for entries with
	 * the same name on the way. 
	 * @throws InvalidArgument     When we find a corrupt entry 
	 * @throws NoSpaceLeftOnDevice 
	 * @throws FileExistsException      When we find an entry with same name
	 */
	// TODO rewrite addLink to use the directory iterator
	public void addLink(DirectoryEntry newEntry) throws IOException, FileExists, InvalidArgument, NoSpaceLeftOnDevice {
       ByteBuffer block;
       int offset = 0;
       
       for (long blockNr : accessData().iterateBlocks()) {
           block = blocks.read(blockNr);

           while (offset + 8 <= block.limit()) { /* space for minimum of one entry */
               DirectoryEntry currentEntry = DirectoryEntry.fromByteBuffer(block, blockNr, offset);
               
               if (currentEntry.getName().equals(newEntry.getName())) 
                   throw new FileExists();
               
               if (currentEntry.getRecLen() == 0 ||
                       currentEntry.getRecLen() > superblock.getBlocksize() ||
                       (currentEntry.getRecLen() & 3) == 0) {
                   throw new InvalidArgument();
               }
               
               /* 
                * See if current directory entry is unused; if so, 
                * absorb it into this one.
                */ 
               if (currentEntry.isUnused() && 
                   currentEntry.getRecLen() >= newEntry.getRecLen()) {
                   
                   newEntry.setRecLen(currentEntry.getRecLen());                   
                   blocks.writePartial(blockNr, offset, newEntry.toByteBuffer());
                   
                   setLinksCount(getLinksCount() + 1);            
                   return;
               }
               
               /*
                * If entry is used, see if we can split the directory entry
                * to make room for the new one
                */
               if (currentEntry.getRecLen() >= newEntry.getRecLen() +
                     DirectoryEntry.minSizeNeeded(currentEntry.getNameLen())) {
                   
                   int spaceFreed = currentEntry.getRecLen() -
                           DirectoryEntry.minSizeNeeded(currentEntry.getNameLen());
                                              
                   /* truncate the old one */
                   currentEntry.truncateRecord();
                   
                   blocks.writePartial(blockNr, offset, currentEntry.toByteBuffer());
                   offset += currentEntry.getRecLen();
                   
                   /* fill in the new one */
                   newEntry.setRecLen(spaceFreed);
                   blocks.writePartial(blockNr, offset, newEntry.toByteBuffer());

                   setLinksCount(getLinksCount() + 1);            
                   return;
               }
               
               offset += currentEntry.getRecLen();
           }               
           offset = 0;
       }
       
       /* We checked every block but didn't find any free space. 
        * Allocate next block add two entries:
        *   (1) the new one
        *   (2) the dummy "rest" entry
        */
       LinkedList<Long> allocBlocks = 
           accessData().getBlocksAllocate(getBlocks()/(superblock.getBlocksize()/512), 1);       
       
       if (allocBlocks.size() == 0) 
           throw new IOException();       
       long blockNr = allocBlocks.getFirst();
       
       blocks.writePartial(blockNr, 0, newEntry.toByteBuffer());
       
       DirectoryEntry rest = DirectoryEntry.createRestDummy(newEntry);
       blocks.writePartial(blockNr, newEntry.getRecLen(), rest.toByteBuffer());
	}

	
	public boolean isEmptyDirectory() {
	    int count = 0;
	    for (DirectoryEntry dir : iterateDirectory()) {
	        count += 1;
	        if (count >= 3) 
	            return false;
	    }
	    return true;
	}
	
	
	/** 
	 * Lookup name in directory. This is done by iterating each entry and
	 * comparing the names. 
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

		sb.append(" DIRECTORY=[");		
		for (DirectoryEntry dir : iterateDirectory()) {
			sb.append(dir.toString());
			sb.append("\n");
		}
		sb.append("]");

		return sb.toString();
	}

	protected DirectoryInode(long blockNr, int offset) {
		super(blockNr, offset);
	}

	public static DirectoryInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		DirectoryInode inode = new DirectoryInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	public void addDotLinks(DirectoryInode parent) throws IOException, FileExists, InvalidArgument, NoSpaceLeftOnDevice {
        DirectoryEntry dot = DirectoryEntry.create(".");
        DirectoryEntry dotdot = DirectoryEntry.create("..");
        
        dot.setFileType(DirectoryEntry.FILETYPE_DIR);
        dot.setIno(this.getIno());
        
        dotdot.setFileType(DirectoryEntry.FILETYPE_DIR);
        dotdot.setIno(parent.getIno());

        this.addLink(dot);
        this.addLink(dotdot);
	}
	/**
	 *  Create new empty directory. Don not add ., .. entries. Use addDotLinks()
	 */
	public static DirectoryInode createEmpty() throws IOException {
	    DirectoryInode inode = new DirectoryInode(-1, -1);
	    Date now = new Date();
	        
	    inode.setModificationTime(now);
	    inode.setAccessTime(now);
	    inode.setChangeTime(now);
	    inode.setDeletionTime(new Date(0));
        inode.setMode(Mode.IFDIR);
        inode.setBlock(new long[Constants.EXT2_N_BLOCKS]);
	    
	    return inode;
	}
	
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_DIR;
	}
	
	/**
	 * Unlink inode from directory. May cause inode destruction
	 */ 
	public void unLinkOther(Inode inode, String name) throws IOException {
        if (inode instanceof DirectoryInode)
            throw new IllegalArgumentException("Use unLinkDir for directories");
        unlink(inode, name);
	}
	
	/**
	 * Remove a subdirectory inode from directory. May cause indoe destruction 
	 */
	public void unLinkDir(DirectoryInode inode, String name) throws IOException, DirectoryNotEmpty {
	    if (!inode.isEmptyDirectory())
	        throw new DirectoryNotEmpty();
	    unlink(inode, name);
	}
	
	/**
	 * Unlink Inode from this directory. May cause freeInode() if link count
	 * reaches zero. 
	 */
	private void unlink(Inode inode, String name) throws IOException {
	    removeLink(name);
	    inode.setLinksCount(inode.getLinksCount() - 1);
	    
	    if (inode.getLinksCount() == 0) {
	        InodeAlloc.freeInode(inode);
	    }
	    
	}
	
	/**
	 * Remove a directory entry. This is probably not what you want. We don't 
	 * update the inode.linksCount here.
	 */
	private void removeLink(String name) throws IOException {
	    /* First: Find the entry and its predecessor */
	    DirectoryEntry prev = null;
	    DirectoryEntry toDelete = null;
	    for (DirectoryEntry current : iterateDirectory()) {
	        if (name.equals(current.getName())) {
	            toDelete = current;
	            break;
	        }
	        prev = current;
	    }

	    /* set the record length of the predecessor to skip the toDelete entry */ 
	    prev.setRecLen(prev.getRecLen() + toDelete.getRecLen());
	    prev.write();
	}
}