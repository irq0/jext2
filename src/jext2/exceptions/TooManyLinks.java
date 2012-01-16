
package jext2.exceptions;

import fuse.Errno;

public class TooManyLinks extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EMLINK;

	public int getErrno() {
		return ERRNO;
	}
}
