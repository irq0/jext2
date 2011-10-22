package fusejext2;

import java.nio.channels.FileChannel;

import jext2.BlockAccess;
import jext2.BlockGroupAccess;
import jext2.Superblock;
import jlowfuse.async.Context;

public class Jext2Context extends Context {
	public BlockAccess blocks;
	public Superblock superblock;
	public BlockGroupAccess blockGroups;

	public FileChannel blockDev;
	
	public InodeAccessProvider inodes;
	
	public Jext2Context(FileChannel blockDev) {
		this.blockDev = blockDev;
	}
}
