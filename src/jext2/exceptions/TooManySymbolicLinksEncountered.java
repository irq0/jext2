
package jlowfuse.exceptions;

import fuse.Errno;

public class TooManySymbolicLinksEncountered extends FuseException {
    static final long serialVersionUID = 42;
    public TooManySymbolicLinksEncountered() {
        super(Errno.ELOOP);
    }
}
