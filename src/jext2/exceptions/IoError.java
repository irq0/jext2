
package jlowfuse.exceptions;

import fuse.Errno;

public class IoError extends FuseException {
    static final long serialVersionUID = 42;
    public IoError() {
        super(Errno.EIO);
    }
}
