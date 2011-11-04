
package jext2.exceptions;

import fuse.Errno;

public class TooManyLinks extends JExt2Exception {
	static final long serialVersionUID = 42;
	public TooManyLinks() {
		super(Errno.EMLINK);
	}
}
