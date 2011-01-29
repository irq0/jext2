
package jlowfuse.exceptions;

import fuse.Errno;

public class NoSpaceLeftOnDevice extends FuseException {
    static final long serialVersionUID = 42;
    public NoSpaceLeftOnDevice() {
        super(Errno.ENOSPC);
    }
}
