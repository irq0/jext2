
package jext2.exceptions;

import fuse.Errno;

public class NoSpaceLeftOnDevice extends JExt2Exception {
    static final long serialVersionUID = 42;
    public NoSpaceLeftOnDevice() {
        super(Errno.ENOSPC);
    }
}
