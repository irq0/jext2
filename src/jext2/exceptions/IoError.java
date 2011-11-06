
package jext2.exceptions;

import fuse.Errno;

public class IoError extends JExt2Exception {
	static final long serialVersionUID = 42;
	protected static final int ERRNO=Errno.EIO;
	public IoError(String msg) {
		super(msg);
	}
	public IoError() {
		super();
	}
}
