package jext2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Methods to test for compatibility of filesystem features */
public class Feature {
	private static Superblock superblock = Superblock.getInstance();
	
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
		StringBuffer buf = new StringBuffer("Filesystem features: ");
				
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
					} catch (IllegalArgumentException e) {
					} catch (IllegalAccessException e) {
					} catch (InvocationTargetException e) {
					}

			}
		}
		
		buf.append("\n");
		return buf.toString();
	}
	
	public static boolean compatSupported() {
		return hasCompatFeature(Constants.JEXT2_FEATURE_COMPAT_SUPP);
	}

	public static boolean incompatSupported() {
		return hasIncompatFeature(Constants.JEXT2_FEATURE_INCOMPAT_SUPP);
	}

	public static boolean roCompatSupported() {
		return hasRoCompatFeature(Constants.JEXT2_FEATURE_RO_COMPAT_SUPP);
	}

	public static boolean incompatUnsupported() {
		return hasIncompatFeature(Constants.JEXT2_FEATURE_INCOMPAT_UNSUPPORTED);
	}

	public static boolean roCompatUnsupported() {
		return hasRoCompatFeature(Constants.JEXT2_FEATURE_RO_COMPAT_UNSUPPORTED);
	}
	
	public static boolean dirPrealloc() {
		return hasCompatFeature(Constants.EXT2_FEATURE_COMPAT_DIR_PREALLOC); 			    
	}
	
	public static boolean imagicInodes() {		
		return hasCompatFeature(Constants.EXT2_FEATURE_COMPAT_IMAGIC_INODES);
	}
	
	public static boolean hasJournal() {
		return hasCompatFeature(Constants.EXT3_FEATURE_COMPAT_HAS_JOURNAL);
	}
	
	public static boolean extAttr() {
		return hasCompatFeature(Constants.EXT2_FEATURE_COMPAT_EXT_ATTR);
	}
	
	public static boolean resizeIno() {
		return hasCompatFeature(Constants.EXT2_FEATURE_COMPAT_RESIZE_INO);
	}
	
	public static boolean dirIndex() {
		return hasCompatFeature(Constants.EXT2_FEATURE_COMPAT_DIR_INDEX);
	}
	
	public static boolean compression() {
		return hasIncompatFeature(Constants.EXT2_FEATURE_INCOMPAT_COMPRESSION);
	}

	public static boolean filetype() {
		return hasIncompatFeature(Constants.EXT2_FEATURE_INCOMPAT_FILETYPE);
	}
	
	public static boolean recover() {
		return hasIncompatFeature(Constants.EXT3_FEATURE_INCOMPAT_RECOVER);
	}
	
	public static boolean journalDev() {
		return hasIncompatFeature(Constants.EXT3_FEATURE_INCOMPAT_JOURNAL_DEV);
	}
	
	public static boolean metaBg() {
		return hasIncompatFeature(Constants.EXT2_FEATURE_INCOMPAT_META_BG);
	}

	public static boolean sparseSuper() {
		return hasRoCompatFeature(Constants.EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER);
	}

	public static boolean largeFile() {
		return hasRoCompatFeature(Constants.EXT2_FEATURE_RO_COMPAT_LARGE_FILE);
	}

	public static boolean btreeDir() {
		return hasRoCompatFeature(Constants.EXT2_FEATURE_RO_COMPAT_BTREE_DIR);
	}
}
