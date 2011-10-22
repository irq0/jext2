package fusejext2.tasks;

import jext2.Constants;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Release extends jlowfuse.async.tasks.Release<Jext2Context> {

	public Release(FuseReq arg0, long arg1, FileInfo arg2) {
		super(arg0, arg1, arg2);
	}

	
	public void run() {
	    if (ino == 1) ino = Constants.EXT2_ROOT_INO;
	    
	    try {
	        Inode inode = context.inodes.get(ino);
	        inode.sync();
	        Reply.err(req, 0);
	    } catch (JExt2Exception e) {
	        Reply.err(req, e.getErrno());
	    }	
	}
}