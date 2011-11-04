package jext2;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jext2.annotations.NotThreadSafe;
import jext2.exceptions.DirectoryNotEmpty;
import jext2.exceptions.FileExists;
import jext2.exceptions.FileNameTooLong;
import jext2.exceptions.FileTooLarge;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NoSpaceLeftOnDevice;
import jext2.exceptions.NoSuchFileOrDirectory;
import jext2.exceptions.TooManyLinks;


/** Inode for directories */
public class DirectoryInode extends DataInode {
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static Superblock superblock = Superblock.getInstance();

	private DirectoryEntryAccess directoryEntries = DirectoryEntryAccess.createForDirectoy(this);

	private ReentrantReadWriteLock directoryLock =
			new ReentrantReadWriteLock(true);

	/**
	 * Lock to use when iterating a directory with {@link #iterateDirectory()}
	 */
	public ReentrantReadWriteLock.ReadLock lockForIterateDirectory() {
		return directoryLock.readLock();
	}

	/**
	 * Get the directory iterator. Please note: A directory entry
	 * is everything found that fits the datastructure. Though "fill" entries
	 * are allways included. You may want do do a #DirectoryEntry.isUnused
	 * to check that.
	 *
	 * While using the DirectoryIterator you must hold the lock provided
	 * by {@link #lockForIterateDirectory()}
	 * @returns Directory iterator for this inode
	 */
	@NotThreadSafe(useLock=true)
	public DirectoryIterator iterateDirectory() {
		return new DirectoryIterator();
	}

	/**
	 * Iterate directory entries.
	 */
	public class DirectoryIterator implements Iterator<DirectoryEntry>, Iterable<DirectoryEntry> {
		private ByteBuffer block;
		private long blockNr;
		private DirectoryEntry entry;
		private DirectoryEntry previousEntry;
		private int offset = 0;

		private Iterator<Long> blockIter;

		DirectoryIterator() {
		    blockIter = accessData().iterateBlocks();
		    this.previousEntry = null;
			this.entry = fetchNextEntry(null);
		}

		@Override
		public boolean hasNext() {
			return (entry != null);
		}

		private DirectoryEntry fetchNextEntry(DirectoryEntry last) {
			try {
			    if (last == null && block == null) {
			        if (!blockIter.hasNext())
			            throw new RuntimeException("DirectoryInode whithout data blocks!");

			        blockNr = blockIter.next();
			        block = blocks.read(blockNr);

			        DirectoryEntry entry = DirectoryEntry.fromByteBuffer(block, blockNr, 0);
			        directoryEntries.add(entry);
			        directoryEntries.retain(entry);
			        return entry;
			    }

				assert last != null;
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
		        DirectoryEntry entry = DirectoryEntry.fromByteBuffer(block, blockNr, offset);
		        directoryEntries.add(entry);
		        directoryEntries.retain(entry);
		        return entry;
			} catch (IoError e) {
				return null;
			}
		}

		@Override
		public DirectoryEntry next() {
			directoryEntries.release(previousEntry);

			DirectoryEntry result = this.entry;
			this.previousEntry = this.entry;
			this.entry = fetchNextEntry(entry);
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

        @Override
		public Iterator<DirectoryEntry> iterator() {
            return this;
        }

	}

	/**
	 * Add directory entry for given inode and name.
	 * @throws NoSpaceLeftOnDevice
	 * @throws FileNameTooLong
	 * @throws FileTooLarge Allocating a new block would hit the max. block count
	 * @throws TooManyLinks When adding a link would cause nlinks to hit the limit.
	 * Checkt is performed before any allocation.
	 * @see addLink(DirectoryEntry newEntry)
	 */
	public void addLink(Inode inode, String name) throws JExt2Exception, FileExists, NoSpaceLeftOnDevice, FileNameTooLong, TooManyLinks, FileTooLarge {
	    if (inode.getLinksCount() >= Constants.EXT2_LINK_MAX)
	        throw new TooManyLinks();

	    DirectoryEntry newDir = DirectoryEntry.create(name);
        newDir.setIno(inode.getIno());
        newDir.setFileType(inode.getFileType());

        addDirectoryEntry(newDir);

        inode.setLinksCount(inode.getLinksCount() + 1);
	}

