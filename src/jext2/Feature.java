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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Methods to test for compatibility of filesystem features */
public class Feature {
	private static Superblock superblock = Superblock.getInstance();

	// Features
	public static final int EXT2_COMPAT_DIR_PREALLOC    = 0x0001;
	public static final int EXT2_COMPAT_IMAGIC_INODES   = 0x0002;
	public static final int EXT3_COMPAT_HAS_JOURNAL     = 0x0004;
	public static final int EXT2_COMPAT_EXT_ATTR        = 0x0008;
	public static final int EXT2_COMPAT_RESIZE_INO      = 0x0010;
	public static final int EXT2_COMPAT_DIR_INDEX       = 0x0020;
	public static final int EXT2_COMPAT_ANY             = 0xffffffff;
	public static final int EXT2_RO_COMPAT_SPARSE_SUPER = 0x0001;
	public static final int EXT2_RO_COMPAT_LARGE_FILE   = 0x0002;
	public static final int EXT2_RO_COMPAT_BTREE_DIR    = 0x0004;
	public static final int EXT2_RO_COMPAT_ANY          = 0xffffffff;
	public static final int EXT2_INCOMPAT_COMPRESSION   = 0x0001;
	public static final int EXT2_INCOMPAT_FILETYPE      = 0x0002;
	public static final int EXT3_INCOMPAT_RECOVER       = 0x0004;
	public static final int EXT3_INCOMPAT_JOURNAL_DEV   = 0x0008;
	public static final int EXT2_INCOMPAT_META_BG       = 0x0010;
	public static final int EXT2_INCOMPAT_ANY           = 0xffffffff;
	// Features supported by ext2fsprogs
	public static final int EXT2PROGS_COMPAT_SUPP           = 0;
	public static final int EXT2PROGS_INCOMPAT_SUPP         = (EXT2_INCOMPAT_FILETYPE);
	public static final int EXT2PROGS_RO_COMPAT_SUPP        = (EXT2_RO_COMPAT_SPARSE_SUPER|EXT2_RO_COMPAT_LARGE_FILE|EXT2_RO_COMPAT_BTREE_DIR);
	public static final int EXT2PROGS_RO_COMPAT_UNSUPPORTED = ~EXT2PROGS_RO_COMPAT_SUPP;
	public static final int EXT2PROGS_INCOMPAT_UNSUPPORTED  = ~EXT2PROGS_INCOMPAT_SUPP;
	// Features supported by linux 2.6.36
	public static final int EXT2KERNEL_COMPAT_SUPP           = (EXT2_COMPAT_EXT_ATTR);
	public static final int EXT2KERNEL_INCOMPAT_SUPP         = (EXT2_INCOMPAT_FILETYPE|EXT2_INCOMPAT_META_BG);
	public static final int EXT2KERNEL_RO_COMPAT_SUPP        = (EXT2_RO_COMPAT_SPARSE_SUPER|EXT2_RO_COMPAT_LARGE_FILE|EXT2_RO_COMPAT_BTREE_DIR);
	public static final int EXT2KERNEL_RO_COMPAT_UNSUPPORTED = ~EXT2KERNEL_RO_COMPAT_SUPP;
	public static final int EXT2KERNEL_INCOMPAT_UNSUPPORTED  = ~EXT2KERNEL_INCOMPAT_SUPP;
	// Features supported by jext2
	public static final int JEXT2_COMPAT_SUPP           = 0;
	public static final int JEXT2_INCOMPAT_SUPP         = (EXT2_INCOMPAT_FILETYPE);
	public static final int JEXT2_RO_COMPAT_SUPP        = (EXT2_RO_COMPAT_SPARSE_SUPER|EXT2_RO_COMPAT_LARGE_FILE);
	public static final int JEXT2_RO_COMPAT_UNSUPPORTED = ~JEXT2_RO_COMPAT_SUPP;
	public static final int JEXT2_INCOMPAT_UNSUPPORTED  = ~JEXT2_INCOMPAT_SUPP;

