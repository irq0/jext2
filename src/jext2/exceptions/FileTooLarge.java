
package jlowfuse.exceptions;

import fuse.Errno;

public class FileTooLarge extends FuseException {
    static final long serialVersionUID = 42;
    public FileTooLarge() {
        super(Errno.EFBIG);
    }
}
