package jext2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RegInode extends Inode {
    DataBlockAccess dataAccess = null;
    
    /**
     * Get the data access provider to read and write to the data area of this
     * inode
     */
    public DataBlockAccess accessData() {
        if (dataAccess == null)
            dataAccess = DataBlockAccess.fromInode(this);
        return dataAccess;
    }
	
	protected RegInode(int blockNr, int offset) throws IOException {
		super(blockNr, offset);
	}
	
	public static RegInode fromByteBuffer(ByteBuffer buf, int offset) throws IOException {
		RegInode inode = new RegInode(-1, offset);
		inode.read(buf);
		return inode;
	}
}	
