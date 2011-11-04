package fusejext2.tasks;

import jext2.Constants;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NotADirectory;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Opendir extends jlowfuse.async.tasks.Opendir<Jext2Context> {

	public Opendir(FuseReq arg0, long arg1, FileInfo arg2) {
		super(arg0, arg1, arg2);
	}

	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
			Inode inode = context.inodes.openInode(ino);
			if (!inode.isDirectory())
				throw new NotADirectory();

			Reply.open(req, fi);

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
