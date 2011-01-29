
package jlowfuse.exceptions;

import fuse.Errno;

public class TryAgain extends FuseException {
    static final long serialVersionUID = 42;
    public TryAgain() {
        super(Errno.EAGAIN);
    }
}
