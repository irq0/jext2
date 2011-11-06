
package jext2.exceptions;

import fuse.Errno;

public class QuotaExceeded extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EDQUOT;
}
