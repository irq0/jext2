
package jlowfuse.exceptions;

import fuse.Errno;

public class DeviceOrResourceBusy extends FuseException {
    static final long serialVersionUID = 42;
    public DeviceOrResourceBusy() {
        super(Errno.EBUSY);
    }
}
