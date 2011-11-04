
package jext2.exceptions;

import fuse.Errno;

public class IsADirectory extends JExt2Exception {
	static final long serialVersionUID = 42;
	public IsADirectory() {
		super(Errno.EISDIR);
	}
}
