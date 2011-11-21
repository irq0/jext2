
package jext2.exceptions;

import fuse.Errno;

public class NoSpaceLeftOnDevice extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.ENOSPC;
	
	public int getErrno() {
		return ERRNO;
	}
}
