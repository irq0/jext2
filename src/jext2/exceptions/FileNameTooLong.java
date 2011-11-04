
package jext2.exceptions;

import fuse.Errno;

public class FileNameTooLong extends JExt2Exception {
	static final long serialVersionUID = 42;
	public FileNameTooLong() {
		super(Errno.ENAMETOOLONG);
	}
}
