package jext2;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// TODO make this a non static class which contains all pointers to runtime:
//      filesystem data like superblock, blockaccess, etc.
//      This then should replace the direct superblock, etc. references in each class
public class Filesystem {
	private static Charset charset = Charset.defaultCharset();
	private static Logger logger = Logger.getLogger("jext2");

	static class Jext2Formatter extends Formatter {
		private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		@Override
		public String format(LogRecord record) {
			StringBuilder s = new StringBuilder(1000)
			// Timestamp
			.append(df.format(new Date(record.getMillis())))
			.append("  ")

			// Source class name
			.append("[")
			.append(record.getSourceClassName())
			.append(".")
			.append(record.getSourceMethodName())
			.append("]")
			.append("  ")

			// Loglevel
			.append(record.getLevel())
			.append("  ")

			// Message
			.append(formatMessage(record))
			.append("\n");
			return s.toString();
		}
	}





	public static Charset getCharset() {
		return charset;
	}
	public static void setCharset(Charset charset) {
		Filesystem.charset = charset;
	}

	/**
	 * Get the PID of the running process
	 */
	public static long getPID() {
		String appName = ManagementFactory.getRuntimeMXBean().getName();
		String strPid = appName.substring(0, appName.indexOf('@')-1);
		return Long.parseLong(strPid);
	}

	public static void initializeLoggingToFile(String logfile) throws IOException {
		FileHandler handler = new FileHandler(logfile);
		handler.setFormatter(new Jext2Formatter());

		logger.addHandler(handler);
		logger.setLevel(Level.ALL);
		logger.info("jext2 start");
	}

	public static Logger getLogger() {
		return logger;
	}

}
