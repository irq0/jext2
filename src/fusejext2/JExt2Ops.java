package fusejext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
	
	private Stat getStat(Inode inode, int ino) {
		Stat s = new Stat();
		s.setUid(inode.getUidLow());
		s.setGid(inode.getGidLow());
		s.setIno(ino);
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
			Inode inode = InodeAccess.readByIno((int)ino);
			if (inode == null) { 
				Reply.err(req, Errno.ENOSYS);
				return;
			} else if (!(inode instanceof RegularInode)) { 
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
			ByteBuffer buf = inode.readData((int)size, (int)off);
			Reply.byteBuffer(req, buf, 0, size);
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}
	}
	
	
	public void write(FuseReq req, long ino, ByteBuffer buf, int off, FileInfo fi) {
	    RegularInode inode = openInodes.get((int)fi.getFh());
	    
	    try {
	        System.out.println("WRITE");
	        int count = inode.writeData(buf, off);
	        if (count < 1) 
	            throw new IOException();
	        else
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
			Inode inode = InodeAccess.readByIno((int)ino);
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
			Inode inode = InodeAccess.readByIno((int)ino);
			if (inode == null) { 
				Reply.err(req, Errno.ENOENT);
				return;
			}
			
			Stat stat = getStat(inode, (int)ino);
			Reply.attr(req, stat, 0.0);
			
		} catch (IOException e) {
			Reply.err(req, Errno.EIO);
		}
	}

	
    public void setattr(FuseReq req, long ino, Stat attr, int to_set, FileInfo fi) {
        if (ino == 1) ino = Constants.EXT2_ROOT_INO;
        
        try {
            Inode inode = InodeAccess.readByIno((int)ino);
            if (inode == null) {
                Reply.err(req, Errno.ENOENT);
                return;
            }

        
            Reply.attr(req, attr, 0.0);
        } catch (IOException e) {
            Reply.err(req, Errno.EIO);
        }
    }

	
	public void lookup(FuseReq req, long ino, String name) {
		if (ino == 1) ino = Constants.EXT2_ROOT_INO;
		try {			
			Inode parent = InodeAccess.readByIno((int)ino);
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
			e.setAttr(getStat(child, entry.getIno()));
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
			Inode inode = InodeAccess.readByIno((int)ino);
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

	
    public void mkdir(FuseReq req, long parent, String name, short mode) {
        if (parent == 1) parent = Constants.EXT2_ROOT_INO;
        try {
            Inode parentInode = InodeAccess.readByIno((int)parent);
            if (parentInode == null) { 
                Reply.err(req, Errno.ENOENT);
                return;
            } else if (!(parentInode instanceof DirectoryInode)) {
                Reply.err(req, Errno.ENOTDIR);
                return;
            }
           
            Inode inode = Inode.createEmpty();
            inode.setMode(mode);
            
            DirectoryEntry newDir = DirectoryEntry.create(name);
            newDir.setIno((int)(inode.getIno()));
            newDir.setFileType((byte)(Constants.LINUX_S_IFDIR));
            
            ((DirectoryInode)parentInode).addLink(newDir);
            
            InodeAlloc.registerInode(parentInode, inode);
            inode.write();
            parentInode.write();
            
            EntryParam e = new EntryParam();
            e.setAttr(getStat((Inode)inode, (int)(inode.getIno())));
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
        s.setNamemax(Constants.EXT2_NAME_LEN);

        // Note: This is not the same as the Linux kernel writes there.. 
        UUID uuid = superblock.getUuid();
        long fsid = uuid.getLeastSignificantBits() ^
        			uuid.getMostSignificantBits();
        s.setFsid(fsid); 
        Reply.statfs(req, s);
    }

	
}
