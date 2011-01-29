
package jlowfuse.exceptions;

import fuse.Errno;

public class FileNameTooLong extends FuseException {
    static final long serialVersionUID = 42;
    public FileNameTooLong() {
        super(Errno.ENAMETOOLONG);
    }
}
