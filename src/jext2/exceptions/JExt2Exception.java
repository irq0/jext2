package jext2.exceptions;

import java.util.logging.Logger;

import jext2.Filesystem;

public class JExt2Exception extends Exception {
	protected static final int ERRNO = -1;

	private static final long serialVersionUID = -7429088074385678308L;

	public JExt2Exception() {
		log("");
	}

	public JExt2Exception(String msg) {
		log(msg);
	}

	private void log(String msg) {
		Logger logger = Filesystem.getLogger();
		if (logger == null) return;

		StackTraceElement[] stack = getStackTrace();

		StringBuilder log = new StringBuilder();
		log.append("!!! EXCEP ");
		log.append(" exception=");
		log.append(this.getClass().getSimpleName());
		log.append(" source=");
		log.append(stack[0].getClassName());
		log.append("->");
		log.append(stack[0].getMethodName());
		log.append(" msg=");
		log.append(msg);
		log.append(" errno=");
		log.append(getErrno());

		logger.fine(log.toString());
	}

	public int getErrno() {
		throw new RuntimeException("This method should not be executed. " +
				"- The errno value defined here is meaningless");
	}
}
