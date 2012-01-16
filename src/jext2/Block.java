/**
 * Base for parsed on disk blocks like Inodes and BlockGroupDescriptors. Not used
 * for data blocks
 */

package jext2;

import java.nio.*;

import jext2.exceptions.IoError;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class Block {

	int cleanHashCode = 0;

	/** block location on filesystem */
	protected long nr;
	public long getBlockNr() {
		return nr;
	}

	public final void setBlockNr(long blockNr) {
		this.nr = blockNr;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
		.append(nr).toHashCode();
	}

	protected void write(ByteBuffer buf) throws IoError {
		if (getBlockNr() == -1)
			throw new IllegalArgumentException("data structure is unregistered");

		BlockAccess.getInstance().writePartial(getBlockNr(), 0, buf);
	}

	/** write data to disk */
	public abstract void write() throws IoError;

	/** read data structure from a ByteBuffer representing a block */
	protected abstract void read(ByteBuffer buf) throws IoError;

	protected Block(long blockNr) {
		this.nr = blockNr;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}

	/**
	 * Return dirty if data changed. Relies on hashCode()
	 */
	public boolean isDirty() {
		int newHashCode = hashCode();
		return cleanHashCode != newHashCode;
	}

	/**
	 * Mark data clean: Reset the dirty state
	 */
	public void cleanDirty() {
		this.cleanHashCode = hashCode();
	}

	public void sync() throws IoError {
		write();
		cleanDirty();
	}


}
