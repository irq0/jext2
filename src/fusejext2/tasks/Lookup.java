package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryEntry;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NotADirectory;
import fusejext2.Jext2Context;
import fusejext2.Util;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Lookup extends jlowfuse.async.tasks.Lookup<Jext2Context> {

	public Lookup(FuseReq req, long parent, String name) {
		super(req, parent, name);
	}

	@Override
	public void run() {
		long parent = this.parent;

		if (parent == 1) parent = Constants.EXT2_ROOT_INO;

		Inode parentInode = null;
		try {
			parentInode = context.inodes.openInode(parent);

			if (!parentInode.isDirectory())
				throw new NotADirectory();

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
			return;
		}

		DirectoryEntry entry = null;
		try {
			entry = ((DirectoryInode)parentInode).lookup(name);
			((DirectoryInode)parentInode).directoryEntries.release(entry);

			Inode child = context.inodes.openInode(entry.getIno());
			Reply.entry(req, Util.inodeToEntryParam(context.superblock, child));

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
