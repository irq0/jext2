package fusejext2.tasks;

import fusejext2.Jext2Context;
import jext2.Constants;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Forget extends jlowfuse.async.tasks.Forget<Jext2Context> {

	public Forget(FuseReq arg0, long arg1, long arg2) {
		super(arg0, arg1, arg2);
	}


	@Override
	public void run() {

		if (ino == 1) ino = Constants.EXT2_ROOT_INO;

		// try to sync if inode is open - which it shouldn't be
		try {
			Inode inode = context.inodes.getOpened(ino);
			if (inode != null)
				inode.sync();
		} catch (JExt2Exception ignored) {
		}


		context.inodes.closeInode(ino);
		Reply.none(req);

	}
}
