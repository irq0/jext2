package fusejext2.tasks;

import jext2.Inode;
import jext2.RegularInode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.Errno;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Open extends jlowfuse.async.tasks.Open<Jext2Context> {

	public Open(FuseReq arg0, long arg1, FileInfo arg2) {
		super(arg0, arg1, arg2);
	}

	public void run() {
		try {
			Inode inode = context.inodes.get(ino);
			if (! (inode instanceof RegularInode)) {
			    Reply.err(req, Errno.EPERM);
			}
			
			Reply.open(req, fi);

		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }	
	}
}
