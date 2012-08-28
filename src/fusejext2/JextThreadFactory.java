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
					.append(", ")
					.append(ExceptionUtils.getRootCauseMessage(e))
					.append("\n")
					.append(ExceptionUtils.getFullStackTrace(e))
					.toString());
				logger.severe("Shutting down due to unexpected exception..");
				System.exit(23);
			}
		});

		logger.info("Created new Thread: " + name);

		return t;
	}
}
