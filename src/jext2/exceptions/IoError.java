
package jext2.exceptions;

import fuse.Errno;

public class IoError extends JExt2Exception {
    static final long serialVersionUID = 42;
    public IoError() {
        super(Errno.EIO);
    }
}
