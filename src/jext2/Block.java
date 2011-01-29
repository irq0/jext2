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
	public long getBlockNr() {
		return blockNr;
	}

	public final void setBlockNr(long blockNr) {
		this.blockNr = blockNr;
	}

	protected void write(ByteBuffer buf) throws IOException {
		if (getBlockNr() == -1) 
			throw new IllegalArgumentException("data structure is unregistered");

		BlockAccess.getInstance().writePartial(getBlockNr(), 0, buf);
	}
	
	/** write data to disk */
	public abstract void write() throws IOException;
	
	/** read data structure from a ByteBuffer representing a block */
	protected abstract void read(ByteBuffer buf) throws IOException;
		
	protected Block(long blockNr) {
		this.blockNr = blockNr;
	}
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
}
