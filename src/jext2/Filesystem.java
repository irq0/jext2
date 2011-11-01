package jext2;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// TODO make this a non static class which contains all pointers to runtime:
//      filesystem data like superblock, blockaccess, etc. 
//      This then should replace the direct superblock, etc. references in each class
public class Filesystem {
    private static Charset charset = Charset.defaultCharset();
    private static Logger logger = null;

	public static Charset getCharset() {
        return charset;
    }
    public static void setCharset(Charset charset) {
        Filesystem.charset = charset;
    }

    /**
     * Get the PID of the running process
     */
    public static long getPID() {
        String appName = ManagementFactory.getRuntimeMXBean().getName();
        String strPid = appName.substring(0, appName.indexOf('@')-1); 
        return Long.parseLong(strPid);
    }
    
    public static void initializeLoggingToFile(String logfile) throws IOException {
        logger = Logger.getLogger("jext2");

	    FileHandler handler = new FileHandler(logfile);
        handler.setFormatter(new SimpleFormatter());
        
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        logger.info("jext2 start");
    }

    public static Logger getLogger() {
        return logger;
    }
    
}
