package fusejext2.tasks;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import jext2.Constants;
import jext2.DirectoryEntry;
import jext2.DirectoryEntryAccess;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NoSuchFileOrDirectory;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Rename extends jlowfuse.async.tasks.Rename<Jext2Context> {

	public Rename(FuseReq req, long parent, String name, long newparent, String newname) {
		super(req, parent, name, newparent, newname);
	}
	
	/* XXX this is probably broken */

	@Override
	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;
		if (newparent == 1) newparent = Constants.EXT2_ROOT_INO;

		ReentrantReadWriteLock parentLock = null;
		ReentrantReadWriteLock newparentLock = null;
		
		try {
			DirectoryInode parentInode;
			DirectoryInode newparentInode; 
			try {
				parentInode = (DirectoryInode)context.inodes.openInode(parent);
				newparentInode = (DirectoryInode)context.inodes.openInode(newparent);
			} catch (JExt2Exception e) {
				throw new RuntimeException("If this ever happens rethink rename command");
			}
			
			parentLock = parentInode.directoryLock();
			newparentLock = newparentInode.directoryLock();
			
			if (parentInode.equals(newparentInode)) {
				parentLock.writeLock().lock();
			} else {
				parentLock.writeLock().lock();
				newparentLock.writeLock().lock();
			}
			
			DirectoryEntryAccess parentEntries = ((DirectoryInode)parentInode).directoryEntries;
			DirectoryEntryAccess newparentEntries = ((DirectoryInode)newparentInode).directoryEntries;		
			
			/* create entries */
			DirectoryEntry entry = parentInode.lookup(name);

			DirectoryEntry newEntry = DirectoryEntry.create(newname);
			newEntry.setIno(entry.getIno());
			newEntry.setFileType(entry.getFileType());

			parentEntries.release(entry);

			/*
			 * When NEW directory entry already exists try to
			 * delete it.
			 */
			try {
				DirectoryEntry existingEntry = newparentInode.lookup(newname);
				if (existingEntry.isDirectory()) {
					DirectoryInode existingDir =
							(DirectoryInode)(context.inodes.openInode(existingEntry.getIno()));

					newparentInode.unLinkDir(existingDir, newname);
				} else {
					Inode existing = context.inodes.openInode(existingEntry.getIno());
					newparentInode.unLinkOther(existing, newname);
				}
				newparentEntries.release(existingEntry);
			} catch (NoSuchFileOrDirectory ignored) {
			}

			/*
			 * When we move a directory we need to update the dot-dot entry
			 * and the nlinks of the parents.
			 */
			if (newEntry.isDirectory()) {
				DirectoryInode newDir =
						(DirectoryInode)(context.inodes.openInode(newEntry.getIno()));

				DirectoryEntry dotdot = newDir.lookup("..");
				dotdot.setIno(newparentInode.getIno());
				dotdot.sync();

				newparentInode.setLinksCount(newparentInode.getLinksCount() + 1);
				parentInode.setLinksCount(parentInode.getLinksCount() - 1);
				
				newDir.directoryEntries.release(dotdot);
			}

			/*
			 * Finally: Change the Directories
			 */
			newparentInode.addDirectoryEntry(newEntry);
			parentInode.removeDirectoryEntry(name);

			assert parentLock.getWriteHoldCount() == 1;
			assert newparentLock.getWriteHoldCount() == 1;

			if (parentInode.equals(newparentInode)) {
				parentLock.writeLock().unlock();
			} else {
				parentLock.writeLock().unlock();
				newparentLock.writeLock().unlock();
			}
				
			Reply.err(req, 0);
		} catch (JExt2Exception e) {
			assert parentLock.getWriteHoldCount() == 1;
			assert newparentLock.getWriteHoldCount() == 1;

			if (parentLock.equals(newparentLock)) {
				parentLock.writeLock().unlock();
			} else {
				parentLock.writeLock().unlock();
				newparentLock.writeLock().unlock();
			}
			Reply.err(req, e.getErrno());
		}
	}
}
