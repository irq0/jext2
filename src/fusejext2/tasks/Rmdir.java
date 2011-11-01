package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.exceptions.JExt2Exception;
import fuse.Errno;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Rmdir extends jlowfuse.async.tasks.Rmdir<Jext2Context> {

	public Rmdir(FuseReq req, long parent, String name) {
		super(req, parent, name);
	}
	
	public void run() {
        if (name.equals(".") || name.equals("..")) {
            Reply.err(req, Errno.EINVAL);
            return;
        }
        
        if (parent == 1) { 
            parent = Constants.EXT2_ROOT_INO;
        }
        
        try {
            DirectoryInode parentInode = (DirectoryInode)(context.inodes.openInode(parent));
            DirectoryInode child = 
                (DirectoryInode)context.inodes.openInode(parentInode.lookup(name).getIno());
            
            child.removeDotLinks(parentInode);
            parentInode.unLinkDir(child, name);

            Reply.err(req, 0);

        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }    
	}

}
