
package jext2.exceptions;

import fuse.Errno;

public class NotADirectory extends JExt2Exception {
    static final long serialVersionUID = 42;
    public NotADirectory() {
        super(Errno.ENOTDIR);
    }
}
