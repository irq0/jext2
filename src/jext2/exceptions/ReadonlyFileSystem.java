
package jext2.exceptions;

import fuse.Errno;

public class ReadonlyFileSystem extends JExt2Exception {
    static final long serialVersionUID = 42;
    public ReadonlyFileSystem() {
        super(Errno.EROFS);
    }
}
