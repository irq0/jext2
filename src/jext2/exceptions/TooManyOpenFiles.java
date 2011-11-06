
package jext2.exceptions;

import fuse.Errno;

public class TooManyOpenFiles extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected final static int ERRNO = Errno.EMFILE;
}