	/** Check if file system has compatible feature. The implementation is
	 * free to support them without risk of damaging meta-data.
	 */
	public static boolean hasCompatFeature(int feature) {
		return (superblock.getFeaturesCompat() & feature) > 0;
	}

	/** Check if file system has incompatible feature. The implementation should
	 * refuse to mount the file system if any indicated feature is unsupported
	 */
	public static boolean hasIncompatFeature(int feature) {
		return (superblock.getFeaturesIncompat() & feature) > 0;
	}

	/** Check if file system has r/o compatible feature. The implementation should
	 * only permit read-only mounting if any of the indicated features is
	 * unsupported */
	public static boolean hasRoCompatFeature(int feature) {
		return (superblock.getFeaturesRoCompat() & feature) > 0;
	}

	/** Return a String with supported features. For simplicity this is
	 * a list of all methods returning true of this class */
	public static String supportedFeatures() {
		StringBuilder buf = new StringBuilder("Filesystem features: ");

		Class<Feature> c = Feature.class;
		for (Method m : c.getDeclaredMethods()) {
			int mod = m.getModifiers();
			String name = m.getName();

			if (!name.equals("supportedFeatures") && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					boolean ret = (Boolean)m.invoke(null);
					if (ret) {
						buf.append(m.getName());
						buf.append(" ");
					}
				} catch (IllegalArgumentException ignored) {
				} catch (IllegalAccessException ignored) {
				} catch (InvocationTargetException ignored) {
				}

			}
		}

		buf.append("\n");
		return buf.toString();
	}

	public static boolean compatSupported() {
		return hasCompatFeature(JEXT2_COMPAT_SUPP);
	}

	public static boolean incompatSupported() {
		return hasIncompatFeature(JEXT2_INCOMPAT_SUPP);
	}

	public static boolean roCompatSupported() {
		return hasRoCompatFeature(JEXT2_RO_COMPAT_SUPP);
	}

	public static boolean incompatUnsupported() {
		return hasIncompatFeature(JEXT2_INCOMPAT_UNSUPPORTED);
	}

	public static boolean roCompatUnsupported() {
		return hasRoCompatFeature(JEXT2_RO_COMPAT_UNSUPPORTED);
	}

	public static boolean dirPrealloc() {
		return hasCompatFeature(EXT2_COMPAT_DIR_PREALLOC);
	}

	public static boolean imagicInodes() {
		return hasCompatFeature(EXT2_COMPAT_IMAGIC_INODES);
	}

	public static boolean hasJournal() {
		return hasCompatFeature(EXT3_COMPAT_HAS_JOURNAL);
	}

	public static boolean extAttr() {
		return hasCompatFeature(EXT2_COMPAT_EXT_ATTR);
	}

	public static boolean resizeIno() {
		return hasCompatFeature(EXT2_COMPAT_RESIZE_INO);
	}

	public static boolean dirIndex() {
		return hasCompatFeature(EXT2_COMPAT_DIR_INDEX);
	}

	public static boolean compression() {
		return hasIncompatFeature(EXT2_INCOMPAT_COMPRESSION);
	}

	public static boolean filetype() {
		return hasIncompatFeature(EXT2_INCOMPAT_FILETYPE);
	}

	public static boolean recover() {
		return hasIncompatFeature(EXT3_INCOMPAT_RECOVER);
	}

	public static boolean journalDev() {
		return hasIncompatFeature(EXT3_INCOMPAT_JOURNAL_DEV);
	}

	public static boolean metaBg() {
		return hasIncompatFeature(EXT2_INCOMPAT_META_BG);
	}

	public static boolean sparseSuper() {
		return hasRoCompatFeature(EXT2_RO_COMPAT_SPARSE_SUPER);
	}

	public static boolean largeFile() {
		return hasRoCompatFeature(EXT2_RO_COMPAT_LARGE_FILE);
	}

	public static boolean btreeDir() {
		return hasRoCompatFeature(EXT2_RO_COMPAT_BTREE_DIR);
	}


}
