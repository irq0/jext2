package fusejext2.tasks;

import jext2.Constants;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Fsyncdir extends jlowfuse.async.tasks.Fsync<Jext2Context> {

	public Fsyncdir(FuseReq arg0, long arg1, int arg2, FileInfo arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
			/*
			 * Do a full disk flush:
			 * Since we are in userland and have no control over blocks
			 * and disks, just sync the inode and force the file channel
			 * to flush
			 */

			Inode inode = context.inodes.openInode(ino);
			assert inode != null;

			inode.sync();
			context.blocks.sync();
			Reply.err(req, 0);
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
