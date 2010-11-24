package jext2;


import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Iterator;


class JExt2 {
	private RandomAccessFile blockDevFile;
	private FileChannel blockDev;
	private BlockAccess blocks;
	private Superblock superblock;
	
	void initializeFilesystem(String filename) {
		try {
			this.blockDevFile = new RandomAccessFile(filename, "rw");
			this.blockDev = blockDevFile.getChannel();
			this.blocks = new BlockAccess(blockDev);
			this.superblock = Superblock.fromBlockAccess(blocks);
			this.blocks.setBlocksize(superblock.getBlocksize());

			System.out.println(superblock);
			
			BlockGroupDescriptor descr = BlockGroupDescriptor.fromByteBuffer(blocks.getAtOffset(2));
			System.out.println(descr);

			for (int i=1; i<superblock.getInodesCount()-superblock.getFreeInodesCount(); i++) {				
				if(getInode(i).getUid() == 23) {
					System.out.println("asopdfjklsadjfklsdajfklsadjfklsdaf");
				} 
			}

			Inode root = getInode(Constants.EXT2_ROOT_INO);
  
			System.out.print("\n\n\n\nROOT\n");
			for (DirectoryEntry dir : (DirectoryInode)root) {
				System.out.println(dir);
			}
			
			System.out.println(((DirectoryInode)root).lookup("ENTEENTE"));


			Inode first = getInode(superblock.getFirstIno());

			System.out.print("\n\n\n\nFIRST\n");
			for (DirectoryEntry dir : (DirectoryInode)first) {
				System.out.println(dir);
			}

			System.out.println(((DirectoryInode)first).lookup("ENTEENTE"));

			System.out.println("\n\n\n\n\n42\n");
			Inode fourtytwo = getInode(13);
			ByteBuffer data = ((RegInode)fourtytwo).read(fourtytwo.getSize(), 0);
			System.out.println(fourtytwo);
			
			data.position(0);
			while (data.hasRemaining()) {
				System.out.print((char)data.get());
			}


			
			System.out.println("FIN");
			




			
			
		} catch (java.io.FileNotFoundException e) {
			
		} catch (java.io.IOException e) {
			
		}
	}

	public static void main(String[] args) throws IOException {	   
		JExt2 fs = new JExt2();		
		fs.initializeFilesystem(args[0]);
		
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
	



	public int groupOfBlk(int blk) {
		return (blk - superblock.getFirstDataBlock()) /
			superblock.getBlocksPerGroup();
	}

	public int groupOfIno(int ino) {
		return (ino - 1) / superblock.getInodesPerGroup();
	}

	public int groupFirstBlock(int group) {
		return superblock.getFirstDataBlock() +
			(group * superblock.getBlocksPerGroup());
	}   

	public int localInodeIndex(int ino) {
		return (ino - 1) % superblock.getInodesPerGroup();
	}

	public int localInodeOffset(int ino) {
		return ((ino - 1) % superblock.getInodesPerGroup()) *
			superblock.getInodeSize();
	}

	public int firstBlockOfGroup(int group) {
		return superblock.getFirstDataBlock() + 1 +
			group * superblock.getBlocksPerGroup();
	}

	public int blockPerInodeTable() {
		return superblock.getInodesPerGroup() / superblock.getInodeSize();
	}
			
	
	public Inode getInode(int ino) throws IOException {
		if (ino == 0 || ino > superblock.getInodesCount()) {
			return null;
		}
		
		int group = groupOfIno(ino);
		int offset = localInodeOffset(ino);
		int tblBlock = offset / superblock.getBlocksize();

		BlockGroupDescriptor descr =
			BlockGroupDescriptor.fromByteBuffer(blocks.getAtOffset
			                                    (firstBlockOfGroup(group)));

		int absBlock = descr.getInodeTable() + tblBlock;

		System.out.println(descr);
		System.out.println("getInode("+ino+"\n"+
		                   " tblBlock="+tblBlock+"\n"+
		                   " offset="+offset+"\n"+
		                   " group="+group+"\n"+
		                   " absBlock="+absBlock);

		int relOffset = offset - (tblBlock * superblock.getBlocksize());
		
		ByteBuffer table = blocks.getAtOffset(absBlock);
		//		Inode inode = Inode.fromByteBuffer(table, relOffset);
		Inode inode = InodeAccess.fromByteBuffer(table, relOffset);
		
		System.out.println(inode);
  
		return inode;
		
	}

	
}
