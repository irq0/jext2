
package jext2.exceptions;

import fuse.Errno;

public class IsADirectory extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EISDIR;
	
	public int getErrno() {
		return ERRNO;
	}
}
