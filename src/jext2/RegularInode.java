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
import java.util.Date;

import jext2.annotations.NotThreadSafe;
import jext2.exceptions.FileTooLarge;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;

/**
 * Inode for regular files.
 */
public class RegularInode extends DataInode {
	protected RegularInode(long blockNr, int offset) throws IoError {
		super(blockNr, offset);
	}

	public static RegularInode fromByteBuffer(ByteBuffer buf, int offset) throws IoError {
		RegularInode inode = new RegularInode(-1, offset);
		inode.read(buf);
		return inode;
	}

	@Override
	public short getFileType() {
		return DirectoryEntry.FILETYPE_REG_FILE;
	}

	@Override
	public boolean isSymlink() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isRegularFile() {
		return true;
	}

	/**
	 * Set size. For regular inodes the size is stored in i_size and i_dir_acl
	 *
	 */
	@Override
	public void setSize(long newsize) {
		super.setSize(newsize & Ext2fsDataTypes.LE32_MAX);
		super.setDirAcl((newsize >>> Ext2fsDataTypes.LE32_SIZE) & Ext2fsDataTypes.LE32_MAX);
	}

	@Override
	public long getSize() {
		return super.getSize() + (super.getDirAcl() << Ext2fsDataTypes.LE32_SIZE);
	}

	/**
	 * Set size and truncate.
	 * @param   size    new size
	 * @throws FileTooLarge
	 */
	@NotThreadSafe(useLock=false)  // XXX will be thread safe once truncate is
	public void setSizeAndTruncate(long size) throws JExt2Exception, FileTooLarge {
		if (size < 0)
			throw new IllegalArgumentException("Try to set negative file size");
		long oldSize = getSize();
		setSize(size);
		if (oldSize > size)
			accessData().truncate(size);
	}

	/**
	 * Create empty Inode. Initialize *Times, block array.
	 */
	public static RegularInode createEmpty() throws IoError {
		RegularInode inode = new RegularInode(-1, -1);
		Date now = new Date();

		inode.setModificationTime(now);
		inode.setAccessTime(now);
		inode.setStatusChangeTime(now);
		inode.setDeletionTime(new Date(0));
		inode.setMode(new ModeBuilder().regularFile().create());
		inode.setBlock(new long[Constants.EXT2_N_BLOCKS]);

		return inode;
	}
}
