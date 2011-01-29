
package jlowfuse.exceptions;

import fuse.Errno;

public class TooManyLinks extends FuseException {
    static final long serialVersionUID = 42;
    public TooManyLinks() {
        super(Errno.EMLINK);
    }
}
