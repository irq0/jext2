package fusejext2;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import jext2.Filesystem;

public class JextThreadFactory implements ThreadFactory {
	private Logger logger = Filesystem.getLogger();
	private String threadPrefix = "jext2thread";
	
	AtomicInteger count = new AtomicInteger(1);
		
	@Override
	public synchronized Thread newThread(Runnable r) {
		final String name = new StringBuilder()
		.append(threadPrefix)
		.append("[")
		.append(count.getAndIncrement())
		.append("]").toString();

		Thread t = new Thread(r);

		t.setName(name);

		t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.severe(new StringBuffer()
					.append("Uncaught Exception in thread ")
					.append(name)
					.append("\n")
					.append(ExceptionUtils.getMessage(e))
					.append("\n")
					.append(ExceptionUtils.getFullStackTrace(e))
					.toString());
			}
		});

		logger.info("Created new Thread: " + name);

		return t;
	}
}