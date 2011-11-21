
package jext2.exceptions;

import fuse.Errno;

public class OperationNotPermitted extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EPERM;
	
	public int getErrno() {
		return ERRNO;
	}
}
