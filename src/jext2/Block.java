/**
 * Abstract Block
 *
 * @author Marcel Lauhoff <ml@irq0.org>
 */

package jext2;

import java.nio.*;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class Block {
	/** block location on filesystem */
	protected int blockNr;
	/** in block offset */
	protected int offset;
	
	/** read data structure from a ByteBuffer representing a block */
	protected abstract void read(ByteBuffer buf);
	
	/** write data structure back to disk */
	protected void write(ByteBuffer buf) {
	}
	
	private Block(ByteBuffer buffer) {
	
	}
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
		                                          ToStringStyle.MULTI_LINE_STYLE);
	}

	public static Block fromByteBuffer(ByteBuffer buffer) {
		Block b = new Block();
		b.buffer = buffer;
		return b;
	}
}
