package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryEntry;
import jext2.DirectoryInode;
import fuse.Dirbuf;
import fuse.FileInfo;
import fuse.FuseExtra;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;


public class Readdir extends jlowfuse.async.tasks.Readdir<Jext2Context> {

	public Readdir(FuseReq req, long ino, long size, long off, FileInfo fi) {
		super(req, ino, size, off, fi);
	}

	@Override
	public void run() {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;

		DirectoryInode inode = (DirectoryInode)(context.inodes.getOpened(ino));

		Dirbuf buf = new Dirbuf();

		for (DirectoryEntry d : inode.iterateDirectory()) {
			if (d.isUnused()) continue;
			FuseExtra.dirbufAdd(req,
					buf,
					d.getName(),
					d.getIno(),
					d.getFileType());
		}

		Reply.dirBufLimited(req, buf, off, size);
	}
}
