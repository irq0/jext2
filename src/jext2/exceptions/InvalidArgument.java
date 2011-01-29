
package jlowfuse.exceptions;

import fuse.Errno;

public class InvalidArgument extends FuseException {
    static final long serialVersionUID = 42;
    public InvalidArgument() {
        super(Errno.EINVAL);
    }
}
