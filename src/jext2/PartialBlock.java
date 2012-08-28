/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

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
