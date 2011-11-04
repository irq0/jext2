package fusejext2;

import java.util.Date;

import jext2.DataInode;
import jext2.Inode;
import jext2.Superblock;
import fuse.EntryParam;
import fuse.Stat;
import fuse.Timespec;

public class Util {
	public static Timespec dateToTimespec(Date date) {
	    Timespec tim = new Timespec();
	    tim.setSec((int)(date.getTime() / 1000));
	    tim.setNsec(0);
	    return tim;
	}

    public static Date timespecToDate(Timespec time) {
	    return new Date(time.getSec()*1000 + time.getNsec()/1000);
	}

	public static Stat inodeToStat(Superblock superblock, Inode inode) {
		Stat s = new Stat();
		s.setUid(inode.getUidLow());
		s.setGid(inode.getGidLow());
		s.setIno(inode.getIno());
		s.setMode(inode.getMode().numeric());
		s.setBlksize(superblock.getBlocksize());
		s.setNlink(inode.getLinksCount());
		s.setSize(inode.getSize());

		if (inode.hasDataBlocks())
		    s.setBlocks(((DataInode)inode).getBlocks());
		else
		    s.setBlocks(0);

		s.setAtim(dateToTimespec(inode.getAccessTime()));
		s.setCtim(dateToTimespec(inode.getStatusChangeTime()));
		s.setMtim(dateToTimespec(inode.getModificationTime()));

		return s;
	}

	public static EntryParam inodeToEntryParam(Superblock superblock, Inode inode) {
        EntryParam e = new EntryParam();
        e.setAttr(inodeToStat(superblock, inode));
        e.setGeneration(inode.getGeneration());
        e.setAttr_timeout(0.0);
        e.setEntry_timeout(0.0);
        e.setIno(inode.getIno());
        return e;
	}

}
