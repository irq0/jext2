
package jext2.exceptions;

import fuse.Errno;

public class DirectoryNotEmpty extends JExt2Exception {
	static final long serialVersionUID = 42;
	public DirectoryNotEmpty() {
		super(Errno.ENOTEMPTY);
	}
}
