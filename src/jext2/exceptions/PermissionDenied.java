
package jext2.exceptions;

import fuse.Errno;

public class PermissionDenied extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EACCES;
	
	public int getErrno() {
		return ERRNO;
	}
}
