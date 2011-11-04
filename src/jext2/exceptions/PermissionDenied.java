
package jext2.exceptions;

import fuse.Errno;

public class PermissionDenied extends JExt2Exception {
	static final long serialVersionUID = 42;
	public PermissionDenied() {
		super(Errno.EACCES);
	}
}
