package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryEntry;
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

	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;
		if (newparent == 1) newparent = Constants.EXT2_ROOT_INO;

		try {
			DirectoryInode parentInode = (DirectoryInode)context.inodes.get(parent);
			DirectoryInode newparentInode = (DirectoryInode)context.inodes.get(newparent);

			/* create entries */
			DirectoryEntry entry = parentInode.lookup(name);

			DirectoryEntry newEntry = DirectoryEntry.create(newname);
			newEntry.setIno(entry.getIno());
			newEntry.setFileType(entry.getFileType());

			/* 
			 * When NEW directory entry already exists try to 
			 * delete it. 
			 */
			try {
				DirectoryEntry existingEntry = newparentInode.lookup(newname);
				if (existingEntry.getFileType() == DirectoryEntry.FILETYPE_DIR) {
					DirectoryInode existingDir = 
							(DirectoryInode)(context.inodes.get(existingEntry.getIno()));

					newparentInode.unLinkDir(existingDir, newname);
				} else {
					Inode existing = context.inodes.get(existingEntry.getIno());
					newparentInode.unLinkOther(existing, newname);
				}
			} catch (NoSuchFileOrDirectory ignored) {
			}

			/*
			 * When we move a directory we need to update the dot-dot entry
			 * and the nlinks of the parents.
			 */
			if (newEntry.getFileType() == DirectoryEntry.FILETYPE_DIR) {
				DirectoryInode newDir = 
						(DirectoryInode)(context.inodes.get(newEntry.getIno()));

				DirectoryEntry dotdot = newDir.lookup("..");
				dotdot.setIno(newparentInode.getIno());
				dotdot.sync();

				newparentInode.setLinksCount(newparentInode.getLinksCount() + 1);
				parentInode.setLinksCount(parentInode.getLinksCount() - 1);
			}

			/*
			 * Finally: Change the Directories 
			 */
			newparentInode.addDirectoryEntry(newEntry);
			parentInode.removeDirectoryEntry(name);

			Reply.err(req, 0);
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
