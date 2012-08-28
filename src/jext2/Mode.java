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
 * UNIX Mode representation
 */
public class Mode {
	protected int mode = 0;

	private Mode() {
	}

	public static Mode createWithNumericValue(int mode) {
		Mode m = new Mode();
		m.mode = mode;
		return m;
	}


	private boolean isTypeMaskSet(int mask) {
		return (mode & IFMT) == mask;
	}

	private boolean isPermisionMaskSet(int mask) {
		return (mode & mask) != 0;
	}

	public static final int IFMT = 00170000;
	public static final int IFSOC = 0140000;
	public static final int IFLNK = 0120000;
	public static final int IFREG = 0100000;
	public static final int IFBLK = 0060000;
	public static final int IFDIR = 0040000;
	public static final int IFCHR = 0020000;
	public static final int IFIFO = 0010000;

	/** Set user ID on execution.  */
	public static final int ISUID = 0004000;
	/** Set group ID on execution.  */
	public static final int ISGID = 0002000;
	/** Sticky bit */
	public static final int ISVTX = 0001000;
	/* Read, write, and execute by owner.  */
	public static final int IRWXU = 00700;
	/** Read by owner */
	public static final int IRUSR = 00400;
	/** Write by owner */
	public static final int IWUSR = 00200;
	/** Execute by owner */
	public static final int IXUSR = 00100;
	/** Read, write, and execute by group  */
	public static final int IRWXG = 00070;
	/** Read by group */
	public static final int IRGRP = 00040;
	/** Write by group */
	public static final int IWGRP = 00020;
	/** Execute by group */
	public static final int IXGRP = 00010;
	/** Read, write, and execute by others  */
	public static final int IRWXO = 00007;
	/** Read by others */
	public static final int IROTH = 00004;
	/** Write by others */
	public static final int IWOTH = 00002;
	/** Execute by others */
	public static final int IXOTH = 00001;

	public boolean isSocket() {
		return isTypeMaskSet(IFSOC);
	}
	public boolean isSymlink() {
		return isTypeMaskSet(IFLNK);
	}
	public boolean isRegular() {
		return isTypeMaskSet(IFREG);
	}
	public boolean isBlockdev() {
		return isTypeMaskSet(IFBLK);
	}
	public boolean isDirectory() {
		return isTypeMaskSet(IFDIR);
	}
	public boolean isChardev() {
		return isTypeMaskSet(IFCHR);
	}
	public boolean isFifo() {
		return isTypeMaskSet(IFIFO);
	}
	public boolean isSetUid() {
		return isPermisionMaskSet(ISUID);
	}
	public boolean isSetGid() {
		return isPermisionMaskSet(ISGID);
	}
	public boolean isSticky() {
		return isPermisionMaskSet(ISVTX);
	}
	public boolean isOwnerReadable() {
		return isPermisionMaskSet(IRUSR);
	}
	public boolean isOwnerWritable() {
		return isPermisionMaskSet(IWUSR);
	}
	public boolean isOwnerExecutable() {
		return isPermisionMaskSet(IXUSR);
	}
	public boolean isGroupReadable() {
		return isPermisionMaskSet(IRGRP);
	}
	public boolean isGroupWritable() {
		return isPermisionMaskSet(IWGRP);
	}
	public boolean isGroupExecutable() {
		return isPermisionMaskSet(IXGRP);
	}
	public boolean isOtherReadable() {
		return isPermisionMaskSet(IROTH);
	}
	public boolean isOtherWritable() {
		return isPermisionMaskSet(IWOTH);
	}
	public boolean isOtherExecutable() {
		return isPermisionMaskSet(IXOTH);
	}

	private char charIfTrue(char chr, boolean truth) {
		return (truth) ? chr : '-';
	}

	public char filetypeAsCharacter() {
		char c = '?';
		if (isDirectory())
			c = 'd';
		else if (isBlockdev())
			c = 'b';
		else if (isChardev())
			c = 'c';
		else if (isSymlink())
			c = 'l';
		else if (isSocket())
			c = 's';
		else if (isFifo())
			c = 'p';
		else if (isRegular())
			c = '-';

		return c;
	}

	public String octalStringRepresentation() {
		if (mode == 0)
			return "0";
		else
			return "0" + Integer.toOctalString(mode);
	}

	public String unixStringRepresentation() {
		StringBuilder s = new StringBuilder();
		s.append(filetypeAsCharacter());
		s.append(charIfTrue('r', isOwnerReadable()));
		s.append(charIfTrue('w', isOwnerWritable()));
		s.append(charIfTrue('x', isOwnerExecutable()));
		s.append(charIfTrue('r', isGroupReadable()));
		s.append(charIfTrue('w', isGroupWritable()));
		s.append(charIfTrue('x', isGroupExecutable()));
		s.append(charIfTrue('r', isOtherReadable()));
		s.append(charIfTrue('w', isOtherWritable()));
		s.append(charIfTrue('x', isOtherExecutable()));

		return s.toString();
	}

	public int numeric() {
		return mode;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Mode: ");
		s.append(unixStringRepresentation());
		s.append("  ");
		s.append(octalStringRepresentation());

		return s.toString();
	}
}
