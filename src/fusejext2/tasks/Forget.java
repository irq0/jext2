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

		

		assert context.inodes.getOpened(ino) != null : "Inode allready flushed from cache";

		assert context.inodes.retainCount(ino) >= nlookup : "Can't forget more than the retains stored";
		
		System.out.println("nlookup: " + nlookup + "retain: " + context.inodes.retainCount(ino));

		context.inodes.retainInode(ino);
		context.inodes.forgetInode(ino, nlookup);
		
		Reply.none(req);
	}
}
