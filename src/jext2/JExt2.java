package jext2;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;


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
		for (BlockGroupDescriptor d : blockGroups.iterateBlockGroups()) {
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
	
	public static void main(String[] args) {
		try { 
			
			JExt2 fs = new JExt2();		
			fs.initializeFilesystem(args[0]);
			fs.checkFeatures();
			fs.printMeta();		
			
			DirectoryInode root = (DirectoryInode)(InodeAccess.readRootInode());			
			DirectoryEntry dir42 = root.lookup("42");
			
			Inode newinode = InodeAccess.readByIno(dir42.getIno());
			newinode.write();
			
			System.out.println("NEW INODE: \n" + newinode);
			DataBlockAccess inodeData = ((RegularInode)newinode).accessData();
			LinkedList<Long> block = inodeData.getBlocks(6, 1);
			System.out.println("blocks: " + block);
			BlockGroupDescriptor bg = BlockGroupAccess.getInstance().getGroupDescriptor(0);
			Bitmap bmap = Bitmap.fromByteBuffer(BlockAccess.getInstance().read(bg.getBlockBitmapPointer()), bg.getBlockBitmapPointer());
			System.out.println(bmap);
			bmap.setBit(4, true);
            System.out.println(bmap);
			
			ByteBuffer buf = BlockAccess.getInstance().read(block.getFirst());
			Ext2fsDataTypes.putString(buf, "TESTTESTTESTTEST", 16, 0);
			BlockAccess.getInstance().write(block.getFirst().intValue(), buf);
            System.out.println("NEW INODE: \n" + newinode);
			newinode.write();
			
		} catch (IOException e) {
			System.err.println("IO Exception - possibly something with block device..");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Some error occured :(");
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
