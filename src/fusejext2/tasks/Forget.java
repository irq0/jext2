package fusejext2.tasks;

import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Forget extends jlowfuse.async.tasks.Forget<Jext2Context> {

	public Forget(FuseReq arg0, long arg1, long arg2) {
		super(arg0, arg1, arg2);
	}


	@Override
	public void run() {
		if (ino != 1)
			context.inodes.forgetInode(ino, nlookup);
		
		Reply.none(req);
	}
}
