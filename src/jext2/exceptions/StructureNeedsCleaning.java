
package jlowfuse.exceptions;

import fuse.Errno;

public class StructureNeedsCleaning extends FuseException {
    static final long serialVersionUID = 42;
    public StructureNeedsCleaning() {
        super(Errno.EUCLEAN);
    }
}
