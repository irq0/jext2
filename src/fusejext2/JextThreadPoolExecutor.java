package fusejext2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jext2.Filesystem;

public class JextThreadPoolExecutor extends ThreadPoolExecutor {
	private Logger logger = Filesystem.getLogger();
	private int throttelingHaltTime = 100;
	private int throttelingQueueLength = 50;

	void logExecutorStatus() {
		logger.info(new StringBuilder()
			.append("Executor status")
			.append(" active_threads=")
			.append(getActiveCount())
			.append(" queue_length=")
			.append(getQueue().size())
			.append(" cur_threads_in_pool=")
			.append(getPoolSize())
			.append(" largest_pool_size=")
			.append(getLargestPoolSize())
			.append(" approx_completed_tasks=")
			.append(getCompletedTaskCount())
			.toString());
	}

	class ExecutorStatusDumper extends Thread {
		int intervallInMillis;
		public ExecutorStatusDumper(int intervalInMillis) {
			this.intervallInMillis = intervalInMillis;
			setDaemon(true);
			setName("jext2executorstatus");
		}

		@Override
		public void run() {
			while (true) {
				logExecutorStatus();
				try {
					sleep(intervallInMillis);
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	public void activateStatusDump(int intervallInMillis) {
		ExecutorStatusDumper t = new ExecutorStatusDumper(intervallInMillis);
		t.start();
	}

	public void activateThrottling(int queueLength, int haltTime) {
		throttelingHaltTime = haltTime;
		throttelingQueueLength = queueLength;
	}

	public JextThreadPoolExecutor(int numberOfThreads) {
		this(numberOfThreads,
				numberOfThreads*10,
				5,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());

		setThreadFactory(new JextThreadFactory());
		logger.info("Starting Thread Pool Executor with " + numberOfThreads + " Threads");
	}

	private JextThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}



	private void throttle() {
		try {
			logger.warning("Throtteling execution");
			logExecutorStatus();
			Thread.sleep(throttelingHaltTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void execute(Runnable command) {
		if (getQueue().size() > throttelingQueueLength)
			throttle();
		super.execute(command);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
	    super.beforeExecute(t, r);
	    if (logger.isLoggable(Level.FINER))
	    	logger.finer(String.format(">>> START thread=[%s] task=[%s]", t.getName(), r));
	}


	@Override
	protected void afterExecute(Runnable r, Throwable t) {
	    super.afterExecute(r, t);
	    if (logger.isLoggable(Level.FINER))
	    	logger.finer(String.format("<<< END task=[%s]", r));
	}

}
