package jext2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Synchronization primitives for Blocks. A Block object with the same block number
 * might be at the same time in memory. Synchronization is thus based on the block number
 * not the object identity.
 */
public class BlockSynchronizer {
	private static BlockSynchronizer instance = new BlockSynchronizer();

	private Map<Long, ReentrantReadWriteLock> locks;
	private ReentrantLock locksLock;


	public BlockSynchronizer() {
		locks = new HashMap<Long, ReentrantReadWriteLock>();
		locksLock = new ReentrantLock(true);
	}

	private ReentrantReadWriteLock getLock(long nr) {
		locksLock.lock();

		ReentrantReadWriteLock lock = locks.get(nr);

		if (lock == null) {
			lock = new ReentrantReadWriteLock(true);
			locks.put(nr, lock);
		}

		locksLock.unlock();
		return lock;
	}

	private void removeLock(ReentrantReadWriteLock removeMe, long nr) {
		locksLock.lock();

		if (!removeMe.hasQueuedThreads()) {
			ReentrantReadWriteLock inMap = locks.get(nr);
			if (removeMe.equals(inMap)) {
				locks.remove(removeMe);
			}
		}

		locksLock.unlock();
	}


	public ReentrantReadWriteLock.ReadLock getReadLock(long nr) {
		ReentrantReadWriteLock lock = getLock(nr);
		return lock.readLock();
	}

	public ReentrantReadWriteLock.WriteLock getWriteLock(long nr) {
		ReentrantReadWriteLock lock = getLock(nr);
		return lock.writeLock();
	}

	public void readLock(long nr) {
		getReadLock(nr).lock();
	}

	public void writeLock(long nr) {
		getWriteLock(nr).lock();
	}

	public void readUnlock(long nr) {
		ReentrantReadWriteLock lock = getLock(nr);
		lock.readLock().unlock();
		removeLock(lock, nr);
	}

	public void writeUnlock(long nr) {
		ReentrantReadWriteLock lock = getLock(nr);
		lock.writeLock().unlock();
		removeLock(lock, nr);
	}

	public static BlockSynchronizer getInstance() {
		return BlockSynchronizer.instance;
	}
}
