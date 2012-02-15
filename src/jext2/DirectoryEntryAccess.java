package jext2;

import jext2.exceptions.JExt2Exception;

public class DirectoryEntryAccess extends DataStructureAccessProvider<String, DirectoryEntry> {
	DirectoryInode inode;


	private DirectoryEntryAccess(DirectoryInode inode) {
		super(100);
		this.inode = inode;
	}

	static DirectoryEntryAccess createForDirectoy(DirectoryInode inode) {
		return new DirectoryEntryAccess(inode);
	}

	public void add(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return;
		add(entry.getName(), entry);
	}

	public void remove(DirectoryEntry entry) {
		assert entry != null;

		remove(entry.getName());
	}

	public void release(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return;
//		assert hasEntry(entry);
		release(entry.getName());
	}

	public DirectoryEntry retain(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return null;
		assert hasEntry(entry);
		return retain(entry.getName());
	}

	public long usageCounter(DirectoryEntry entry) {
		if (entry.isUnused()) return 0;
		return usageCounter(entry.getName());
	}

	public DirectoryEntry retainAdd(DirectoryEntry entry) {
		assert entry != null;
		if (entry.isUnused()) return entry;

		DirectoryEntry result;
		
		Data d = table.get(entry.getName());
		if (d != null) {
			d.lock();
			d.usage += 1;
			result = d.value;
			d.unlock();

		} else {
			d = new Data();
			d.lock();
			d.usage = 1;
			d.value = entry;
			result = entry;

			table.put(entry.getName(), d);
			d.unlock();
		}

		return result;

	}

	public boolean hasEntry(DirectoryEntry entry) {
		assert entry != null;

		if (entry.isUnused()) return true;
		boolean result;

		result = table.containsKey(entry.getName());

		return result;
	}

	@Override
	protected DirectoryEntry createInstance(String key) throws JExt2Exception {
		throw new RuntimeException("Don't use this function!");
	}

}
