package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.Errno;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Opendir extends jlowfuse.async.tasks.Opendir<Jext2Context> {

	public Opendir(FuseReq arg0, long arg1, FileInfo arg2) {
		super(arg0, arg1, arg2);
	}

	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
            Inode inode = context.inodes.get(ino);
			if (!(inode instanceof DirectoryInode)) {
				Reply.err(req, Errno.ENOTDIR);
				return;
			}
			
			Reply.open(req, fi);

		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }	
	}
}
