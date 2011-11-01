package fusejext2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jext2.Filesystem;
import jlowfuse.async.tasks.JLowFuseTask;

public class JextThreadPoolExecutor extends ThreadPoolExecutor {
	Logger logger = Filesystem.getLogger();
	
	
	public JextThreadPoolExecutor(int numberOfThreads) {
		this(numberOfThreads, numberOfThreads, 23, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public JextThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	
	protected void beforeExecute(Thread t, Runnable r) {
		logger.fine(String.format("THREAD %s: Running task %s", t, r));
	}
	
	
	protected void afterExecute(Runnable r, Throwable t) {
		logger.fine(String.format("Task %s finished", r));
	}
	
}