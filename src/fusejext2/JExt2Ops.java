package fusejext2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.UUID;

import fuse.StatVFS;

import jext2.BlockAccess;
import jext2.BlockGroupAccess;
import jext2.Constants;
import jext2.Superblock;
import jlowfuse.AbstractLowlevelOps;
import jlowfuse.FuseReq;
import jlowfuse.Reply;

public class JExt2Ops extends AbstractLowlevelOps {

	private BlockAccess blocks;
	private Superblock superblock;
	private BlockGroupAccess blockGroups;

	private FileChannel blockDev;
	
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
