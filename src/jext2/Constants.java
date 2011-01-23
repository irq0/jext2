package jext2;

public class Constants {
	// Special inode numbers   
	public static final int	EXT2_BAD_INO         = 1; /* Bad blocks inode */
	public static final int EXT2_ROOT_INO        = 2; /* Root inode */
	public static final int EXT2_BOOT_LOADER_INO = 5; /* Boot loader inode */
	public static final int EXT2_UNDEL_DIR_INO   = 6; /* Undelete directory inode */
		
	// First non-reserved inode for old ext2 filesystems 
	public static final int EXT2_GOOD_OLD_FIRST_INO	=11;
	
	// Maximal count of links to a file
	public static final int EXT2_LINK_MAX = 32000;

	public static final int EXT2_MIN_FRAG_SIZE = 1024;
	public static final int	EXT2_MAX_FRAG_SIZE = 4096;
	public static final int EXT2_MIN_FRAG_LOG_SIZE = 10;

 	public static final int	EXT2_NDIR_BLOCKS = 12;
 	public static final int	EXT2_IND_BLOCK   = EXT2_NDIR_BLOCKS;
 	public static final int	EXT2_DIND_BLOCK  = (EXT2_IND_BLOCK + 1);
 	public static final int	EXT2_TIND_BLOCK  = (EXT2_DIND_BLOCK + 1);
 	public static final int	EXT2_N_BLOCKS    = (EXT2_TIND_BLOCK + 1);
 
	// Mount flags
 	public static final int EXT2_MOUNT_CHECK        = 0x000001;	/* Do mount-time checks */
	public static final int EXT2_MOUNT_OLDALLOC		= 0x000002;	/* Don't use the new Orlov allocator */
	public static final int EXT2_MOUNT_GRPID		= 0x000004;	/* Create files with directory's group */
	public static final int EXT2_MOUNT_DEBUG		= 0x000008;	/* Some debugging messages */
	public static final int EXT2_MOUNT_ERRORS_CONT	= 0x000010;	/* Continue on errors */
	public static final int EXT2_MOUNT_ERRORS_RO	= 0x000020;	/* Remount fs ro on errors */
	public static final int EXT2_MOUNT_ERRORS_PANIC	= 0x000040;	/* Panic on errors */
	public static final int EXT2_MOUNT_MINIX_DF		= 0x000080;	/* Mimics the Minix statfs */
	public static final int EXT2_MOUNT_NOBH			= 0x000100;	/* No buffer_heads */
	public static final int EXT2_MOUNT_NO_UID32		= 0x000200; /* Disable 32-bit UIDs */
	public static final int EXT2_MOUNT_XATTR_USER	= 0x004000; /* Extended user attributes */
	public static final int EXT2_MOUNT_POSIX_ACL	= 0x008000; /* POSIX Access Control Lists */
	public static final int EXT2_MOUNT_XIP			= 0x010000;	/* Execute in place */
	public static final int EXT2_MOUNT_USRQUOTA		= 0x020000;	/* user quota */
	public static final int EXT2_MOUNT_GRPQUOTA		= 0x040000;	/* group quota */
	public static final int EXT2_MOUNT_RESERVATION	= 0x080000;	/* Preallocation */

	// File system states
	public static final int	EXT2_VALID_FS =	0x0001;	/* Unmounted cleanly */
	public static final int	EXT2_ERROR_FS =	0x0002;	/* Errors detected */
	
	// Maximal mount counts between two filesystem checks
	public static final int EXT2_DFL_MAX_MNT_COUNT = 20; /* Allow 20 mounts */
	public static final int EXT2_DFL_CHECKINTERVAL = 0;	 /* Don't use interval check */

	// Behaviour when detecting errors
	public static final int EXT2_ERRORS_CONTINUE= 1;	/* Continue execution */
	public static final int EXT2_ERRORS_RO		= 2;	/* Remount fs read-only */
	public static final int EXT2_ERRORS_PANIC	= 3;	/* Panic */
	public static final int EXT2_ERRORS_DEFAULT	= EXT2_ERRORS_CONTINUE;
	
