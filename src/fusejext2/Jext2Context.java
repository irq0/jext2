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

package fusejext2;

import java.nio.channels.FileChannel;

import jext2.BlockAccess;
import jext2.BlockGroupAccess;
import jext2.InodeAccess;
import jext2.Superblock;
import jlowfuse.async.Context;

public class Jext2Context extends Context {
	public BlockAccess blocks;
	public Superblock superblock;
	public BlockGroupAccess blockGroups;

	public FileChannel blockDev;

	public InodeAccess inodes;

	public Jext2Context(FileChannel blockDev) {
		this.blockDev = blockDev;
	}
}
