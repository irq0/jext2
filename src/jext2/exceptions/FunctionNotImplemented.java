
package jlowfuse.exceptions;

import fuse.Errno;

public class FunctionNotImplemented extends FuseException {
    static final long serialVersionUID = 42;
    public FunctionNotImplemented() {
        super(Errno.ENOSYS);
    }
}
