package jext2;

import java.nio.ByteBuffer;

import jext2.exceptions.JExt2Exception;

/** 
 * Access methods for bitmaps. Takes care that there is not more than one
 * Bitmap object for a bitmap
 * 
 */
public class BitmapAccess extends DataStructureAccessProvider<Long, Bitmap>{
	private static BitmapAccess _instance = new BitmapAccess();
	
	private static BlockAccess blocks = BlockAccess.getInstance();	

	private BitmapAccess() {
	}
	
	public Bitmap openInodeBitmap(BlockGroupDescriptor group) throws JExt2Exception{
		return open(group.getInodeBitmapPointer());
		
	}
	
	public Bitmap openDataBitmap(BlockGroupDescriptor group) throws JExt2Exception {
		return open(group.getBlockBitmapPointer());
	}
	
	public void closeBitmap(Bitmap bmap) {
		close(bmap.nr);
	}

	@Override
	protected Bitmap createInstance(Long blockNr) throws JExt2Exception {
		ByteBuffer buf = blocks.read(blockNr);
		return Bitmap.fromByteBuffer(buf, blockNr);
	}
	
	public static BitmapAccess getInstance() {
		return BitmapAccess._instance;
	}
}
