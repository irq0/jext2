package jext2;

import jext2.exceptions.JExt2Exception;

public class DirectoryEntryAccess extends DataStructureAccessProvider<String, DirectoryEntry> {
	DirectoryInode inode;


	private DirectoryEntryAccess(DirectoryInode inode) {
		this.inode = inode;
	}

	static DirectoryEntryAccess createForDirectoy(DirectoryInode inode) {
		return new DirectoryEntryAccess(inode);
	}

	public void add(DirectoryEntry entry) {
		if (entry.isUnused()) return;
		assert entry != null;
		add(entry.getName(), entry);
	}

	public void release(DirectoryEntry entry) {
		if (entry.isUnused()) return;
		assert entry != null;
		assert hasEntry(entry);
		release(entry.getName());
	}

	public DirectoryEntry retain(DirectoryEntry entry) {
		if (entry.isUnused()) return null;
		assert entry != null;
		assert hasEntry(entry);
		return retain(entry.getName());
	}

	public long usageCounter(DirectoryEntry entry) {
		if (entry.isUnused()) return 0;
		return usageCounter(entry.getName());
	}

	public boolean hasEntry(DirectoryEntry entry) {
		if (entry.isUnused()) return true;
		assert entry != null;
		boolean result;

		lock.lock();
		result = table.containsKey(entry.getName());
		lock.unlock();

		return result;
	}

	@Override
	protected DirectoryEntry createInstance(String key) throws JExt2Exception {
		throw new RuntimeException("Don't use this function!");
	}

}
