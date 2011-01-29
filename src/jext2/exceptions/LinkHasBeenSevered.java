
package jlowfuse.exceptions;

import fuse.Errno;

public class LinkHasBeenSevered extends FuseException {
    static final long serialVersionUID = 42;
    public LinkHasBeenSevered() {
        super(Errno.ENOLINK);
    }
}
