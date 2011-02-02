package fusejext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import jext2.*;
import jext2.exceptions.FileTooLarge;
import jext2.exceptions.JExt2Exception;
import fuse.*;
import jlowfuse.*;
import fuse.StatVFS;

public class JExt2Ops extends AbstractLowlevelOps {

	private BlockAccess blocks;
	private Superblock superblock;
	private BlockGroupAccess blockGroups;

	private FileChannel blockDev;
	
	private InodeAccessProvider inodes = new InodeAccessProvider();
	
	public JExt2Ops(FileChannel blockDev) {
		this.blockDev = blockDev;
	}
	
	public void init() {
		try {
			blocks = new BlockAccess(blockDev);
			superblock = Superblock.fromBlockAccess(blocks);
			blocks.initialize(superblock);

	        if (Feature.incompatUnsupported() || Feature.roCompatUnsupported()) {
	            System.out.println("Featureset incompatible with JExt2 :(");
	            System.exit(23);
	        }      
	        
	        if (superblock.getMagic() != 0xEF53) {
	            System.out.println("Wrong magic -> no ext2");
	            System.exit(23);
	        }
	        
	        // TODO set times, volume name, etc in  superblock
	        
			blockGroups = new BlockGroupAccess();
			blockGroups.readDescriptors();
		} catch (IOException e) {
			System.out.println("init() failed :(");
			System.out.println(23);
		}
		
	}
	
	private Date timespecToDate(Timespec time) {
	    Date date = new Date(time.getSec()*1000 + time.getNsec()/1000); 
	    return date;
	}
	
	private Timespec dateToTimespec(Date date) {
	    Timespec tim = new Timespec();
	    tim.setSec((int)(date.getTime() / 1000));
	    tim.setNsec(0);
	    return tim;
	}    
	    
	private EntryParam makeEntryParam(Inode inode) {
        EntryParam e = new EntryParam();
        e.setAttr(makeStat(inode));
        e.setGeneration(inode.getGeneration());
        e.setAttr_timeout(0.0);
        e.setEntry_timeout(0.0);
        e.setIno(inode.getIno());
        return e;
	}
	
	private Stat makeStat(Inode inode) {
		Stat s = new Stat();
		s.setUid(inode.getUidLow());
		s.setGid(inode.getGidLow());
		s.setIno(inode.getIno());
		s.setMode(inode.getMode());
		s.setBlksize(superblock.getBlocksize());
		s.setNlink(inode.getLinksCount());
		s.setSize(inode.getSize());
		
		if (inode instanceof DataInode) 
		    s.setBlocks(((DataInode)inode).getBlocks());
		else 
		    s.setBlocks(0);
		
		s.setAtim(dateToTimespec(inode.getAccessTime()));
		s.setCtim(dateToTimespec(inode.getStatusChangeTime()));
		s.setMtim(dateToTimespec(inode.getModificationTime()));
		
		return s;
	}		

	public void open(FuseReq req, long ino, FileInfo fi) {
		try {
			Inode inode = inodes.get(ino);
			if (! (inode instanceof RegularInode)) {
			    Reply.err(req, Errno.EPERM);
			}
			
			Reply.open(req, fi);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
	}
	
	public void read(FuseReq req, long ino, long size, long off, FileInfo fi) {
		try {
		    RegularInode inode = (RegularInode)(inodes.getOpen(ino));
		    // TODO the (int) cast is due to the java-no-unsigned problem. handle this better here
			ByteBuffer buf = inode.readData((int)size, off);
			Reply.byteBuffer(req, buf, 0, size);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		} catch (JExt2Exception e) {
		    Reply.err(req, e.getErrno());
		}
		
	}
	
	
	public void write(FuseReq req, long ino, ByteBuffer buf, long off, FileInfo fi) {
	    try {
	        RegularInode inode = (RegularInode)(inodes.getOpen(ino));
	        buf.rewind();
	        int count = inode.writeData(buf, off);
	        
	        if (count < 1) 
	            throw new IOException();
	        
	        Reply.write(req, count);
	    
	    } catch (IOException e) {
	        Reply.err(req, Errno.EIO);
	    } catch (JExt2Exception e) {
	        Reply.err(req, e.getErrno());
	    }
	    
	}
	
	public void release(FuseReq req, long ino, FileInfo fi) {
	    inodes.close(ino);
		Reply.err(req, 0);
	}
	
	public void readlink(FuseReq req, long ino) {
	    if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
		    Inode inode = inodes.get(ino);
			if (!(inode instanceof SymlinkInode)) { 
				Reply.err(req, Errno.EPERM);
				return;
			}
			
			Reply.readlink(req, ((SymlinkInode)inode).getSymlink());			
			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
		
	}
	
	public void flush(FuseReq req, long ino, FileInfo fi) {
		Reply.err(req, 0);
	}
	
	public void getattr(FuseReq req, long ino, FileInfo fi) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
		    Inode inode = inodes.get(ino);
			Stat stat = makeStat(inode);
			
			System.out.println("inode=" + ino + " isize=" +inode.getSize() + " st_size=" + stat.getSize());
			
			Reply.attr(req, stat, 0.0);
			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
	}

