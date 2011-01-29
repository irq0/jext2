
package jlowfuse.exceptions;

import fuse.Errno;

public class QuotaExceeded extends FuseException {
    static final long serialVersionUID = 42;
    public QuotaExceeded() {
        super(Errno.EDQUOT);
    }
}
