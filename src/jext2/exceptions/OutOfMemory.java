
package jlowfuse.exceptions;

import fuse.Errno;

public class OutOfMemory extends FuseException {
    static final long serialVersionUID = 42;
    public OutOfMemory() {
        super(Errno.ENOMEM);
    }
}
