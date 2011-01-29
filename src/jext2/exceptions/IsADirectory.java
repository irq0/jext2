
package jlowfuse.exceptions;

import fuse.Errno;

public class IsADirectory extends FuseException {
    static final long serialVersionUID = 42;
    public IsADirectory() {
        super(Errno.EISDIR);
    }
}
