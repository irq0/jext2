package jext2;
import java.nio.ByteBuffer;
import java.io.IOException;
class InodeAccess {

		public static Inode fromByteBuffer(ByteBuffer buf, int offset) throws IOException{
		Inode inode = Inode.fromByteBuffer(buf, offset);
		int mode = inode.getMode();

		System.out.println("MODE" + mode);
		
		if ((mode & Constants.LINUX_S_IFMT) == Constants.LINUX_S_IFDIR) {
			DirectoryInode newInode = DirectoryInode.fromByteBuffer(buf, offset);
			return newInode;
		} else if ((mode & Constants.LINUX_S_IFMT) == Constants.LINUX_S_IFREG) {
			RegInode newInode = RegInode.fromByteBuffer(buf, offset);
			return newInode;
		} else {
			return inode;
		}
	}
}

