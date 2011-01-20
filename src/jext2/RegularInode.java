package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RegularInode extends DataInode {
    protected RegularInode(int blockNr, int offset) throws IOException {
        super(blockNr, offset);
    }
	
	public static RegularInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		RegularInode inode = new RegularInode(-1, offset);
		inode.read(buf);
		return inode;
	}
}	
