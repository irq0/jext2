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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.text.StrBuilder;

import fusejext2.FuseJExt2;

import jext2.exceptions.JExt2Exception;

public abstract class DataStructureAccessProvider<KEY,VAL> {
	protected Map<KEY, Data> table;

	Logger logger = Filesystem.getLogger();

	protected class Data {
		VAL value;
		long usage = 0;
		private ReentrantLock lock = new ReentrantLock(true);

		public void lock() {
			lock.lock();
		}

		public void unlock() {
			lock.unlock();
		}

		public String toString() {
			if (value instanceof Inode) {
				return "[Inode: " + ((Inode)value).getIno() + " #" + usage + "]";
			} else if (value instanceof DirectoryEntry) {
				return "[DirEntry: " + ((DirectoryEntry)value).getName() + " #" + usage + "]";
			} else {
				return "[" + value.getClass() + " #" + usage + "]";
			}
		}
	}

	private StackTraceElement[] filterStrackTraceForLog(StackTraceElement[] stack) {
		LinkedList<StackTraceElement> interresting = new LinkedList<StackTraceElement>();

		for (StackTraceElement element : stack) {
			if (element.getClassName().contains("jext2"))
				interresting.add(element);
		}

		return interresting.toArray(new StackTraceElement[0]);
	}

	protected void log(String op, String msg) {
		if (logger.isLoggable(Level.FINEST)) {
			StackTraceElement[] fullStack = Thread.currentThread().getStackTrace();
			StackTraceElement[] interrestingStackElements = filterStrackTraceForLog(fullStack);
			ArrayUtils.reverse(interrestingStackElements);

			String strstack = new StrBuilder().appendWithSeparators(interrestingStackElements, "->").toString();

			StringBuilder log = new StringBuilder();
			log.append(" class=");
			log.append(this.getClass().getSimpleName());
			log.append(" op=");
			log.append(op);
			log.append(" msg=");
			log.append(msg);
			log.append(" source=");
			log.append(strstack);

			logger.finest(log.toString());
		}

	}

	protected DataStructureAccessProvider(int initialCapacity) {
		table = new ConcurrentHashMap<KEY, Data>(initialCapacity, 0.75f, (int)(Math.ceil(FuseJExt2.numberOfThreads * 1.5f)));
	}


	protected abstract VAL createInstance(KEY key) throws JExt2Exception;


	protected long usageCounter(KEY key) {
		Data ds = getDataStructure(key);
		long usage;
		
		if (ds == null) {
			log("usageCounter","key:" + key + " counter:-1");
			return -1;
		} else {
			ds.lock();

			log("usageCounter","key:" + key + " counter:" + ds.usage);
			
			if (ds.usage < 0)
				usage = -1;
			else 
				usage = ds.usage;
			
			ds.unlock();
			return usage;
		}
	}

	/**
	 * Add new entry. Usage counter is zero
	 */
	protected void add(KEY key, VAL value) {
		assert value != null;

		Data ds = new Data();
		ds.lock();
		ds.value = value;

		table.put(key, ds);
		log("add","key:" + key);
		ds.unlock();
	}


	/**
	 * Open the entry. Increases usage counter and creates an instance if necessary
	 */
	protected VAL open(KEY key) throws JExt2Exception {
		Data ds = getDataStructure(key);
		VAL result;
		
		if (ds != null) {
			ds.lock();
			ds.usage += 1;
			result = ds.value;
			ds.unlock();
						
		} else {
			VAL val = createInstance(key);
			assert val != null;
		
			ds = new Data();
			ds.lock();
			ds.usage = 1;
			ds.value = val;
			result = val;
			
			add(key, val);
			ds.unlock();
		}

		assert result != null;

		log("open",":" + key);
		return result;
	}


	/**
	 * Retrieve entry already in table. Increase usage counter
	 */
	protected VAL retain(KEY key) {
		Data ds = getDataStructure(key);

		if (ds == null) {
			log("retain","nosuccess:" + key);
			return null;
		} else {
			ds.lock();
			ds.usage += 1;
			ds.unlock();

			assert ds.usage > 0;
			assert ds.value != null;

			log("retain","success:" + key);
			return ds.value;
		}
	}

	private Data getDataStructure(KEY key) {
		Data ds = table.get(key);
		return ds;
	}

	/**
	 * Retrieve entry already in table. Does not create a new one or change
	 * the usage counter
	 */
	protected VAL get(KEY key) {
		Data ds = getDataStructure(key);
		VAL result;

		if (ds == null) {
			log("get","nosuccess:" + key);
			result = null;
		} else {
			ds.lock();
			assert ds.value != null;
			log("get","success:" + key);
			result = ds.value;
			ds.unlock();
		}
		return result;
	}

	/**
	 * Release entry. Decreases usage counter and might remove entry from table
	 */
	protected void release(KEY key) {
		release(key, 1);
	}

	protected void release(KEY key, long times) {
		log("release","key:" + key + " times=" + times);

		Data ds = table.get(key);
		if (ds != null) {
			ds.lock();

			ds.usage -= times;

			if (ds.usage <= 0) {
				table.remove(key);
				log("release","removed:" + key);

			}
			ds.unlock();
		}
	}

	protected void remove(KEY key) {
		log("remove", "key:" + key);
		table.remove(key);
	}

	public String toString() {
		String s = new StringBuilder()
			.append(this.getClass().getCanonicalName())
			.append(": ")
			.append(table)
			.toString();
		return s;

	}
}
