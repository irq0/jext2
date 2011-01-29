
package jext2.exceptions;

import fuse.Errno;

public class BlockDeviceRequired extends JExt2Exception {
    static final long serialVersionUID = 42;
    public BlockDeviceRequired() {
        super(Errno.ENOTBLK);
    }
}
