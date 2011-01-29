
package jlowfuse.exceptions;

import fuse.Errno;

public class ReadonlyFileSystem extends FuseException {
    static final long serialVersionUID = 42;
    public ReadonlyFileSystem() {
        super(Errno.EROFS);
    }
}
