package jext2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jext2.exceptions.JExt2Exception;

public abstract class DataStructureAccessProvider<KEY,VAL> {
	private Map<KEY, ValueAndUsage> table;
	private ReentrantLock lock;
		
	private class ValueAndUsage {
		VAL value;
		long usage = 0;
	}

	protected DataStructureAccessProvider() {
		table = new HashMap<KEY, ValueAndUsage>();
	}
	
	
	protected abstract VAL createInstance(KEY key) throws JExt2Exception;
	
	/**
	 * Open the entry. Increases usage counter and creates an instance if necessary
	 */
	protected VAL open(KEY key) throws JExt2Exception {
		lock.lock();
		
		ValueAndUsage ds = table.get(key);
		
		if (ds == null) {
			ds = new ValueAndUsage();
			ds.value = createInstance(key);
			table.put(key, ds);
		}
		
		assert ds != null;	
		ds.usage += 1;
		
		lock.unlock();	
		return ds.value;
	}
	
	
	/**
	 * Retrieve entry already in table. Increase usage counter
	 */
	protected VAL retain(KEY key) {
		lock.lock();
		ValueAndUsage ds = table.get(key);

		if (ds == null || ds.usage <= 0) {
			lock.unlock();	
			return null;
		} else {
			lock.unlock();
			assert ds.value != null;
			ds.usage += 1;
			return ds.value;
		}
	}

	/**
	 * Retrieve entry already in table. Does not create a new one or change 
	 * the usage counter 
	 */
	protected VAL get(KEY key) {
		lock.lock();
		ValueAndUsage ds = table.get(key);

		if (ds == null || ds.usage <= 0) {
			lock.unlock();	
			return null;
		} else {
			lock.unlock();
			assert ds.value != null;
			return ds.value;
		}
	}
	
	/**
	 * Close an entry. Decreases usage counter and might remove entry from table
	 */
	protected void close(KEY key) {
		lock.lock();

		ValueAndUsage ds = table.get(key);
		assert ds != null;
		
		ds.usage -= 1;
		
		if (ds.usage <= 0) {
			table.remove(key);
		}
		
		lock.unlock();	
	}
}
