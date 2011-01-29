
package jlowfuse.exceptions;

import fuse.Errno;

public class NoSuchFileOrDirectory extends FuseException {
    static final long serialVersionUID = 42;
    public NoSuchFileOrDirectory() {
        super(Errno.ENOENT);
    }
}
