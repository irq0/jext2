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
