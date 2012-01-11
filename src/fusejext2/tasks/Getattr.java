package fusejext2.tasks;

import jext2.Constants;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fuse.Stat;
import fusejext2.Jext2Context;
import fusejext2.Util;


public class Getattr extends jlowfuse.async.tasks.Getattr<Jext2Context> {

	public Getattr(FuseReq req, long ino, FileInfo fi) {
		super(req, ino, fi);
	}


	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		Inode inode;
		try {
			inode = context.inodes.openInode(ino);
	 		Stat stat = Util.inodeToStat(context.superblock, inode);
			Reply.attr(req, stat, 0.0);
			context.inodes.forgetInode(ino, 1);
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}

	}
}