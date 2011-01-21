/**
 * Base for parsed on disk blocks like Inodes and BlockGroupDescriptors. Not used
 * for data blocks
 */

package jext2;

import java.io.IOException;
import java.nio.*;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class Block {
	/** block location on filesystem */
	protected long blockNr;
	/** in block offset */
	protected int offset;
	
	public long getBlockNr() {
		return blockNr;
	}

	public int getOffset() {
		return offset;
	}

	public final void setBlockNr(long blockNr) {
		this.blockNr = blockNr;
	}

	public final void setOffset(int offset) {
		this.offset = offset;
	}
	
	protected void write(ByteBuffer buf) throws IOException {
		if (getOffset() == -1 || getBlockNr() == -1) 
			throw new IllegalArgumentException("data structure is unregistered");

		BlockAccess.getInstance().writePartial(getBlockNr(), getOffset(), buf);
	}
	
	/** write data to disk */
	public abstract void write() throws IOException;
	
	/** read data structure from a ByteBuffer representing a block */
	protected abstract void read(ByteBuffer buf) throws IOException;
		
	protected Block(long blockNr, int offset) {
		this.blockNr = blockNr;
		this.offset = offset;
	}
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
}
