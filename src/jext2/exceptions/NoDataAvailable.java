
package jlowfuse.exceptions;

import fuse.Errno;

public class NoDataAvailable extends FuseException {
    static final long serialVersionUID = 42;
    public NoDataAvailable() {
        super(Errno.ENODATA);
    }
}
