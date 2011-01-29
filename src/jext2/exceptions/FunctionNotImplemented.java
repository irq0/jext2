
package jext2.exceptions;

import fuse.Errno;

public class FunctionNotImplemented extends JExt2Exception {
    static final long serialVersionUID = 42;
    public FunctionNotImplemented() {
        super(Errno.ENOSYS);
    }
}
