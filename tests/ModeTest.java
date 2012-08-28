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

import static org.junit.Assert.*;

import jext2.*;

import org.junit.*;

public class ModeTest {
	@Test
	public void modeBuilderShouldBuildA754Directory() {
		Mode mode = new ModeBuilder()
							.directory()
							.ownerReadWriteExecute()
							.groupRead().groupExecute()
							.othersRead()
							.create();

		assertFalse(mode.isBlockdev());
		assertFalse(mode.isChardev());
		assertTrue(mode.isDirectory());
		assertFalse(mode.isFifo());
		assertFalse(mode.isRegular());
		assertFalse(mode.isSymlink());
		assertFalse(mode.isSocket());

		assertTrue(mode.isOwnerExecutable());
		assertTrue(mode.isOwnerReadable());
		assertTrue(mode.isOwnerWritable());

		assertTrue(mode.isGroupExecutable());
		assertTrue(mode.isGroupReadable());
		assertFalse(mode.isGroupWritable());

		assertFalse(mode.isOtherExecutable());
		assertTrue(mode.isOtherReadable());
		assertFalse(mode.isOtherWritable());

		assertEquals(mode.numeric(),
				Mode.createWithNumericValue(040754).numeric());
	}

	@Test
	public void modeBuilderShouldBuildA644DRegularFileUsingAddAndMask() {
		Mode mode = new ModeBuilder()
							.regularFile()
							.numeric(0666)
							.mask(~0002)
							.create();

		assertFalse(mode.isBlockdev());
		assertFalse(mode.isChardev());
		assertFalse(mode.isDirectory());
		assertFalse(mode.isFifo());
		assertTrue(mode.isRegular());
		assertFalse(mode.isSymlink());
		assertFalse(mode.isSocket());

		assertFalse(mode.isOwnerExecutable());
		assertTrue(mode.isOwnerReadable());
		assertTrue(mode.isOwnerWritable());

		assertFalse(mode.isGroupExecutable());
		assertTrue(mode.isGroupReadable());
		assertTrue(mode.isGroupWritable());

		assertFalse(mode.isOtherExecutable());
		assertTrue(mode.isOtherReadable());
		assertFalse(mode.isOtherWritable());

		assertEquals(mode.numeric(),
				Mode.createWithNumericValue(0100664).numeric());
	}
}
