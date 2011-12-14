package jext2;
import java.nio.ByteBuffer;

import jext2.exceptions.InvalidArgument;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;

public class InodeAccess extends DataStructureAccessProvider<Long, Inode>{
	private static InodeAccess _instance = new InodeAccess();

	private static Superblock superblock = Superblock.getInstance();
	private static BlockAccess blocks = BlockAccess.getInstance();
	private static BlockGroupAccess blockGroups = BlockGroupAccess.getInstance();

	private InodeAccess() {
	}

	public static Inode readFromByteBuffer(ByteBuffer buf) throws IoError {
		Mode mode = Mode.createWithNumericValue(Ext2fsDataTypes.getLE16(buf, 0)); /* NOTE: mode is first field in inode */

		if (mode.isDirectory()) {
			return DirectoryInode.fromByteBuffer(buf, 0);
		} else if (mode.isRegular()) {
			return RegularInode.fromByteBuffer(buf, 0);
		} else if (mode.isSymlink()) {
			return SymlinkInode.fromByteBuffer(buf, 0);
		} else {
			return Inode.fromByteBuffer(buf, 0);
		}
	}

	public static Inode readByIno(long ino) throws IoError, InvalidArgument {
		if (ino == 0 || ino > superblock.getInodesCount()) {
			throw new InvalidArgument();
		}

		int group = Calculations.groupOfIno(ino);
		int offset = Calculations.localInodeOffset(ino);
		int tblBlock = offset / superblock.getBlocksize();

		BlockGroupDescriptor descr = blockGroups.getGroupDescriptor(group);

		long absBlock = descr.getInodeTablePointer() + tblBlock;
		int relOffset = offset - (tblBlock * superblock.getBlocksize());

		if (absBlock < 0 || relOffset < 0)
			throw new IoError();

		assert getInstance().get(ino) == null; /* access provider shuldn't have an old reference */
		
		ByteBuffer rawInode = Inode.allocateByteBuffer();
		blocks.readToBuffer(absBlock, relOffset, rawInode);
		Inode inode = InodeAccess.readFromByteBuffer(rawInode);

		// TODO check for NOENT exception

		inode.setBlockGroup(group);
		inode.setIno(ino);
		inode.setBlockNr(absBlock);
		inode.setOffset(relOffset);

		return inode;

	}

	public static InodeAccess getInstance() {
		return _instance;
	}

	public static Inode readRootInode() throws IoError {
		try {
			return readByIno(Constants.EXT2_ROOT_INO);
		} catch (InvalidArgument e) {
			throw new RuntimeException("should not happen");
		}
	}


	public Inode getOpened(long ino)  {
		return get(ino);
	}

	public Inode openInode(long ino) throws JExt2Exception {
		Inode inode = open(ino);

		assert !inode.isDeleted() : "Inode is marked as deleted ?!";

		return inode;
	}

	public void closeInode(long ino) {
		release(ino);
	}

	@Override
	protected Inode createInstance(Long ino) throws JExt2Exception {
		return InodeAccess.readByIno(ino);
	}
}