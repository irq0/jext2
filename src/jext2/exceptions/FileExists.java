
package jext2.exceptions;

import fuse.Errno;

public class FileExists extends JExt2Exception {
	static final long serialVersionUID = 42;
	public FileExists() {
		super(Errno.EEXIST);
	}
}
