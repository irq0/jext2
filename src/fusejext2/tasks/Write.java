package fusejext2.tasks;
import java.nio.ByteBuffer;

import jext2.RegularInode;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;

import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Write extends jlowfuse.async.tasks.Write<Jext2Context> {

	public Write(FuseReq req, long ino, ByteBuffer buf, long off, FileInfo fi) {
		super(req, ino, buf, off, fi);
	}
	
	public void run() {
	    try {
	        RegularInode inode = (RegularInode)(context.inodes.getOpen(ino));
	        buf.rewind();
	        int count = inode.writeData(buf, off);
	        
	        if (count < 1) 
	            throw new IoError();
	        
	        Reply.write(req, count);
	    
	    } catch (JExt2Exception e) {
	        Reply.err(req, e.getErrno());
	    }
	}

}
