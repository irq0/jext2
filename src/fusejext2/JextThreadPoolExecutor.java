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

package fusejext2;

import java.util.concurrent.ArrayBlockingQueue;
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

	class JextCallerRunsPolicy extends CallerRunsPolicy {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			logger.warning("Throttling execution: running task in FUSE thread");
			logExecutorStatus();

			super.rejectedExecution(r, e);
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

	public JextThreadPoolExecutor(int numberOfThreads, int queueLength) {
		this(numberOfThreads,
				numberOfThreads*10,
				5,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(queueLength)
				);

		setThreadFactory(new JextThreadFactory());
		setRejectedExecutionHandler(new JextCallerRunsPolicy());
		logger.info("Starting Thread Pool Executor with " + numberOfThreads + " Threads");
	}

	private JextThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
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