	// Codes for operating systems
	public static final int EXT2_OS_LINUX   = 0;
	public static final int EXT2_OS_HURD    = 1;
	public static final int EXT2_OS_MASIX   = 2;
	public static final int EXT2_OS_FREEBSD = 3;
	public static final int EXT2_OS_LITES   = 4;

	// Revision levels
	public static final int EXT2_GOOD_OLD_REV = 0;	/* The good old (original) format */
	public static final int EXT2_DYNAMIC_REV  = 1; 	/* V2 format w/ dynamic inode sizes */

	public static final int EXT2_CURRENT_REV=	EXT2_GOOD_OLD_REV;
	public static final int EXT2_MAX_SUPP_REV=	EXT2_DYNAMIC_REV;

	public static final int EXT2_GOOD_OLD_INODE_SIZE= 128;

	// Features
	public static final int EXT2_FEATURE_COMPAT_DIR_PREALLOC    = 0x0001;
	public static final int EXT2_FEATURE_COMPAT_IMAGIC_INODES   = 0x0002;
	public static final int EXT3_FEATURE_COMPAT_HAS_JOURNAL     = 0x0004;
	public static final int EXT2_FEATURE_COMPAT_EXT_ATTR        = 0x0008;
	public static final int EXT2_FEATURE_COMPAT_RESIZE_INO      = 0x0010;
	public static final int EXT2_FEATURE_COMPAT_DIR_INDEX       = 0x0020;
	public static final int EXT2_FEATURE_COMPAT_ANY             = 0xffffffff;
	
	public static final int EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER = 0x0001;
	public static final int EXT2_FEATURE_RO_COMPAT_LARGE_FILE   = 0x0002;
	public static final int EXT2_FEATURE_RO_COMPAT_BTREE_DIR    = 0x0004;
	public static final int EXT2_FEATURE_RO_COMPAT_ANY          = 0xffffffff;
	
	public static final int EXT2_FEATURE_INCOMPAT_COMPRESSION   = 0x0001;
	public static final int EXT2_FEATURE_INCOMPAT_FILETYPE      = 0x0002;
	public static final int EXT3_FEATURE_INCOMPAT_RECOVER       = 0x0004;
	public static final int EXT3_FEATURE_INCOMPAT_JOURNAL_DEV   = 0x0008;
	public static final int EXT2_FEATURE_INCOMPAT_META_BG       = 0x0010;
	public static final int EXT2_FEATURE_INCOMPAT_ANY           = 0xffffffff;

	// Features supported by ext2fsprogrs impl
	public static final int EXT2PROGS_FEATURE_COMPAT_SUPP           = 0;
	public static final int EXT2PROGS_FEATURE_INCOMPAT_SUPP         = (EXT2_FEATURE_INCOMPAT_FILETYPE);
	public static final int EXT2PROGS_FEATURE_RO_COMPAT_SUPP        = (EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER|EXT2_FEATURE_RO_COMPAT_LARGE_FILE|EXT2_FEATURE_RO_COMPAT_BTREE_DIR);
	public static final int EXT2PROGS_FEATURE_RO_COMPAT_UNSUPPORTED = ~EXT2PROGS_FEATURE_RO_COMPAT_SUPP;
	public static final int EXT2PROGS_FEATURE_INCOMPAT_UNSUPPORTED  = ~EXT2PROGS_FEATURE_INCOMPAT_SUPP;

	// Features supported by linux 2.6.36
	public static final int EXT2KERNEL_FEATURE_COMPAT_SUPP           = (EXT2_FEATURE_COMPAT_EXT_ATTR);
	public static final int EXT2KERNEL_FEATURE_INCOMPAT_SUPP         = (EXT2_FEATURE_INCOMPAT_FILETYPE|EXT2_FEATURE_INCOMPAT_META_BG);
	public static final int EXT2KERNEL_FEATURE_RO_COMPAT_SUPP        = (EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER|EXT2_FEATURE_RO_COMPAT_LARGE_FILE|EXT2_FEATURE_RO_COMPAT_BTREE_DIR);
	public static final int EXT2KERNEL_FEATURE_RO_COMPAT_UNSUPPORTED = ~EXT2KERNEL_FEATURE_RO_COMPAT_SUPP;
	public static final int EXT2KERNEL_FEATURE_INCOMPAT_UNSUPPORTED  = ~EXT2KERNEL_FEATURE_INCOMPAT_SUPP;

