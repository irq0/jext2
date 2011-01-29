
package jlowfuse.exceptions;

import fuse.Errno;

public class DirectoryNotEmpty extends FuseException {
    static final long serialVersionUID = 42;
    public DirectoryNotEmpty() {
        super(Errno.ENOTEMPTY);
    }
}
