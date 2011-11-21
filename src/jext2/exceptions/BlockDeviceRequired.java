
package jext2.exceptions;

import fuse.Errno;

public class BlockDeviceRequired extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.ENOTBLK;
	
	public int getErrno() {
		return ERRNO;
	}
}
