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
