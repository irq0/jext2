package fusejext2.tasks;

import jext2.Constants;
import jext2.DirectoryInode;
import jext2.Inode;
import jext2.InodeAlloc;
import jext2.ModeBuilder;
import jext2.exceptions.JExt2Exception;
import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.Errno;
import fuse.FuseContext;
import fusejext2.Jext2Context;
import fusejext2.Util;

public class Mkdir extends jlowfuse.async.tasks.Mkdir<Jext2Context> {

	public Mkdir(FuseReq req, long parent, String name, short mode) {
		super(req, parent, name, mode);
	}

	public void run() {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            Inode parentInode = context.inodes.openInode(parent);
            if (!(parentInode instanceof DirectoryInode)) {
                Reply.err(req, Errno.ENOTDIR);
                return;
            }

            FuseContext fuseContext = req.getContext();            
            DirectoryInode inode = 
                DirectoryInode.createEmpty();            
            inode.setMode(ModeBuilder.directory()
		            .mask(mode & ~fuseContext.getUmask())
		            .create());
            inode.setUid(fuseContext.getUid());
            inode.setGid(fuseContext.getGid());
            InodeAlloc.registerInode(parentInode, inode);
            inode.addDotLinks((DirectoryInode)parentInode);
            
            ((DirectoryInode)parentInode).addLink(inode, name);
            inode.sync();
            Reply.entry(req, Util.inodeToEntryParam(context.superblock, inode));    

        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
	}
}
