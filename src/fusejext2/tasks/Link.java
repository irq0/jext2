package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.exceptions.IsADirectory;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NotADirectory;
import fusejext2.Jext2Context;
import fusejext2.Util;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Link extends jlowfuse.async.tasks.Link<Jext2Context> {

	public Link(FuseReq arg0, long arg1, long arg2, String arg3) {
		super(arg0, arg1, arg2, arg3);
	}
	
	public void run() {
        if (ino == 1) ino = Constants.EXT2_ROOT_INO;
        try {
            Inode parent = context.inodes.get(newparent);
            if (!(parent instanceof DirectoryInode))
                throw new NotADirectory();
            
            Inode child = context.inodes.get(ino);
            if (child instanceof DirectoryInode)
                throw new IsADirectory();
            
            ((DirectoryInode)parent).addLink(child, newname);
            Reply.entry(req, Util.inodeToEntryParam(context.superblock, child));    

        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
	}

}