	// Features supported by jext2
	public static final int JEXT2_FEATURE_COMPAT_SUPP           = 0;
	public static final int JEXT2_FEATURE_INCOMPAT_SUPP         = (EXT2_FEATURE_INCOMPAT_FILETYPE);
	public static final int JEXT2_FEATURE_RO_COMPAT_SUPP        = (EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER);
	public static final int JEXT2_FEATURE_RO_COMPAT_UNSUPPORTED = ~JEXT2_FEATURE_RO_COMPAT_SUPP;
	public static final int JEXT2_FEATURE_INCOMPAT_UNSUPPORTED  = ~JEXT2_FEATURE_INCOMPAT_SUPP;
	
	
	// Default values for user and/or group using reserved blocks
	public static final int	EXT2_DEF_RESUID = 0;
	public static final int	EXT2_DEF_RESGID = 0;

	// Default mount options
	public static final int EXT2_DEFM_DEBUG         = 0x0001;
	public static final int EXT2_DEFM_BSDGROUPS     = 0x0002;
	public static final int EXT2_DEFM_XATTR_USER    = 0x0004;
	public static final int EXT2_DEFM_ACL           = 0x0008;
	public static final int EXT2_DEFM_UID16         = 0x0010;
	/* Not used by ext2, but reserved for use by ext3 */
	public static final int EXT3_DEFM_JMODE         = 0x0060; 
	public static final int EXT3_DEFM_JMODE_DATA    = 0x0020;
	public static final int EXT3_DEFM_JMODE_ORDERED = 0x0040;
	public static final int EXT3_DEFM_JMODE_WBACK   = 0x0060;

	// EXT2_DIR_PAD defines the directory entries boundaries
	// NOTE: It must be a multiple of 4
	public static final int EXT2_DIR_PAD     = 4;
	public static final int EXT2_DIR_ROUND   = (EXT2_DIR_PAD - 1);
	public static final int EXT2_MAX_REC_LEN = ((1<<16)-1);

	//Ext2 directory file types.  Only the low 3 bits are used.  The
	//other bits are reserved for now.
	public static final short	EXT2_FT_UNKNOWN	 = 0;
	public static final short	EXT2_FT_REG_FILE = 1;
	public static final short	EXT2_FT_DIR      = 2;
	public static final short	EXT2_FT_CHRDEV	 = 3;
	public static final short	EXT2_FT_BLKDEV	 = 4;
	public static final short	EXT2_FT_FIFO	 = 5;
	public static final short	EXT2_FT_SOCK	 = 6;
	public static final short	EXT2_FT_SYMLINK	 = 7;
	public static final short	EXT2_FT_MAX      = 8;
	
	// Structure of a directory entry
	public static final int EXT2_NAME_LEN = 255;

	public static final int EXT2_MIN_BLOCK_SIZE = 1024;
	public static final int EXT2_MAX_BLOCK_SIZE = 4096;


	/* ext2fs.h (e2fsprogs):
	 * Ext2/linux mode flags.  We define them here so that we don't need
	 * to depend on the OS's sys/stat.h, since we may be compiling on a
	 * non-Linux system.
	 */
	public static final int LINUX_S_IFMT  = 00170000;
	public static final int LINUX_S_IFSOC = 0140000;
	public static final int LINUX_S_IFLNK = 0120000;
	public static final int LINUX_S_IFREG = 0100000;
	public static final int LINUX_S_IFBLK = 0060000;
	public static final int LINUX_S_IFDIR = 0040000;
	public static final int LINUX_S_IFCHR = 0020000;
	public static final int LINUX_S_IFIFO = 0010000;
	public static final int LINUX_S_ISUID = 0004000;
	public static final int LINUX_S_ISGID = 0002000;
	public static final int LINUX_S_ISVTX = 0001000;
	
