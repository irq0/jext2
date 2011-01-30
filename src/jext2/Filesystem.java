package jext2;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;

public class Filesystem {
    /**
     * Get the PID of the running process
     */
    public static long getPID() {
        String appName = ManagementFactory.getRuntimeMXBean().getName();
        String strPid = appName.substring(0, appName.indexOf('@')-1); 
        long pid = Long.parseLong(strPid);
        return pid;     
    }
 
    // TODO add commandline option - 
    // filesystem should normally be charset agnosic but since java uses unicode
    //  for its strings conversion is required
    public static Charset getCharset() {
        return Charset.forName("UTF-8"); 
    }
        
        
}
