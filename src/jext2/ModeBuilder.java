package jext2;

public class ModeBuilder {

	int mode = 0;
	boolean filetypeApplied = false;
	private static ModeBuilder myInstance = new ModeBuilder();

	private void applyToMode(int mask) {
		this.mode = this.mode & mask;
	}

	private void applyToModeAndCheckFiletype(int mask) {
		if (filetypeApplied)
			throw new IllegalArgumentException("A file can only have one filetype");

		applyToMode(mask);
	}

	private static ModeBuilder lucidApplyToMode(int mask) {
		ModeBuilder m = instance();
		m.applyToMode(mask);
		return m;
	}

	private static ModeBuilder lucidApplyToModeAndCheckFiletype(int mask) {
		ModeBuilder m = instance();
		m.applyToModeAndCheckFiletype(mask);
		return m;
	}

	private static ModeBuilder instance() {
		return myInstance;
	}

	public void reset() {
		mode = 0;
		filetypeApplied = false;
	}

	public static ModeBuilder regularFile() {
		return lucidApplyToModeAndCheckFiletype(Mode.IFREG);
	}

	public static ModeBuilder directory() {
		return lucidApplyToModeAndCheckFiletype(Mode.IFDIR);
	}

	public static ModeBuilder socket() {
		return lucidApplyToModeAndCheckFiletype(Mode.IFSOC);
	}

	public static ModeBuilder link() {
		return lucidApplyToModeAndCheckFiletype(Mode.IFLNK);
	}

	public static ModeBuilder blockDevice() {
		return lucidApplyToModeAndCheckInterface(Mode.IFBLK);
	}

	public static ModeBuilder characterDevice() {
		return lucidApplyToModeAndCheckInterface(Mode.IFCHR);
	}

	public static ModeBuilder fifo() {
		return lucidApplyToModeAndCheckInterface(Mode.IFIFO);
	}

	public static ModeBuilder setUid() {
		return lucidApplyToMode(Mode.ISUID);
	}

	public static ModeBuilder setGid() {
		return lucidApplyToMode(Mode.ISGID);
	}

	public static ModeBuilder sticky() {
		return lucidApplyToMode(Mode.ISVTX);
	}

	public static ModeBuilder ownerRead() {
		return lucidApplyToMode(Mode.IRUSR);
	}

	public static ModeBuilder ownerWrite() {
		return lucidApplyToMode(Mode.IWUSR);
	}

	public static ModeBuilder ownerExecute() {
		return lucidApplyToMode(Mode.IXUSR);
	}

	public static ModeBuilder groupRead() {
		return lucidApplyToMode(Mode.IRGRP);
	}

	public static ModeBuilder groupWrite() {
		return lucidApplyToMode(Mode.IWGRP);
	}

	public static ModeBuilder groupExecute() {
		return lucidApplyToMode(Mode.IXGRP);
	}

	public static ModeBuilder othersRead() {
		return lucidApplyToMode(Mode.IROTH);
	}

	public static ModeBuilder othersWrite() {
		return lucidApplyToMode(Mode.IWOTH);
	}

	public static ModeBuilder othersExecute() {
		return lucidApplyToMode(Mode.IXOTH);
	}

	public static ModeBuilder numeric(int mask) {
		return lucidApplyToMode(mask);
	}

	public static Mode create() {
		ModeBuilder builder = instance();
		Mode m = Mode.createWithNumericValue(builder.mode);
		builder.reset();
		return m;
	}
}
