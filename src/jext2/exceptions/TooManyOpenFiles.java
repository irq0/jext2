
package jlowfuse.exceptions;

import fuse.Errno;

public class TooManyOpenFiles extends FuseException {
    static final long serialVersionUID = 42;
    public TooManyOpenFiles() {
        super(Errno.EMFILE);
    }
}