	/**
	 * Add directory entry. Iterate over data blocks and check for entries with
	 * the same name on the way. This function does the heavy lifting compared to #addLink(Inode, String)
	 * and should never be used directly.
	 * @throws NoSpaceLeftOnDevice
	 * @throws FileTooLarge Allocating a new block would hit the max. block count
	 * @throws FileExistsException      When we stumble upon an entry with same name
	 */
	// TODO rewrite addLink to use the directory iterator
	public void addDirectoryEntry(DirectoryEntry newEntry) throws JExt2Exception, FileExists, NoSpaceLeftOnDevice, FileTooLarge {
       ByteBuffer block;
       int offset = 0;

       directoryEntries.retain(newEntry);

       // Using a write lock for the whole process is a bit much but easy..
       directoryLock.writeLock().lock();

       for (long blockNr : accessData().iterateBlocks()) {
           block = blocks.read(blockNr);

           while (offset + 8 <= block.limit()) { /* space for minimum of one entry */
               DirectoryEntry currentEntry = DirectoryEntry.fromByteBuffer(block, blockNr, offset);
               directoryEntries.add(currentEntry);
               directoryEntries.retain(currentEntry);

               if (currentEntry.getName().equals(newEntry.getName()))
                   throw new FileExists();

               if (currentEntry.getRecLen() == 0 ||
                       currentEntry.getRecLen() > superblock.getBlocksize()) {
                   throw new RuntimeException
                       ("zero-length or bigger-than-blocksize directory entry"
                               + "entry: " + currentEntry);
               }

               /*
                * See if current directory entry is unused; if so:
                * assimilate!
                */
               if (currentEntry.isUnused() &&
                   currentEntry.getRecLen() >= newEntry.getRecLen()) {

                   newEntry.setRecLen(currentEntry.getRecLen());
                   blocks.writePartial(blockNr, offset, newEntry.toByteBuffer());

                   setModificationTime(new Date()); // should be handeld by block layer
                   setStatusChangeTime(new Date());

                   directoryEntries.release(currentEntry);
                   directoryEntries.release(newEntry);
                   directoryLock.writeLock().unlock();
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

                   setModificationTime(new Date());
                   setStatusChangeTime(new Date());
                   directoryEntries.release(currentEntry);
                   directoryEntries.release(newEntry);
                   directoryLock.writeLock().unlock();
                   return;
               }
               directoryEntries.release(currentEntry);

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
           throw new IoError();
       long blockNr = allocBlocks.getFirst();

       blocks.writePartial(blockNr, 0, newEntry.toByteBuffer());

       DirectoryEntry rest = DirectoryEntry.createRestDummy(newEntry);
       blocks.writePartial(blockNr, newEntry.getRecLen(), rest.toByteBuffer());

       setSize(getSize() + superblock.getBlocksize());
       setStatusChangeTime(new Date());

       directoryEntries.release(newEntry);
       directoryLock.writeLock().unlock();
	}


	public boolean isEmptyDirectory() {
	    int count = 0;
	    directoryLock.readLock().lock();
	    for (@SuppressWarnings("unused") DirectoryEntry dir : iterateDirectory()) {
	        count += 1;
	        if (count >= 3) {
	    	    directoryLock.readLock().unlock();
	            return false;
	        }
	    }
	    directoryLock.readLock().unlock();
	    return true;
	}


	/**
	 * Lookup name in directory. This is done by iterating each entry and
	 * comparing the names.
	 *
	 * @return DirectoryEntry or null in case its not found
	 * @throws NoSuchFileOrDirectory
	 * @throws FileNameTooLong
	 */
	public DirectoryEntry lookup(String name) throws NoSuchFileOrDirectory, FileNameTooLong {
	    if (Ext2fsDataTypes.getStringByteLength(name) > DirectoryEntry.MAX_NAME_LEN)
	        throw new FileNameTooLong();

	    directoryLock.readLock().lock();
		for (DirectoryEntry dir : iterateDirectory()) {
			directoryEntries.retain(dir);
			if (name.equals(dir.getName())) {
			    directoryLock.readLock().unlock();
				return dir;
			}
			directoryEntries.release(dir);
		}

	    directoryLock.readLock().unlock();
		throw new NoSuchFileOrDirectory();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());

		sb.append(" DIRECTORY=[");

	    directoryLock.readLock().lock();
		for (DirectoryEntry dir : iterateDirectory()) {
			directoryEntries.retain(dir);
			sb.append(dir.toString());
			sb.append("\n");
			directoryEntries.release(dir);
		}
	    directoryLock.readLock().unlock();

		sb.append("]");

		return sb.toString();
	}

	protected DirectoryInode(long blockNr, int offset) {
		super(blockNr, offset);
	}

	public static DirectoryInode fromByteBuffer(ByteBuffer buf, int offset) throws IoError {
		DirectoryInode inode = new DirectoryInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	public void addDotLinks(DirectoryInode parent)
	        throws JExt2Exception, FileExists, NoSpaceLeftOnDevice, TooManyLinks, FileTooLarge {
	    try {
	        addLink(this, ".");
	        addLink(parent, "..");
	    } catch (FileNameTooLong e) {
	        throw new RuntimeException("should not happen");
        }
	}

	/**
	 * Remove "." and ".." entry from the directory. Should be called before
	 * a unlink on a directory.
	 * @throws NoSuchFileOrDirectory
	 * @praram parent parent inode. Used to resolve the ".." link
	 */
	public void removeDotLinks(DirectoryInode parent) throws IoError, NoSuchFileOrDirectory {
	    removeDirectoryEntry(".");
	    removeDirectoryEntry("..");

	    setStatusChangeTime(new Date());

	    setLinksCount(getLinksCount() - 1);
	    parent.setLinksCount(parent.getLinksCount() - 1);

//	    write();
//	    parent.write();
	}


	/**
	 *  Create new empty directory. Don't add "." and .." entries.
	 *  Use #addDotLinks() for this.
	 */
	public static DirectoryInode createEmpty() throws IoError {
	    DirectoryInode inode = new DirectoryInode(-1, -1);
	    Date now = new Date();

	    inode.setModificationTime(now);
	    inode.setAccessTime(now);
	    inode.setStatusChangeTime(now);
	    inode.setDeletionTime(new Date(0));
        inode.setMode(new ModeBuilder().directory().create());
        inode.setBlock(new long[Constants.EXT2_N_BLOCKS]);

	    return inode;
	}

	@Override
	public short getFileType() {
	    return DirectoryEntry.FILETYPE_DIR;
	}

    @Override
	public boolean isSymlink() {
    	return false;
    }

    @Override
	public boolean isDirectory() {
    	return true;
    }

    @Override
	public boolean isRegularFile() {
    	return false;
    }

    /**
	 * Unlink inode from directory. May cause inode destruction. Inode can
	 * be any kind of inode except directories.
	 *
	 * @param  inode   inode to unlink
	 * @param  name    name of the directory entry
	 */
	public void unLinkOther(Inode inode, String name) throws JExt2Exception {
        if (inode.isDirectory())
            throw new IllegalArgumentException("Use unLinkDir for directories");
        unlink(inode, name);
	}

	/**
	 * Remove a subdirectory inode from directory. May cause inode destruction
	 *
	 * @see #isEmptyDirectory()
	 * @param  inode   Subdirectory inode to be unlinked
	 * @param  name    name of the directory entry
	 * @throws DirectoryNotEmpty Well, you can't unlink non-empty directories.
	 *  "." and ".." entries don't count.
	 */
	public void unLinkDir(DirectoryInode inode, String name) throws JExt2Exception, DirectoryNotEmpty {
	    if (!inode.isEmptyDirectory())
	        throw new DirectoryNotEmpty();
	    unlink(inode, name);
	}

	/**
	 * Unlink Inode from this directory. May delete associated inode if link count
	 * reaches zero.
	 * @throws JExt2Exception
	 */
	private void unlink(Inode inode, String name) throws JExt2Exception {
	    removeDirectoryEntry(name);
	    inode.setLinksCount(inode.getLinksCount() - 1);
	    setStatusChangeTime(new Date());

	    if (inode.getLinksCount() <= 0) {
	        inode.delete();
	    }

	}

	/**
	 * Remove a directory entry. This is probably not what you want. We don't
	 * update the inode.linksCount here.
	 * @see #unlink(Inode inode, String name)
	 * @param name Name of the entry
	 * @throws NoSuchFileOrDirectory
	 */
	public void removeDirectoryEntry(String name) throws IoError, NoSuchFileOrDirectory {

	    /* First: Find the entry and its predecessor */
		directoryLock.readLock().lock();
	    DirectoryEntry prev = null;
	    DirectoryEntry toDelete = null;
	    for (DirectoryEntry current : iterateDirectory()) {
			directoryEntries.retain(current);
	        if (name.equals(current.getName())) {
	            toDelete = current;
	            break;
	        }
			directoryEntries.release(prev);
	        prev = current;
	    }

	    assert directoryEntries.usageCounter(prev) > 0;
	    assert directoryEntries.usageCounter(toDelete) > 0;

	    directoryLock.readLock().unlock();

	    /* Another thread could have deleted the entry */
	    if (toDelete.isUnused()) {
	    	directoryEntries.release(toDelete);
	    	throw new NoSuchFileOrDirectory();
	    }
	    /*
	     * When we are at the beginning of a block there is
	     * no prev entry we can use
	     */
		assert toDelete != null;
		directoryLock.writeLock().lock();
		if (toDelete.getOffset() == 0) {
	        toDelete.setIno(0);
	        toDelete.clearName();
	        toDelete.setFileType(DirectoryEntry.FILETYPE_UNKNOWN);
	        toDelete.write();
	    /*
	     * set the record length of the predecessor to skip
	     * the toDelete entry
	     */
	    } else {
		    assert prev != null;
		    prev.setRecLen(prev.getRecLen() + toDelete.getRecLen());
	        prev.write(); // ok here: is meta data
	    }

    	directoryEntries.release(toDelete);
    	directoryEntries.release(prev);

    	directoryLock.writeLock().unlock();
	    setModificationTime(new Date());

	}
}