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

package jext2;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.text.StrBuilder;

public class JextReentrantReadWriteLock extends ReentrantReadWriteLock {
	private static final long serialVersionUID = 1469540317409758869L;

	final Logger logger = Filesystem.getLogger();
	
	public JextReentrantReadWriteLock(boolean fair) {
		super(fair);
	}
	
	private String[] getElementsThatRequestedLock(StackTraceElement[] stack) {
		LinkedList<String> interresting = new LinkedList<String>();
		
		for (StackTraceElement element : stack) {
			if ( element.getClassName().contains("jext2") &&
					!element.getClassName().contains("JextReentrantReadWriteLock")) {
				interresting.add(element.getFileName() + ":" + element.getLineNumber());
			} 
		}
		return interresting.toArray(new String[0]);
	}

	
	private void log(String message) {
		StackTraceElement[] fullStack = Thread.currentThread().getStackTrace();
		String[] interrestingStackElements = getElementsThatRequestedLock(fullStack);
		ArrayUtils.reverse(interrestingStackElements);
		String strstack = new StrBuilder().appendWithSeparators(interrestingStackElements, "->").toString();
		
		logger.finer(new StringBuilder()
			.append(" LOCK ")
			.append(message)
			.append(" Thread: ")
			.append(Thread.currentThread().getName())
			.append(" Current holds:")
			.append(" (read=")
			.append(getReadHoldCount())
			.append(" write=")
			.append(getWriteHoldCount())
			.append(")")
			.append(" Number of read locks: ")
			.append(getReadLockCount())
			.append(" Queued readers: ")
			.append(getQueuedReaderThreads())
			.append(" Queued writers: ")
			.append(getQueuedWriterThreads())
			.append(" Source: ")
			.append(strstack)
			.toString());
	}
	
	private void logIfLoggable(String message) {
		if (logger.isLoggable(Level.FINER)) {
			log(message);
		}
	}
			
	
	@Override
	public ReadLock readLock() {
		final ReentrantReadWriteLock.ReadLock l = super.readLock();
		return new ReadLock(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void lock() {
				logIfLoggable("read-lock()");
				l.lock();
			}

			@Override
			public void lockInterruptibly() throws InterruptedException {
				logIfLoggable("read-lock-inter()");
				l.lockInterruptibly();
			}

			@Override
			public Condition newCondition() {
				return l.newCondition();
			}

			@Override
			public boolean tryLock() {
				logIfLoggable("read-trylock()");
				return l.tryLock();
			}

			@Override
			public boolean tryLock(long arg0, TimeUnit arg1) throws InterruptedException {
				logIfLoggable("read-trylock()");
				return l.tryLock(arg0, arg1);
			}

			@Override
			public void unlock() {
				logIfLoggable("read-unlock()");
				l.unlock();			
			}
		};
	}

	@Override
	public WriteLock writeLock() {
		final ReentrantReadWriteLock.WriteLock l = super.writeLock();
		return new WriteLock(this) {
			private static final long serialVersionUID = -4345682740953361177L;

			@Override
			public int getHoldCount() {
				return l.getHoldCount();
			}

			@Override
			public boolean isHeldByCurrentThread() {
				return l.isHeldByCurrentThread();
			}

			@Override
			public void lock() {
				logIfLoggable("write-lock()");
				l.lock();
			}

			@Override
			public void lockInterruptibly() throws InterruptedException {
				logIfLoggable("write-lock-inter()");
				l.lockInterruptibly();
			}
			
			@Override
			public Condition newCondition() {
				return l.newCondition();
			}

			@Override
			public boolean tryLock() {
				logIfLoggable("write-trylock()");
				return l.tryLock();
			}

			@Override
			public boolean tryLock(long arg0, TimeUnit arg1) throws InterruptedException {
				logIfLoggable("write-trylock()");
				return l.tryLock(arg0, arg1);
			}

			@Override
			public void unlock() {
				logIfLoggable("write-unlock()");
				l.unlock();
				
			}
		};
	}
	
}
