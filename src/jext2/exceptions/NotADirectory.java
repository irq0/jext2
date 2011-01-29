
package jlowfuse.exceptions;

import fuse.Errno;

public class NotADirectory extends FuseException {
    static final long serialVersionUID = 42;
    public NotADirectory() {
        super(Errno.ENOTDIR);
    }
}
