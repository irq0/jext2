package jext2;

import java.nio.ByteBuffer;

import org.apache.commons.lang.builder.HashCodeBuilder;

import jext2.exceptions.IoError;

/**
 * Base class for data structures taking only part of Block
 */
public abstract class PartialBlock extends Block {

	/** in block offset */
	protected int offset;

	@Override
	public abstract void write() throws IoError;

	@Override
	protected abstract void read(ByteBuffer buf) throws IoError;

	@Override
	protected void write(ByteBuffer buf) throws IoError {
		if (getOffset() == -1 || getBlockNr() == -1)
			throw new IllegalArgumentException("data structure is unregistered");

		BlockAccess.getInstance().writePartial(getBlockNr(), getOffset(), buf);
	}
	public int getOffset() {
		return offset;
	}

	public final void setOffset(int offset) {
		this.offset = offset;
	}

	protected PartialBlock(long blockNr, int offset) {
		super(blockNr);
		this.offset = offset;
	}


	@Override
	public int hashCode() {
		return new HashCodeBuilder()
		.appendSuper(super.hashCode())
		.append(offset).toHashCode();
	}
}
