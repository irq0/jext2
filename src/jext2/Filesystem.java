package jext2;

import java.lang.management.ManagementFactory;

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
        
        
}
