/**
 * Base for parsed on disk blocks like Inodes and BlockGroupDescriptors. 
 */

package jext2;

import java.io.IOException;
import java.nio.*;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class Block {
	/** block location on filesystem */
	protected int blockNr;
	/** in block offset */
	protected int offset;
	
	/** read data structure from a ByteBuffer representing a block */
	protected abstract void read(ByteBuffer buf) throws IOException;
	
	/** write data structure back to disk */
	protected void write(ByteBuffer buf) {
	}
	
	protected Block(int blockNr, int offset) {
		this.blockNr = blockNr;
		this.offset = offset;
	}
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}
}
