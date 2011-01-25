package fusejext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import jext2.*;
import jext2.DirectoryInode.FileExistsException;
import fuse.*;
import jlowfuse.*;
import fuse.StatVFS;

public class JExt2Ops extends AbstractLowlevelOps {

	private BlockAccess blocks;
	private Superblock superblock;
	private BlockGroupAccess blockGroups;

	private FileChannel blockDev;
	
	private List<RegularInode> openInodes = new Vector<RegularInode>();
	private List<DirectoryInode> openDirectories = new Vector<DirectoryInode>();
	
	public JExt2Ops(FileChannel blockDev) {
		this.blockDev = blockDev;
	}
	
	public void init() {
		try {
			blocks = new BlockAccess(blockDev);
			superblock = Superblock.fromBlockAccess(blocks);
			blocks.setBlocksize(superblock.getBlocksize());

			blockGroups = new BlockGroupAccess();
			blockGroups.readDescriptors();
		} catch (IOException e) {
			System.out.println("init() failed :(");
			System.out.println(23);
		}
		
	}
	
	private Stat getStat(Inode inode) {
		Stat s = new Stat();
		s.setUid(inode.getUidLow());
		s.setGid(inode.getGidLow());
		s.setIno(inode.getIno());
		s.setMode(inode.getMode());
		s.setBlksize(superblock.getBlocksize());
		s.setBlocks(inode.getBlocks());
		s.setNlink(inode.getLinksCount());
		s.setSize(inode.getSize());
		
		Timespec atim = new Timespec();
		atim.setSec((int)(inode.getAccessTime().getTime() / 1000));			
		s.setAtim(atim);
		
		Timespec ctim = new Timespec();
		ctim.setSec((int)(inode.getChangeTime().getTime() / 1000));			
		s.setCtim(ctim);
		
		Timespec mtim = new Timespec();
		mtim.setSec((int)(inode.getAccessTime().getTime() / 1000));			
		s.setMtim(mtim);
		
		return s;
	}		

	public void open(FuseReq req, long ino, FileInfo fi) {
		try {
			Inode inode = InodeAccess.readByIno(ino);
			if (inode == null) { 
				Reply.err(req, Errno.ENOSYS);
				return;
			} else if (! (inode instanceof RegularInode)) { 
				Reply.err(req, Errno.EPERM);
				return;
			}
			
			long pos = openInodes.size();
			openInodes.add((int)pos,((RegularInode)inode));
			fi.setFh(pos);
			Reply.open(req, fi);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}
	}
	
	public void read(FuseReq req, long ino, int size, int off, FileInfo fi) {
		RegularInode inode = openInodes.get((int)fi.getFh());

		try {
			ByteBuffer buf = inode.readData(size, off);
			Reply.byteBuffer(req, buf, 0, size);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}
	}
	
	
	public void write(FuseReq req, long ino, ByteBuffer buf, int off, FileInfo fi) {
	    RegularInode inode = openInodes.get((int)fi.getFh());
	    
	    try {
	        buf.rewind();
	        int count = inode.writeData(buf, off);
	        
	        if (count < 1) 
	            throw new IOException();
	        
	        Reply.write(req, count);
	    
	    } catch (IOException e) {
	        Reply.err(req, Errno.EIO);
	    }
	}
	
	public void release(FuseReq req, long ino, FileInfo fi) {
		openInodes.remove((int)fi.getFh());
		Reply.err(req, 0);
	}
	
	public void readlink(FuseReq req, long ino) {
		try {
			Inode inode = InodeAccess.readByIno(ino);
			if (inode == null) { 
				Reply.err(req, Errno.ENOSYS);
				return;
			} else if (!(inode instanceof SymlinkInode)) { 
				Reply.err(req, Errno.EPERM);
				return;
			}
			
			Reply.readlink(req, ((SymlinkInode)inode).getSymlink());			
			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}
		
	}
	
	public void flush(FuseReq req, long ino, FileInfo fi) {
		Reply.err(req, 0);
	}
	
	public void getattr(FuseReq req, long ino, FileInfo fi) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
			Inode inode = InodeAccess.readByIno(ino);
			if (inode == null) { 
				Reply.err(req, Errno.ENOENT);
				return;
			}
			
