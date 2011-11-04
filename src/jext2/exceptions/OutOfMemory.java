
package jext2.exceptions;

import fuse.Errno;

public class OutOfMemory extends JExt2Exception {
	static final long serialVersionUID = 42;
	public OutOfMemory() {
		super(Errno.ENOMEM);
	}
}
