package jext2;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


class JExt2 {
	private String  blockDevFilename;
	private RandomAccessFile blockDevFile;
	private FileChannel blockDev;
	private BlockAccess blocks;
	private Superblock superblock;
	private BlockGroupAccess blockGroups;
	
	private void initializeFilesystem(String filename) {
		try {
			blockDevFilename = filename;
			blockDevFile = new RandomAccessFile(filename, "rw");
			blockDev = blockDevFile.getChannel();
			blocks = new BlockAccess(blockDev);
			superblock = Superblock.fromBlockAccess(blocks);
			blocks.setBlocksize(superblock.getBlocksize());

			blockGroups = new BlockGroupAccess();
			blockGroups.readDescriptors();
			
		} catch (java.io.FileNotFoundException e) {
			System.out.println("Cannot open block device.. exiting");
			System.exit(23);
		} catch (java.io.IOException e) {
			System.out.println("Unrecoverable IO error occured.. dying");
			e.printStackTrace();			
		}
	}

	private void printMeta() {
		System.out.println("JExt2 - Java EXT2 Filesystem Implementation");
		System.out.println(" blockdev=" + blockDevFilename);
		System.out.println(superblock);
		
		System.out.println(superblock.getGroupsCount() + " Block groups on filesystem:");
		for (BlockGroup d : blockGroups.iterateBlockGroups()) {
			System.out.println(d);
		}
		
		System.out.println(Feature.supportedFeatures());
		
		System.out.println("Root Inode: ");
		try {
			System.out.println(InodeAccess.readByIno(Constants.EXT2_ROOT_INO));
			System.out.println(InodeAccess.readByIno(14));
			System.out.println(InodeAccess.readByIno(15));

		} catch (java.io.IOException e) {
		}
	}
	
	private void checkFeatures() {
		if (Feature.incompatUnsupported() || Feature.roCompatUnsupported()) {
			System.out.println("Featureset incompatible with JExt2 :(");
			System.exit(23);
		}		
	}
	
	public static void main(String[] args) throws IOException {	   
		JExt2 fs = new JExt2();		
		fs.initializeFilesystem(args[0]);
		fs.checkFeatures();
		fs.printMeta();		
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
