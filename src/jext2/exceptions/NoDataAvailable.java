
package jext2.exceptions;

import fuse.Errno;

public class NoDataAvailable extends JExt2Exception {
    static final long serialVersionUID = 42;
    public NoDataAvailable() {
        super(Errno.ENODATA);
    }
}
