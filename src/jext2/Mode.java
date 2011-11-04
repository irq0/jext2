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

	public static boolean mask(int mode, int mask) {
		return (mode & IFMT) == mask;
	}

	public boolean isMaskSet(int mask) {
		return mask(mode, mask);
	}

	public static final int IFMT  = 00170000;
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
		return isMaskSet(IFSOC);
	}
	public boolean isSymlink() {
		return isMaskSet(IFLNK);
	}
	public boolean isRegular() {
		return isMaskSet(IFREG);
	}
	public boolean isBlockdev() {
		return isMaskSet(IFBLK);
	}
	public boolean isDirectory() {
		return isMaskSet(IFDIR);
	}
	public boolean isChardev() {
		return isMaskSet(IFCHR);
	}
	public boolean isFifo() {
		return isMaskSet(IFIFO);
	}
	public boolean isSetUid() {
		return isMaskSet(ISUID);
	}
	public boolean isSetGid() {
		return isMaskSet(ISGID);
	}
	public boolean isSticky() {
		return isMaskSet(ISVTX);
	}
	public boolean isOwnerReadable() {
		return isMaskSet(IRUSR);
	}
	public boolean isOwnerWritable() {
		return isMaskSet(IWUSR);
	}
	public boolean isOwnerExecutable() {
		return isMaskSet(IXUSR);
	}
	public boolean isGroupReadable() {
		return isMaskSet(IRGRP);
	}
	public boolean isGroupWritable() {
		return isMaskSet(IWGRP);
	}
	public boolean isGroupExecutable() {
		return isMaskSet(IXGRP);
	}
	public boolean isOtherReadable() {
		return isMaskSet(IROTH);
	}
	public boolean isOtherWritable() {
		return isMaskSet(IWOTH);
	}
	public boolean isOtherExecutable() {
		return isMaskSet(IXOTH);
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
		s.append(Integer.toOctalString(mode));

		return s.toString();
	}
}
