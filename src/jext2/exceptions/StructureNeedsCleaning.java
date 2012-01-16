
package jext2.exceptions;

import fuse.Errno;

public class StructureNeedsCleaning extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EUCLEAN;

	public int getErrno() {
		return ERRNO;
	}
}
