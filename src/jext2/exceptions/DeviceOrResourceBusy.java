
package jext2.exceptions;

import fuse.Errno;

public class DeviceOrResourceBusy extends JExt2Exception {
    static final long serialVersionUID = 42;
    public DeviceOrResourceBusy() {
        super(Errno.EBUSY);
    }
}
