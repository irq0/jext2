package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.InodeAlloc;
import jext2.Mode;
import jext2.ModeBuilder;
import jext2.RegularInode;
import jext2.exceptions.FunctionNotImplemented;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NotADirectory;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FuseContext;
import fusejext2.Jext2Context;
import fusejext2.Util;

public class Mknod extends jlowfuse.async.tasks.Mknod<Jext2Context> {

	public Mknod(FuseReq req, long parent, String name, short mode, short rdev) {
		super(req, parent, name, mode, rdev);
	}

	@Override
	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;
		try {
			Mode createMode = Mode.createWithNumericValue(mode & 0xFFFF);

			/* Only support regular files */
			if (!createMode.isRegular())
				throw new FunctionNotImplemented();

			Inode parentInode = context.inodes.getOpened(parent);
			if (!parentInode.isDirectory())
				throw new NotADirectory();

			FuseContext fuseContext = req.getContext();
			RegularInode inode = RegularInode.createEmpty();
			inode.setMode(new ModeBuilder().regularFile()
					.numeric(createMode.numeric() & ~fuseContext.getUmask())
					.create() );

			inode.setUid(fuseContext.getUid());
			inode.setGid(fuseContext.getGid());
			InodeAlloc.registerInode(parentInode, inode);

			((DirectoryInode)parentInode).addLink(inode, name);
			inode.sync();

			Inode cached = context.inodes.openInode(inode.getIno());
			assert cached.equals(inode);

			Reply.entry(req, Util.inodeToEntryParam(context.superblock, inode));
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}
}
