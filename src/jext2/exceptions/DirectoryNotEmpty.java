
package jext2.exceptions;

import fuse.Errno;

public class DirectoryNotEmpty extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.ENOTEMPTY;

	public int getErrno() {
		return ERRNO;
	}
}
