
package jext2.exceptions;

import fuse.Errno;

public class FileTooLarge extends JExt2Exception {
    static final long serialVersionUID = 42;
    public FileTooLarge() {
        super(Errno.EFBIG);
    }
}