			Stat stat = getStat(inode);
			Reply.attr(req, stat, 0.0);
			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}
	}

	private boolean checkToSet(int to_set, int attr) {
	    return ((to_set & attr) != 0);
	}
	
	private Date timespecToDate(Timespec time) {
        Date date = new Date(time.getSec()*1000 + time.getNsec()/1000); 
        return date;
	}
	
    public void setattr(FuseReq req, long ino, Stat attr, int to_set, FileInfo fi) {
        if (ino == 1) ino = Constants.EXT2_ROOT_INO;
        
        try {
            Inode inode = InodeAccess.readByIno(ino);
            if (inode == null) {
                Reply.err(req, Errno.ENOENT);
                return;
            }

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
                
            }            
            if (checkToSet(to_set, FuseConstants.FUSE_SET_ATTR_UID)) {
                inode.setUid(attr.getUid());
            }            
            
            inode.write();
            
            Stat s = getStat(inode);
            Reply.attr(req, s, 0.0);
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        }
    }

	
	public void lookup(FuseReq req, long ino, String name) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {			
			Inode parent = InodeAccess.readByIno(ino);
			if (parent == null) {
				Reply.err(req, Errno.ENOENT);
				return;
			} else if (!(parent instanceof DirectoryInode)) { 
				Reply.err(req, Errno.ENOTDIR);
				return;
			}
			
			DirectoryEntry entry = ((DirectoryInode)parent).lookup(name);			
			if (entry == null) {  
				Reply.err(req, Errno.ENOENT);
				return;
			}

			Inode child = InodeAccess.readByIno(entry.getIno());
			EntryParam e = new EntryParam();
			e.setAttr(getStat(child));
			e.setGeneration(child.getGeneration());
			e.setAttr_timeout(0.0);
			e.setEntry_timeout(0.0);
			e.setIno(entry.getIno());
			
			Reply.entry(req, e);			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}				
	}
	
	
	public void opendir(FuseReq req, long ino, FileInfo fi) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {
			Inode inode = InodeAccess.readByIno(ino);
			if (inode == null) { 
				Reply.err(req, Errno.ENOENT);
				return;
			} else if (!(inode instanceof DirectoryInode)) {
				Reply.err(req, Errno.ENOTDIR);
				return;
			}
			
			int pos = openDirectories.size();
			openDirectories.add(pos, (DirectoryInode)inode);
			fi.setFh(pos);
			Reply.open(req, fi);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}	
	}
	
	
	public void readdir(FuseReq req, long ino, int size, int off, FileInfo fi) {
		DirectoryInode inode = openDirectories.get((int)fi.getFh());
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
		openDirectories.remove((int)fi.getFh());
		Reply.err(req, 0);		
	}

    public void mknod(FuseReq req, long parent, String name, short umode, short dev) {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            int mode = umode & 0xFFFF;

            /* Only support regular files */
            if (! Mode.isRegular(mode)) {
                Reply.err(req, Errno.ENOSYS);
            }
            
            Inode parentInode = InodeAccess.readByIno(parent);
            if (parentInode == null) { 
                Reply.err(req, Errno.ENOENT);
                return;
            } else if (!(parentInode instanceof DirectoryInode)) {
                Reply.err(req, Errno.ENOTDIR);
                return;
            }
           
            RegularInode inode = RegularInode.createEmpty();            
            inode.orMode(mode);
            inode.setUidLow(0);
            inode.setUidHigh(0);
            inode.setGidLow(0);
            inode.setGidHigh(0);
            InodeAlloc.registerInode(parentInode, inode);
            inode.write();
            
            ((DirectoryInode)parentInode).addLink(inode, name);
            
            EntryParam e = new EntryParam();
            e.setAttr(getStat((Inode)inode));
            e.setGeneration(inode.getGeneration());
            e.setAttr_timeout(0.0);
            e.setEntry_timeout(0.0);
            e.setIno(inode.getIno());

            Reply.entry(req, e);    
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (FileExistsException e) {
            Reply.err(req, Errno.EEXIST);
        }
	
    }	
    public void mkdir(FuseReq req, long parent, String name, short mode) {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            Inode parentInode = InodeAccess.readByIno(parent);
            if (parentInode == null) { 
                Reply.err(req, Errno.ENOENT);
                return;
            } else if (!(parentInode instanceof DirectoryInode)) {
                Reply.err(req, Errno.ENOTDIR);
                return;
            }

            System.out.println(req);
            
            DirectoryInode inode = 
                DirectoryInode.createEmpty();            
            inode.orMode(mode);
            inode.setUidLow(0);
            inode.setUidHigh(0);
            inode.setGidLow(0);
            inode.setGidHigh(0);
            InodeAlloc.registerInode(parentInode, inode);
            inode.addDotLinks((DirectoryInode)parentInode);
            inode.write();
            
            ((DirectoryInode)parentInode).addLink(inode, name);
            
            EntryParam e = new EntryParam();
            e.setAttr(getStat((Inode)inode));
            e.setGeneration(inode.getGeneration());
            e.setAttr_timeout(0.0);
            e.setEntry_timeout(0.0);
            e.setIno(inode.getIno());
            
            Reply.entry(req, e);    
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        } catch (FileExistsException e) {
            Reply.err(req, Errno.EEXIST);
        }
    }
	
	
    public void statfs(FuseReq req, long ino) {
        StatVFS s = new StatVFS();

        // TODO rw: calculate overhead
        // TODO rw: ext2_count_free_blocks
        // TODO rw: ext2_count_free_inodes
        
        s.setBsize(superblock.getBlocksize());
        s.setBlocks(superblock.getBlocksCount()); // - overhead_last );
        s.setBfree(superblock.getFreeBlocksCount()); 
        s.setBavail(s.getBfree() - superblock.getResevedBlocksCount());
        if (s.getBfree() < superblock.getResevedBlocksCount())
        	s.setBavail(0);
        s.setFiles(superblock.getInodesCount());
        s.setFfree(superblock.getFreeInodesCount());
        s.setNamemax(DirectoryEntry.MAX_NAME_LEN);

        // Note: This is not the same as the Linux kernel writes there.. 
        UUID uuid = superblock.getUuid();
        long fsid = uuid.getLeastSignificantBits() ^
        			uuid.getMostSignificantBits();
        s.setFsid(fsid); 
        Reply.statfs(req, s);
    }

	
}
