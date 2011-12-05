package fusejext2.tasks;

import fuse.FileInfo;
import fusejext2.Jext2Context;
import jext2.Constants;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Releasedir extends jlowfuse.async.tasks.Releasedir<Jext2Context> {
	public Releasedir(FuseReq req, long ino, FileInfo fi) {
		super(req, ino, fi);
	}

	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;

		try {
			Inode inode = context.inodes.openInode(ino);
			inode.sync();
			Reply.err(req, 0);
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
