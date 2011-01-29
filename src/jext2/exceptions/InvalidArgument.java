
package jext2.exceptions;

import fuse.Errno;

public class InvalidArgument extends JExt2Exception {
    static final long serialVersionUID = 42;
    public InvalidArgument() {
        super(Errno.EINVAL);
    }
}
