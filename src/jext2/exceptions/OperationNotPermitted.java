
package jlowfuse.exceptions;

import fuse.Errno;

public class OperationNotPermitted extends FuseException {
    static final long serialVersionUID = 42;
    public OperationNotPermitted() {
        super(Errno.EPERM);
    }
}