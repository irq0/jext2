
package jext2.exceptions;

import fuse.Errno;

public class QuotaExceeded extends JExt2Exception {
	static final long serialVersionUID = 42;
	public QuotaExceeded() {
		super(Errno.EDQUOT);
	}
}
