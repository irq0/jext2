
package jext2.exceptions;

import fuse.Errno;

public class StructureNeedsCleaning extends JExt2Exception {
	static final long serialVersionUID = 42;
	public StructureNeedsCleaning() {
		super(Errno.EUCLEAN);
	}
}
