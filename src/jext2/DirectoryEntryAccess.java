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
		assert !hasEntry(entry);
		add(entry.getName(), entry);
	}

	public void release(DirectoryEntry entry) {
		assert hasEntry(entry);
		release(entry.getName());
	}

	public DirectoryEntry retain(DirectoryEntry entry) {
		assert hasEntry(entry);
		return retain(entry.getName());
	}

	public long usageCounter(DirectoryEntry entry) {
		return usageCounter(entry.getName());
	}

	public boolean hasEntry(DirectoryEntry entry) {
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
