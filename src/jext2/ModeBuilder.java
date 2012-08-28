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

/**
 * Builder class for Mode
 */
public class ModeBuilder {

	int mode = 0;
	boolean filetypeApplied = false;

	private void maskMode(int mask) {
		this.mode = this.mode & mask;
	}

	private void addToMode(int add) {
		this.mode = this.mode | add;
	}

	private void addToModeAndCheckFiletype(int mask) {
		if (filetypeApplied)
			throw new IllegalArgumentException("A file can only have one filetype");

		addToMode(mask);
	}

	private ModeBuilder lucidAddToMode(int add) {
		addToMode(add);
		return this;
	}

	private ModeBuilder lucidMaskMode(int mask) {
		maskMode(mask);
		return this;
	}

	private ModeBuilder lucidAddToModeAndCheckFiletype(int mask) {
		addToModeAndCheckFiletype(mask);
		return this;
	}

	public ModeBuilder regularFile() {
		return lucidAddToModeAndCheckFiletype(Mode.IFREG);
	}

	public ModeBuilder directory() {
		return lucidAddToModeAndCheckFiletype(Mode.IFDIR);
	}

	public ModeBuilder socket() {
		return lucidAddToModeAndCheckFiletype(Mode.IFSOC);
	}

	public ModeBuilder link() {
		return lucidAddToModeAndCheckFiletype(Mode.IFLNK);
	}

	public ModeBuilder blockDevice() {
		return lucidAddToModeAndCheckFiletype(Mode.IFBLK);
	}

	public ModeBuilder characterDevice() {
		return lucidAddToModeAndCheckFiletype(Mode.IFCHR);
	}

	public ModeBuilder fifo() {
		return lucidAddToModeAndCheckFiletype(Mode.IFIFO);
	}

	public ModeBuilder setUid() {
		return lucidAddToMode(Mode.ISUID);
	}

	public ModeBuilder setGid() {
		return lucidAddToMode(Mode.ISGID);
	}

	public ModeBuilder sticky() {
		return lucidAddToMode(Mode.ISVTX);
	}

	public ModeBuilder ownerRead() {
		return lucidAddToMode(Mode.IRUSR);
	}

	public ModeBuilder ownerWrite() {
		return lucidAddToMode(Mode.IWUSR);
	}

	public ModeBuilder ownerExecute() {
		return lucidAddToMode(Mode.IXUSR);
	}

	public ModeBuilder ownerReadWriteExecute() {
		return lucidAddToMode(Mode.IRWXU);
	}

	public ModeBuilder groupRead() {
		return lucidAddToMode(Mode.IRGRP);
	}

	public ModeBuilder groupWrite() {
		return lucidAddToMode(Mode.IWGRP);
	}

	public ModeBuilder groupExecute() {
		return lucidAddToMode(Mode.IXGRP);
	}

	public ModeBuilder groupReadWriteExecute() {
		return lucidAddToMode(Mode.IRWXG);
	}

	public ModeBuilder othersRead() {
		return lucidAddToMode(Mode.IROTH);
	}

	public ModeBuilder othersWrite() {
		return lucidAddToMode(Mode.IWOTH);
	}

	public ModeBuilder othersExecute() {
		return lucidAddToMode(Mode.IXOTH);
	}

	public ModeBuilder othersReadWriteExecute() {
		return lucidAddToMode(Mode.IRWXO);
	}

	public ModeBuilder numeric(int add) {
		return lucidAddToMode(add);
	}

	public ModeBuilder mask(int mask) {
		return lucidMaskMode(mask);
	}

	public ModeBuilder allReadWriteExecute() {
		return ownerReadWriteExecute().groupReadWriteExecute().othersReadWriteExecute();
	}

	public Mode create() {
		Mode m = Mode.createWithNumericValue(mode);
		return m;
	}
}
