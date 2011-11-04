package fusejext2.tasks;

import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fusejext2.Jext2Context;

public class Access extends jlowfuse.async.tasks.Access<Jext2Context> {

	public Access(FuseReq arg0, long arg1, int arg2) {
		super(arg0, arg1, arg2);
	}

	@Override
	public void run() {
		/* Allow everything. Want permissions? Use -o default_permissions */
		Reply.err(req, 0);
	}
}