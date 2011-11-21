
package jext2.exceptions;

import fuse.Errno;

public class NoSuchFileOrDirectory extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected static final int ERRNO = Errno.ENOENT;
	
	public int getErrno() {
		return ERRNO;
	}
}
