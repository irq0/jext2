
package jext2.exceptions;

import fuse.Errno;

public class DeviceOrResourceBusy extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EBUSY;
	
	public int getErrno() {
		return ERRNO;
	}
}
