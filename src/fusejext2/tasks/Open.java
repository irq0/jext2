package fusejext2.tasks;

import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.OperationNotPermitted;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Open extends jlowfuse.async.tasks.Open<Jext2Context> {

	public Open(FuseReq arg0, long arg1, FileInfo arg2) {
		super(arg0, arg1, arg2);
	}

	@Override
	public void run() {
		try {
			Inode inode = context.inodes.getOpened(ino);
			if (!inode.isRegularFile())
				throw new OperationNotPermitted();

			Reply.open(req, fi);
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
