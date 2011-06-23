package jext2;

/**
 * Handle the Linux mode flags. Values were taken from e2fsprogs
 */ 
@SuppressWarnings( {"OctalInteger"})
public class Mode {

    public static boolean mask(int mode, int mask) {
    	return (mode & IFMT) == mask;
    }

    public static final int IFMT  = 00170000;
    public static final int IFSOC = 0140000;
    public static final int IFLNK = 0120000;
    public static final int IFREG = 0100000;
    public static final int IFBLK = 0060000;
    public static final int IFDIR = 0040000;
    public static final int IFCHR = 0020000;
    public static final int IFIFO = 0010000;
    
    /** Set user ID on execution.  */
    public static final int ISUID = 0004000;
    /** Set group ID on execution.  */
    public static final int ISGID = 0002000;
    /** Sticky bit */
    public static final int ISVTX = 0001000;
    /* Read, write, and execute by owner.  */
    public static final int IRWXU = 00700;    
    /** Read by owner */
    public static final int IRUSR = 00400;
    /** Write by owner */
    public static final int IWUSR = 00200;
    /** Execute by owner */
    public static final int IXUSR = 00100;
    /** Read, write, and execute by group  */
    public static final int IRWXG = 00070;    
    /** Read by group */
    public static final int IRGRP = 00040;
    /** Write by group */
    public static final int IWGRP = 00020;
    /** Execute by group */
    public static final int IXGRP = 00010;
    /** Read, write, and execute by others  */    
    public static final int IRWXO = 00007;    
    /** Read by others */
    public static final int IROTH = 00004;
    /** Write by others */
    public static final int IWOTH = 00002;
    /** Execute by others */
    public static final int IXOTH = 00001;
       
    public static boolean isSocket(int mode) {
        return (mode & IFMT) == IFSOC;
    }    
    public static boolean isSymlink(int mode) {     
        return (mode & IFMT) == IFLNK;
    }    
    public static boolean isRegular(int mode) {
        return (mode & IFMT) == IFREG;
    }
    public static boolean isBlockdev(int mode) {
        return (mode & IFMT) == IFBLK;
    }
    public static boolean isDirectory(int mode) {
        return (mode & IFMT) == IFDIR;
    }
    public static boolean isChardev(int mode) {
        return (mode & IFMT) == IFCHR;
    }
    public static boolean isFifo(int mode) {
        return (mode & IFMT) == IFIFO;
    }
}
