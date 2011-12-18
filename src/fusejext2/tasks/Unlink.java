package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.InodeAccess;
import jext2.exceptions.InvalidArgument;
import jext2.exceptions.JExt2Exception;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Unlink extends jlowfuse.async.tasks.Unlink<Jext2Context> {

	public Unlink(FuseReq req, long parent, String name) {
		super(req, parent, name);
	}

	@Override
	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;

		try {
			if (name.equals(".") || name.equals(".."))
				throw new InvalidArgument();

			DirectoryInode parentInode = (DirectoryInode)(context.inodes.openInode(parent));
			Inode child =
					context.inodes.openInode(parentInode.lookup(name).getIno());

			parentInode.unLinkOther(child, name);

			Reply.err(req, 0);

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}

}
