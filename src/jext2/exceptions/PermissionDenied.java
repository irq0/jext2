
package jlowfuse.exceptions;

import fuse.Errno;

public class PermissionDenied extends FuseException {
    static final long serialVersionUID = 42;
    public PermissionDenied() {
        super(Errno.EACCES);
    }
}
