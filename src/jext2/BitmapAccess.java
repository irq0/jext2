/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

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
		super(100);
	}

	public Bitmap openInodeBitmap(BlockGroupDescriptor group) throws JExt2Exception{
		return open(group.getInodeBitmapPointer());

	}

	public Bitmap openDataBitmap(BlockGroupDescriptor group) throws JExt2Exception {
		return open(group.getBlockBitmapPointer());
	}

	public void closeBitmap(Bitmap bmap) {
		release(bmap.nr);
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
