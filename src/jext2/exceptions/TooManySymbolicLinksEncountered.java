
package jext2.exceptions;

import fuse.Errno;

public class TooManySymbolicLinksEncountered extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.ELOOP;
	
	public int getErrno() {
		return ERRNO;
	}
}
