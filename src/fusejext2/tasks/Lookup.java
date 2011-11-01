package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryEntry;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.exceptions.JExt2Exception;
import fuse.Errno;
import fusejext2.Jext2Context;
import fusejext2.Util;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Lookup extends jlowfuse.async.tasks.Lookup<Jext2Context> {

	public Lookup(FuseReq req, long parent, String name) {
		super(req, parent, name);
	}

	public void run() {
		if (parent == 1) parent = Constants.EXT2_ROOT_INO;
		try {		
		    Inode parentInode = context.inodes.openInode(parent);
			if (!(parentInode instanceof DirectoryInode)) { 
				Reply.err(req, Errno.ENOTDIR);
				return;
			}
			
			DirectoryEntry entry = ((DirectoryInode)parentInode).lookup(name);			
			Inode child = context.inodes.openInode(entry.getIno());			
			Reply.entry(req, Util.inodeToEntryParam(context.superblock, child));			

		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }			
	}
}
