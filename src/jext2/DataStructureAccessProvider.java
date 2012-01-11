package jext2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.text.StrBuilder;

import jext2.exceptions.JExt2Exception;

public abstract class DataStructureAccessProvider<KEY,VAL> {
	protected Map<KEY, ValueAndUsage> table;
	protected ReentrantLock lock = new ReentrantLock(true);

	Logger logger = Filesystem.getLogger();

	protected class ValueAndUsage {
		VAL value;
		long usage = 0;
		
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
		if (logger.isLoggable(Level.FINE)) {
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

			logger.fine(log.toString());
		}
		
	}
	
	protected DataStructureAccessProvider() {
		table = new HashMap<KEY, ValueAndUsage>();
	}


	protected abstract VAL createInstance(KEY key) throws JExt2Exception;


	protected long usageCounter(KEY key) {
		ValueAndUsage ds = getValueAndUsage(key);

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

		ValueAndUsage ds = new ValueAndUsage();
		ds.value = value;

		lock.lock();
		table.put(key, ds);
		lock.unlock();
		log("add","key:" + key);
	}


	/**
	 * Open the entry. Increases usage counter and creates an instance if necessary
	 */
	protected VAL open(KEY key) throws JExt2Exception {
		lock.lock();
		ValueAndUsage ds = table.get(key);
		lock.unlock();

		if (ds == null) {
			add(key, createInstance(key));
			ds = getValueAndUsage(key);
		}

		assert ds != null;
		ds.usage += 1;
		
		log("open",":" + key);
		return ds.value;
	}


	/**
	 * Retrieve entry already in table. Increase usage counter
	 */
	protected VAL retain(KEY key) {
		lock.lock();
		ValueAndUsage ds = table.get(key);

		if (ds == null) {
			lock.unlock();
			log("retain","nosuccess:" + key);
			return null;
		} else {
			lock.unlock();
			
			assert ds.usage >= 0;
			assert ds.value != null;
			ds.usage += 1;
			log("retain","success:" + key);
			return ds.value;
		}
	}

	private ValueAndUsage getValueAndUsage(KEY key) {
		lock.lock();
		ValueAndUsage ds = table.get(key);
		lock.unlock();

		return ds;
	}

	/**
	 * Retrieve entry already in table. Does not create a new one or change
	 * the usage counter
	 */
	protected VAL get(KEY key) {
		ValueAndUsage ds = getValueAndUsage(key);

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
		log("release","key:" + key);
		lock.lock();

		ValueAndUsage ds = table.get(key);
		if (ds != null) {		
			ds.usage -= times;
			
			if (ds.usage <= 0) {
				table.remove(key);
				log("release","removed:" + key);

			}
		}

		lock.unlock();
	}
	
	protected void remove(KEY key) {
		log("remove", "key:" + key);
		lock.lock();
		table.remove(key);
		lock.unlock();
	}
	
	public String toString() {
		lock.lock();
		String s = new StringBuilder()
			.append(this.getClass().getCanonicalName())
			.append(": ")
			.append(table)
			.toString();
		lock.unlock();
		return s;
		
	}
}
