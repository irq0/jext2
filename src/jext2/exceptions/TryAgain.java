
package jext2.exceptions;

import fuse.Errno;

public class TryAgain extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EAGAIN;
	
	public int getErrno() {
		return ERRNO;
	}
}
