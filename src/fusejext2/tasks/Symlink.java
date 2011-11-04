package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.InodeAlloc;
import jext2.SymlinkInode;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FuseContext;
import fusejext2.Jext2Context;
import fusejext2.Util;

public class Symlink extends jlowfuse.async.tasks.Symlink<Jext2Context> {

	public Symlink(FuseReq arg0, String arg1, long arg2, String arg3) {
		super(arg0, arg1, arg2, arg3);
	}


	@Override
	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;
		try {
			DirectoryInode parentInode = (DirectoryInode)context.inodes.openInode(parent);

			FuseContext fuseContext = req.getContext();
			SymlinkInode inode = SymlinkInode.createEmpty();
			inode.setUid(fuseContext.getUid());
			inode.setGid(fuseContext.getGid());
			InodeAlloc.registerInode(parentInode, inode);
			inode.write();

			parentInode.addLink(inode, name);
			inode.setSymlink(link);

			Reply.entry(req, Util.inodeToEntryParam(context.superblock, inode));

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
