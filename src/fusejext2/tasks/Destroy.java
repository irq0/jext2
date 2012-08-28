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

import java.util.Date;

import jext2.exceptions.IoError;
import fusejext2.Jext2Context;

public class Destroy extends jlowfuse.async.tasks.Destroy<Jext2Context> {
	@Override
	public void run() {
		try {
			context.superblock.setLastWrite(new Date());
			context.superblock.sync();
			context.blockGroups.syncDescriptors();

			context.blocks.sync();
		} catch (IoError e) {
			System.out.println("IoError on final superblock/block group descr sync");
		}
	}
}
