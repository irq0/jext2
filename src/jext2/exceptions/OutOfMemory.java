
package jext2.exceptions;

import fuse.Errno;

public class OutOfMemory extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.ENOMEM;
	
	public int getErrno() {
		return ERRNO;
	}
}
