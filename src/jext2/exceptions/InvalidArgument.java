
package jext2.exceptions;

import fuse.Errno;

public class InvalidArgument extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EINVAL;

	public int getErrno() {
		return ERRNO;
	}
}
