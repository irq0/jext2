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

	protected DataStructureAccessProvider() {
		table = new ConcurrentHashMap<KEY, Data>();
	}


	protected abstract VAL createInstance(KEY key) throws JExt2Exception;


	protected long usageCounter(KEY key) {
		Data ds = getValueAndUsage(key);

		if (ds == null || ds.usage < 0) {
			log("usageCounter","key:" + key + " counter:-1");
			return -1;
		} else {
			log("usageCounter","key:" + key + " counter:" + ds.usage);
			return ds.usage;
		}
	}

	/**
	 * Add new entry. Usage counter is zero
	 */
	protected void add(KEY key, VAL value) {
		assert value != null;

		Data ds = new Data();
		ds.value = value;

		table.put(key, ds);
		log("add","key:" + key);
	}


	/**
	 * Open the entry. Increases usage counter and creates an instance if necessary
	 */
	protected VAL open(KEY key) throws JExt2Exception {
		Data ds = table.get(key);

		if (ds == null) {
			add(key, createInstance(key));
			ds = getValueAndUsage(key);
		}

		assert ds != null;

		ds.lock();
		ds.usage += 1;
		ds.unlock();

		log("open",":" + key);
		return ds.value;
	}


	/**
	 * Retrieve entry already in table. Increase usage counter
	 */
	protected VAL retain(KEY key) {
		Data ds = table.get(key);

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

	private Data getValueAndUsage(KEY key) {
		Data ds = table.get(key);
		return ds;
	}

	/**
	 * Retrieve entry already in table. Does not create a new one or change
	 * the usage counter
	 */
	protected VAL get(KEY key) {
		Data ds = getValueAndUsage(key);

		if (ds == null || ds.usage <= 0) {
			log("get","nosuccess:" + key);
			return null;
		} else {
			assert ds.value != null;
			log("get","success:" + key);
			return ds.value;
		}
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
		ds.lock();
		if (ds != null) {
			ds.usage -= times;

			if (ds.usage <= 0) {
				table.remove(key);
				log("release","removed:" + key);

			}
		}

		ds.unlock();
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
