
package jext2.exceptions;

import fuse.Errno;

public class LinkHasBeenSevered extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.ENOLINK;
}
