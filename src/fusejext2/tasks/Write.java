/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

package fusejext2.tasks;
import java.nio.ByteBuffer;

import jext2.RegularInode;
import jext2.exceptions.IoError;
import jext2.exceptions.JExt2Exception;

import jlowfuse.FuseReq;
import jlowfuse.Reply;
import fuse.FileInfo;
import fusejext2.Jext2Context;

public class Write extends jlowfuse.async.tasks.Write<Jext2Context> {

	public Write(FuseReq req, long ino, ByteBuffer buf, long off, FileInfo fi) {
		super(req, ino, buf, off, fi);
	}

	@Override
	public void run() {
		try {
			RegularInode inode = (RegularInode)(context.inodes.getOpened(ino));
			buf.rewind();
			int count = inode.writeData(buf, off);

			if (count < 1)
				throw new IoError();

			Reply.write(req, count);

		} catch (JExt2Exception e) {
			Reply.err(req, e.getErrno());
		}
	}

}
