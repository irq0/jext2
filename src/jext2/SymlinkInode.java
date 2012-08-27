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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.builder.HashCodeBuilder;

import jext2.annotations.NotThreadSafe;
import jext2.exceptions.FileTooLarge;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;
import jext2.exceptions.NoSpaceLeftOnDevice;

public class SymlinkInode extends DataInode {
	private String symlink = "";

	public int FAST_SYMLINK_MAX=Constants.EXT2_N_BLOCKS * 4;

	private ReentrantReadWriteLock symlinkLock = new JextReentrantReadWriteLock(true);

	@NotThreadSafe(useLock=true)
	private String readSlowSymlink() throws JExt2Exception, FileTooLarge {
		ByteBuffer buf = readData((int)getSize(), 0);
		return Ext2fsDataTypes.getString(buf, 0, buf.limit());
	}

	@NotThreadSafe(useLock=true)
	private void writeSlowSymlink(String link, int size) throws JExt2Exception, NoSpaceLeftOnDevice, FileTooLarge {
		ByteBuffer buf = ByteBuffer.allocate(Ext2fsDataTypes.getStringByteLength(link));
		Ext2fsDataTypes.putString(buf, link, buf.capacity(), 0);
		buf.rewind();
		writeData(buf, 0);
	}

	@NotThreadSafe(useLock=true)
	private String readFastSymlink(ByteBuffer buf) throws IoError {
		return Ext2fsDataTypes.getString(buf, 40 + offset, (int)getSize());
	}

	@NotThreadSafe(useLock=true)
	private void writeFastSymlink(String link, int size) throws IoError {
		this.symlink = link;
		setSize(size);
		setBlocks(0);
		write();
	}

	@NotThreadSafe(useLock=true)
	private void writeSymlink(String link, int size) throws NoSpaceLeftOnDevice, FileTooLarge, JExt2Exception {

		if (size < FAST_SYMLINK_MAX) {
			writeFastSymlink(link, size);
		} else {
			writeSlowSymlink(link, size);
		}
	}

	@NotThreadSafe(useLock=true)
	private void cleanupSlowToFast() throws JExt2Exception {
		try {
			accessData().truncate(0);
		} catch (FileTooLarge e) {
			throw new RuntimeException("should not happen");
		}
	}

	private void cleanupFastToSlow() {
		this.symlink = "";
	}

	@NotThreadSafe(useLock=true)
	private void cleanup(String newLink, int newSize) throws JExt2Exception {
		if (isSlowSymlink() && newSize <= FAST_SYMLINK_MAX) {
			cleanupFastToSlow();
		} else {
			cleanupSlowToFast();
		}
	}

	public final String getSymlink() throws JExt2Exception, FileTooLarge {
		symlinkLock.readLock().lock();

		String result;

		if (isFastSymlink())
			result = this.symlink;
		else
			result = readSlowSymlink();

		symlinkLock.readLock().unlock();
		return result;
	}

	/**
	 * Set new symlink. We either write data blocks or characters to the
	 * data block pointer depending on the symlink length.
	 * @throws FileTooLarge This is unlikely to happen because is requires
	 *     a symlink spanning thousands of blocks.
	 */
	public void setSymlink(String newLink) throws NoSpaceLeftOnDevice, JExt2Exception, FileTooLarge {
		int newSize = Ext2fsDataTypes.getStringByteLength(newLink);  // this is quite expensive and thus passed on

		setSize(0);

		symlinkLock.writeLock().lock();
		cleanup(newLink, newSize);
		writeSymlink(newLink, newSize);
		symlinkLock.writeLock().unlock();

		setStatusChangeTime(new Date());
	}

	protected SymlinkInode(long blockNr, int offset) throws IoError {
		super(blockNr, offset);
	}



	@Override
	protected void read(ByteBuffer buf) throws IoError {
		super.read(buf);

		if (isFastSymlink())
			this.symlink = readFastSymlink(buf);
	}

	@Override
	protected void write(ByteBuffer buf) throws IoError {
		if (isFastSymlink() && getSize() > 0)
			Ext2fsDataTypes.putString(buf, symlink, (int)getSize(), 40);
		super.write(buf);
	}

	@Override
	public void write() throws IoError {
		ByteBuffer buf = allocateByteBuffer();
		write(buf);
	}

	public static SymlinkInode fromByteBuffer(ByteBuffer buf, int offset) throws IoError {
		SymlinkInode inode = new SymlinkInode(-1, offset);
		inode.read(buf);
		return inode;
	}



	/**
	 * Create empty Inode. Initialize *Times, block array.
	 */
	public static SymlinkInode createEmpty() throws JExt2Exception {
		SymlinkInode inode = new SymlinkInode(-1, -1);
		Date now = new Date();

		inode.setModificationTime(now);
		inode.setAccessTime(now);
		inode.setStatusChangeTime(now);
		inode.setDeletionTime(new Date(0));
		inode.setMode(new ModeBuilder().link().allReadWriteExecute().create());
		inode.setBlock(new long[Constants.EXT2_N_BLOCKS]);
		inode.setBlocks(0);
		inode.symlink = "";
		inode.setSize(0);

		return inode;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
		.appendSuper(super.hashCode())
		.append(symlink).toHashCode();
	}

	@Override
	public short getFileType() {
		return DirectoryEntry.FILETYPE_SYMLINK;
	}

	@Override
	public boolean isSymlink() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isRegularFile() {
		return false;
	}
	/**
	 * Check if inode is a fast symlink. A fast symlink stores the link in the
	 * data block pointers instead of data blocks itself
	 *
	 * @return true if Inode is fast symlink; false otherwise.
	 */
	@Override
	public boolean isFastSymlink() {
		return (getBlocks() == 0);
	}

	@Override
	public boolean isSlowSymlink() {
		return !isFastSymlink();
	}

	@Override
	public boolean hasDataBlocks() {
		return isSlowSymlink();
	}
}
