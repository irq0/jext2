package fusejext2.tasks;

import jext2.Constants;
import jext2.Inode;
import jext2.SymlinkInode;
import jext2.exceptions.JExt2Exception;
import fuse.Errno;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Readlink extends jlowfuse.async.tasks.Readlink<Jext2Context> {

	public Readlink(FuseReq arg0, long arg1) {
		super(arg0, arg1);
	}

	public void run() {
	    if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
		    Inode inode = context.inodes.openInode(ino);
			if (!inode.isSymlink()) { 
				Reply.err(req, Errno.EPERM);
				return;
			}
			
			Reply.readlink(req, ((SymlinkInode)inode).getSymlink());			
			
		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
		
	}
}
