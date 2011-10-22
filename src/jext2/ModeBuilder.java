package jext2;

/**
 * Builder class for Mode
 */
public class ModeBuilder {

	int mode = 0;
	boolean filetypeApplied = false;
	private static ModeBuilder myInstance = new ModeBuilder();

	private void maskMode(int mask) {
		this.mode = this.mode & mask;
	}

	private void addToMode(int add) {
		this.mode = this.mode | add;
	}

	private void addToModeAndCheckFiletype(int mask) {
		if (filetypeApplied)
			throw new IllegalArgumentException("A file can only have one filetype");

		addToMode(mask);
	}

	private static ModeBuilder lucidAddToMode(int add) {
		ModeBuilder m = instance();
		m.addToMode(add);
		return m;
	}

	private static ModeBuilder lucidMaskMode(int mask) {
		ModeBuilder m = instance();
		m.maskMode(mask);
		return m;
	}

	private static ModeBuilder lucidAddToModeAndCheckFiletype(int mask) {
		ModeBuilder m = instance();
		m.addToModeAndCheckFiletype(mask);
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
		return lucidAddToModeAndCheckFiletype(Mode.IFREG);
	}

	public static ModeBuilder directory() {
		return lucidAddToModeAndCheckFiletype(Mode.IFDIR);
	}

	public static ModeBuilder socket() {
		return lucidAddToModeAndCheckFiletype(Mode.IFSOC);
	}

	public static ModeBuilder link() {
		return lucidAddToModeAndCheckFiletype(Mode.IFLNK);
	}

	public static ModeBuilder blockDevice() {
		return lucidAddToModeAndCheckFiletype(Mode.IFBLK);
	}

	public static ModeBuilder characterDevice() {
		return lucidAddToModeAndCheckFiletype(Mode.IFCHR);
	}

	public static ModeBuilder fifo() {
		return lucidAddToModeAndCheckFiletype(Mode.IFIFO);
	}

	public ModeBuilder setUid() {
		return lucidAddToMode(Mode.ISUID);
	}

	public ModeBuilder setGid() {
		return lucidAddToMode(Mode.ISGID);
	}

	public ModeBuilder sticky() {
		return lucidAddToMode(Mode.ISVTX);
	}

	public ModeBuilder ownerRead() {
		return lucidAddToMode(Mode.IRUSR);
	}

	public ModeBuilder ownerWrite() {
		return lucidAddToMode(Mode.IWUSR);
	}

	public ModeBuilder ownerExecute() {
		return lucidAddToMode(Mode.IXUSR);
	}
	
	public ModeBuilder ownerReadWriteExecute() {
		return lucidAddToMode(Mode.IRWXU);
	}

	public ModeBuilder groupRead() {
		return lucidAddToMode(Mode.IRGRP);
	}

	public ModeBuilder groupWrite() {
		return lucidAddToMode(Mode.IWGRP);
	}

	public ModeBuilder groupExecute() {
		return lucidAddToMode(Mode.IXGRP);
	}
	
	public ModeBuilder groupReadWriteExecute() {
		return lucidAddToMode(Mode.IRWXG);
	}

	public ModeBuilder othersRead() {
		return lucidAddToMode(Mode.IROTH);
	}

	public ModeBuilder othersWrite() {
		return lucidAddToMode(Mode.IWOTH);
	}

	public ModeBuilder othersExecute() {
		return lucidAddToMode(Mode.IXOTH);
	}
	
	public ModeBuilder othersReadWriteExecute() {
		return lucidAddToMode(Mode.IRWXO);
	}

	public ModeBuilder numeric(int add) {
		return lucidAddToMode(add);
	}

	public ModeBuilder mask(int mask) {
		return lucidMaskMode(mask);
	}
	
	public ModeBuilder allReadWriteExecute() {
		return ownerReadWriteExecute().groupReadWriteExecute().othersReadWriteExecute();
	}

	public Mode create() {
		Mode m = Mode.createWithNumericValue(mode);
		reset();
		return m;
	}
}
