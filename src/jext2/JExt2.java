package jext2;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import jext2.exceptions.IoError;

class JExt2 {
	private String  blockDevFilename;
	private Superblock superblock;
	private BlockGroupAccess blockGroups;
	
	private void initializeFilesystem(String filename) {
		try {
			blockDevFilename = filename;
			RandomAccessFile blockDevFile = new RandomAccessFile(filename, "rw");
			FileChannel blockDev = blockDevFile.getChannel();
			BlockAccess blocks = new BlockAccess(blockDev);
			superblock = Superblock.fromBlockAccess(blocks);
			blocks.initialize(superblock);

			blockGroups = new BlockGroupAccess();
			blockGroups.readDescriptors();
			
		} catch (java.io.FileNotFoundException e) {
			System.out.println("Cannot open block device.. exiting");
			System.exit(23);
		} catch (IoError e) {
		    System.out.println("Unrecoverable IO error occurred.. dying");
			e.printStackTrace();			
		}
	}

	private void printMeta() {
		System.out.println("JExt2 - Java EXT2 Filesystem Implementation");
		System.out.println(" blockdev=" + blockDevFilename);
		System.out.println(superblock);
		
		System.out.println(superblock.getGroupsCount() + " Block groups on filesystem:");
		for (BlockGroupDescriptor d : blockGroups.iterateBlockGroups()) {
			System.out.println(d);
		}
		
		System.out.println(Feature.supportedFeatures());
		
		System.out.println("Root Inode: ");
		try {
			System.out.println(InodeAccess.readByIno(Constants.EXT2_ROOT_INO));
			System.out.println(InodeAccess.readByIno(14));
			System.out.println(InodeAccess.readByIno(15));

		} catch (Exception ignored) {
		}
	}
	
	private void checkFeatures() {
		if (Feature.incompatUnsupported() || Feature.roCompatUnsupported()) {
			System.out.println("Feature set incompatible with JExt2 :(");
			System.exit(23);
		}		
	}
	
	public static void main(String[] args) {
		try { 
			
			JExt2 fs = new JExt2();		
			fs.initializeFilesystem(args[0]);
			fs.checkFeatures();
			fs.printMeta();		
			
		} catch (Exception e) {
			System.err.println("Some error occurred :(");
			System.err.println("MESSAGE: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void dumpByteBuffer(ByteBuffer buf, int offset) {
		buf.position(offset);
		System.out.println(buf);
		for (int i=0; i<buf.capacity()-offset; i += 4) {
			System.out.print("[" + (i+offset) +"]  ");
			for (int k=i; k<i+4; k++) {
				System.out.print("| " + (buf.get(k+offset) & 0xff) + " |") ;
			}
	
			System.out.println();
		}
		buf.position(offset);

	}

	
}
