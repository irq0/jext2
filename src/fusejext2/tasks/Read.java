package fusejext2.tasks;

import java.nio.ByteBuffer;

import jext2.RegularInode;
import jext2.exceptions.JExt2Exception;

import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Read extends jlowfuse.async.tasks.Read<Jext2Context> {

	public Read(FuseReq req, long ino, long size, long off, FileInfo fi) {
		super(req, ino, size, off, fi);
	}

	@Override
	public void run() {
		try {
			RegularInode inode = (RegularInode)(context.inodes.getOpened(ino));
			// TODO the (int) cast is due to the java-no-unsigned problem. upgrade to java 1.7?
			ByteBuffer buf = inode.readData((int)size, off);
			Reply.byteBuffer(req, buf, 0, buf.limit());
		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}

	}

}