	public static final int LINUX_S_IRWXU = 00700;
	public static final int LINUX_S_IRUSR = 00400;
	public static final int LINUX_S_IWUSR = 00200;
	public static final int LINUX_S_IXUSR = 00100;
	public static final int LINUX_S_IRWXG = 00070;
	public static final int LINUX_S_IRGRP = 00040;
	public static final int LINUX_S_IWGRP = 00020;
	public static final int LINUX_S_IXGRP = 00010;
	
	public static final int LINUX_S_IRWXO = 00007;
	public static final int LINUX_S_IROTH = 00004;
	public static final int LINUX_S_IWOTH = 00002;
	public static final int LINUX_S_IXOTH = 00001;


	/*
	 * Inode flags (GETFLAGS/SETFLAGS)
	 */
	public static final int EXT2_SECRM_FL           = 0x00000001; /* Secure deletion */
	public static final int EXT2_UNRM_FL			= 0x00000002; /* Undelete */
	public static final int EXT2_COMPR_FL			= 0x00000004; /* Compress file */
													
	public static final int EXT2_SYNC_FL			= 0x00000008; /* Synchronous updates */
	public static final int EXT2_IMMUTABLE_FL		= 0x00000010; /* Immutable file */
	public static final int EXT2_APPEND_FL			= 0x00000020; /* writes to file may only append */
	public static final int EXT2_NODUMP_FL			= 0x00000040; /* do not dump file */
	public static final int EXT2_NOATIME_FL			= 0x00000080; /* do not update atime */
	/* Reserved for compression usage... */			
	public static final int EXT2_DIRTY_FL			= 0x00000100;		
	public static final int EXT2_COMPRBLK_FL		= 0x00000200; /* One or more compressed clusters */
	public static final int EXT2_NOCOMP_FL			= 0x00000400; /* Don't compress */
	public static final int EXT2_ECOMPR_FL			= 0x00000800; /* Compression error */
	/* End compression flags --- maybe not all used */	
	public static final int EXT2_BTREE_FL			= 0x00001000; /* btree format dir */
	public static final int EXT2_INDEX_FL			= 0x00001000; /* hash-indexed directory */
	public static final int EXT2_IMAGIC_FL			= 0x00002000; /* AFS directory */
	public static final int EXT2_JOURNAL_DATA_FL	= 0x00004000; /* Reserved for ext3 */
	public static final int EXT2_NOTAIL_FL			= 0x00008000; /* file tail should not be merged */
	public static final int EXT2_DIRSYNC_FL			= 0x00010000; /* dirsync behaviour (directories only) */
	public static final int EXT2_TOPDIR_FL			= 0x00020000; /* Top of directory hierarchies*/
	public static final int EXT2_RESERVED_FL		= 0x80000000; /* reserved for ext2 lib */

	public static final int EXT2_FL_USER_VISIBLE	= 0x0003DFFF; /* User visible flags */
	public static final int EXT2_FL_USER_MODIFIABLE = 0x000380FF; /* User modifiable flags */

	/* Flags that should be inherited by new inodes from their parent. */
	public static final int EXT2_FL_INHERITED = (EXT2_SECRM_FL |
	                                             EXT2_UNRM_FL |
	                                             EXT2_COMPR_FL |
	                                             EXT2_SYNC_FL |
	                                             EXT2_IMMUTABLE_FL |
	                                             EXT2_APPEND_FL |
	                                             EXT2_NODUMP_FL |
	                                             EXT2_NOATIME_FL |
	                                             EXT2_COMPRBLK_FL|
	                                             EXT2_NOCOMP_FL |
	                                             EXT2_JOURNAL_DATA_FL |
	                                             EXT2_NOTAIL_FL |
	                                             EXT2_DIRSYNC_FL);

	/* Flags that are appropriate for regular files (all but dir-specific ones). */
	public static final int EXT2_REG_FLMASK = (~(EXT2_DIRSYNC_FL | EXT2_TOPDIR_FL));

	/* Flags that are appropriate for non-directories/regular files. */
	public static final int EXT2_OTHER_FLMASK = (EXT2_NODUMP_FL | EXT2_NOATIME_FL);
}
