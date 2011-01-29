
package jlowfuse.exceptions;

import fuse.Errno;

public class FileExists extends FuseException {
    static final long serialVersionUID = 42;
    public FileExists() {
        super(Errno.EEXIST);
    }
}
