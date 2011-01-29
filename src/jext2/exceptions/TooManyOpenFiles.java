
package jext2.exceptions;

import fuse.Errno;

public class TooManyOpenFiles extends JExt2Exception {
    static final long serialVersionUID = 42;
    public TooManyOpenFiles() {
        super(Errno.EMFILE);
    }
}
