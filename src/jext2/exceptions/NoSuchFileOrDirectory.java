
package jext2.exceptions;

import fuse.Errno;

public class NoSuchFileOrDirectory extends JExt2Exception {
    static final long serialVersionUID = 42;
    public NoSuchFileOrDirectory() {
        super(Errno.ENOENT);
    }
}
