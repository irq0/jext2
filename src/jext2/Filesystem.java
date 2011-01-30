package jext2;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;

// TODO make this a non static class which contains all pointers to runtime:
//      filesystem data like superblock, blockaccess, etc. 
//      This then should replace the direct superblock, etc. references in each class
public class Filesystem {
    private static Charset charset = Charset.defaultCharset();
    
    public static final Charset getCharset() {
        return charset;
    }
    public static final void setCharset(Charset charset) {
        Filesystem.charset = charset;
    }

    /**
     * Get the PID of the running process
     */
    public static long getPID() {
        String appName = ManagementFactory.getRuntimeMXBean().getName();
        String strPid = appName.substring(0, appName.indexOf('@')-1); 
        long pid = Long.parseLong(strPid);
        return pid;     
    }

}
