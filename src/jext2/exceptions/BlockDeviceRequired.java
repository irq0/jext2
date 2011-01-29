
package jlowfuse.exceptions;

import fuse.Errno;

public class BlockDeviceRequired extends FuseException {
    static final long serialVersionUID = 42;
    public BlockDeviceRequired() {
        super(Errno.ENOTBLK);
    }
}
