package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RegInode extends DataInode {
    protected RegInode(int blockNr, int offset) throws IOException {
        super(blockNr, offset);
    }
	
	public static RegInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		RegInode inode = new RegInode(-1, offset);
		inode.read(buf);
		return inode;
	}
}	
