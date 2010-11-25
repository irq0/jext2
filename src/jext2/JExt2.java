package jext2;


import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Iterator;


class JExt2 {
	private String  blockDevFilename;
	private RandomAccessFile blockDevFile;
	private FileChannel blockDev;
	private BlockAccess blocks;
	private Superblock superblock;
	private BlockGroupAccess blockGroups;
	
	void initializeFilesystem(String filename) {
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
			
		} catch (java.io.IOException e) {
			
		}
	}

	void printMeta() {
		System.out.println("JExt2 - Java EXT2 Filesystem Implementation");
		System.out.println(" blockdev=" + blockDevFilename);
		System.out.println(superblock);
		
		System.out.println(Calculations.groupCount() + " Block groups on filesystem:");
		for (BlockGroupDescriptor d : blockGroups.iterateBlockGroups()) {
			System.out.println(d);
		}
		
		System.out.println(Feature.supportedFeatures());
		
	}
	
	
	public static void main(String[] args) throws IOException {	   
		JExt2 fs = new JExt2();		
		fs.initializeFilesystem(args[0]);
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
