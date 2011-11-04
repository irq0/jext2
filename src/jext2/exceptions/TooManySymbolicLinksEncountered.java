
package jext2.exceptions;

import fuse.Errno;

public class TooManySymbolicLinksEncountered extends JExt2Exception {
	static final long serialVersionUID = 42;
	public TooManySymbolicLinksEncountered() {
		super(Errno.ELOOP);
	}
}