	private boolean checkToSet(int to_set, int attr) {
	    return ((to_set & attr) != 0);
	}
	

    public void setattr(FuseReq req, long ino, Stat attr, int to_set, FileInfo fi) {
        if (ino == 1) ino = Constants.EXT2_ROOT_INO;
        
        try {
            Inode inode = inodes.get(ino);

            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_ATIME)) {
                inode.setAccessTime(timespecToDate(attr.getAtim()));
            } 
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_ATIME_NOW)) {
                inode.setAccessTime(new Date());
            }
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_GID)) {
                inode.setGid(attr.getGid());
            }            
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_MODE)) {
                inode.setMode((int)attr.getMode());
            }            
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_MTIME)) {
                inode.setModificationTime(timespecToDate(attr.getMtim()));
            }            
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_MTIME_NOW)) {
                inode.setModificationTime(new Date());
            }            
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_SIZE)) {
                if (inode instanceof RegularInode) {
                    ((RegularInode) inode).setSizeAndTruncate(attr.getSize());
                }
            }            
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_UID)) {
                inode.setUid(attr.getUid());
            }            
            
            inode.write();
            
            Stat s = makeStat(inode);
            Reply.attr(req, s, 0.0);
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
    }

	
	public void lookup(FuseReq req, long ino, String name) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {		
		    Inode parent = inodes.get(ino);
			if (!(parent instanceof DirectoryInode)) { 
				Reply.err(req, Errno.ENOTDIR);
				return;
			}
			
			DirectoryEntry entry = ((DirectoryInode)parent).lookup(name);			
			Inode child = inodes.get(entry.getIno());			
			Reply.entry(req, makeEntryParam(child));			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }			
	}
	
	
	public void opendir(FuseReq req, long ino, FileInfo fi) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
            Inode inode = inodes.get(ino);
			if (!(inode instanceof DirectoryInode)) {
				Reply.err(req, Errno.ENOTDIR);
				return;
			}
			
			Reply.open(req, fi);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		} catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
	}
	
	
	public void readdir(FuseReq req, long ino, long size, long off, FileInfo fi) {
	    if (ino == 1) ino = Constants.EXT2_ROOT_INO;

        DirectoryInode inode = (DirectoryInode)(inodes.getOpen(ino));
        
		Dirbuf buf = new Dirbuf();

		for (DirectoryEntry d : inode.iterateDirectory()) {
			FuseExtra.dirbufAdd(req, 
					buf,
					d.getName(),
					d.getIno(),
					d.getFileType());
		}

		Reply.dirBufLimited(req, buf, off, size);
	}
	
	public void releasedir(FuseReq req, long ino, FileInfo fi) {
	    if (ino == 1) ino = Constants.EXT2_ROOT_INO;
        inodes.close(ino);
		Reply.err(req, 0);		
	}

    public void mknod(FuseReq req, long parent, String name, short umode, short dev) {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            int mode = umode & 0xFFFF;

            /* Only support regular files */
            if (! Mode.isRegular(mode)) {
                Reply.err(req, Errno.ENOSYS);
                return;
            }

            Inode parentInode = inodes.get(parent);
            if (!(parentInode instanceof DirectoryInode)) {
                Reply.err(req, Errno.ENOTDIR);
                return;
            }
            
            FuseContext context = req.getContext();           
            RegularInode inode = RegularInode.createEmpty();            
            inode.setMode(Mode.IFREG | (mode & ~(int)(context.getUmask())));
            inode.setUid(context.getUid());
            inode.setGid(context.getGid());
            InodeAlloc.registerInode(parentInode, inode);
            inode.write();
            
            ((DirectoryInode)parentInode).addLink(inode, name);            
            Reply.entry(req, makeEntryParam(inode));    
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
	
    }	
    public void mkdir(FuseReq req, long parent, String name, short mode) {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            Inode parentInode = inodes.get(parent);
            if (!(parentInode instanceof DirectoryInode)) {
                Reply.err(req, Errno.ENOTDIR);
                return;
            }

            FuseContext context = req.getContext();            
            DirectoryInode inode = 
                DirectoryInode.createEmpty();            
            inode.setMode(Mode.IFDIR | (mode & ~(int)(context.getUmask())));
            inode.setUid(context.getUid());
            inode.setGid(context.getGid());
            InodeAlloc.registerInode(parentInode, inode);
            inode.addDotLinks((DirectoryInode)parentInode);
            inode.write();
            
            ((DirectoryInode)parentInode).addLink(inode, name);
            Reply.entry(req, makeEntryParam(inode));    
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
    }
	
	
    public void statfs(FuseReq req, long ino) {
        StatVFS s = new StatVFS();

        s.setBsize(superblock.getBlocksize());
        s.setFrsize(superblock.getBlocksize());
        s.setBlocks(superblock.getBlocksCount() - superblock.getOverhead());
        s.setBfree(superblock.getFreeBlocksCount());
        
        if (s.getBfree() >= superblock.getResevedBlocksCount())
            s.setBavail(superblock.getFreeBlocksCount() - superblock.getResevedBlocksCount());
        else 
        	s.setBavail(0);
        
        s.setFiles(superblock.getInodesCount());
        s.setFfree(superblock.getFreeInodesCount());
        s.setFavail(superblock.getFreeInodesCount());
        
        s.setFsid(0); 
        s.setFlag(0);
        s.setNamemax(DirectoryEntry.MAX_NAME_LEN);

        Reply.statfs(req, s);
    }

    /*
     * Allow everything. If you want permissions use -o default_permissions
     */
    public void access(FuseReq req, long ino, int mask) {
        Reply.err(req, 0);
    }

    public void rmdir(FuseReq req, long parent, String name) {
        if (name.equals(".") || name.equals("..")) {
            Reply.err(req, Errno.EINVAL);
            return;
        }
        
        if (parent == 1) { 
            parent = Constants.EXT2_ROOT_INO;
        }
        
        try {
            DirectoryInode parentInode = (DirectoryInode)(inodes.get(parent));
            DirectoryInode child = 
                (DirectoryInode)inodes.get(parentInode.lookup(name).getIno());
            
            parentInode.unLinkDir(child, name);
            
            Reply.err(req, 0);
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }    
    }

    public void unlink(FuseReq req, long parent, String name) {
        if (name.equals(".") || name.equals("..")) {
            Reply.err(req, Errno.EINVAL);
            return;
        }
        
        if (parent == 1) { 
            parent = Constants.EXT2_ROOT_INO;
        }
            
        try {
            DirectoryInode parentInode = (DirectoryInode)(inodes.get(parent));
            Inode child = 
                    inodes.get(parentInode.lookup(name).getIno());
                
            parentInode.unLinkOther(child, name);
                
            Reply.err(req, 0);
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (JExt2Exception e) {
            Reply.err(req, e.getErrno());
        }
    }

    public void symlink(FuseReq req, String link, long parent, String name) {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            DirectoryInode parentInode = (DirectoryInode)inodes.get(parent);

            FuseContext context = req.getContext();           
            SymlinkInode inode = SymlinkInode.createEmpty();            
            inode.setUid(context.getUid());
            inode.setGid(context.getGid());
            InodeAlloc.registerInode(parentInode, inode);
            inode.write();
            
            ((DirectoryInode)parentInode).addLink(inode, name);            
            inode.setSymlink(link);
            
            Reply.entry(req, makeEntryParam(inode));    
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (JExt2Exception e) {
            System.out.println(":)");
            Reply.err(req, e.getErrno());
        }
        
        
    }

}
