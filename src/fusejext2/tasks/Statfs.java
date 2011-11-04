package fusejext2.tasks;

import jext2.DirectoryEntry;
import jext2.Superblock;
import fuse.StatVFS;
import fusejext2.Jext2Context;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class Statfs extends jlowfuse.async.tasks.Statfs<Jext2Context> {

	public Statfs(FuseReq req, long ino) {
		super(req, ino);
	}

	@Override
	public void run() {
        StatVFS s = new StatVFS();

        Superblock superblock = context.superblock;

        s.setBsize(superblock.getBlocksize());
        s.setFrsize(superblock.getBlocksize());
        s.setBlocks(superblock.getBlocksCount() - superblock.getOverhead());
        s.setBfree(superblock.getFreeBlocksCount());

        if (s.getBfree() >= superblock.getReservedBlocksCount())
            s.setBavail(superblock.getFreeBlocksCount() - superblock.getReservedBlocksCount());
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

}
